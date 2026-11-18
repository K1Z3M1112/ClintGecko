package com.simplefox.browser.blocker.engine

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Pure Kotlin implementation of the website blocker engine (previously C++/JNI).
 * Public API and on-disk format are unchanged: little-endian header
 * (magic u32, version u32, domainCount u64, checksum u64) followed by sorted unsigned u64 FNV-1a hashes.
 *
 * Performance notes:
 *  - Hosts-file parsing works directly on a reusable CharArray: no per-line String/StringBuilder allocations.
 *  - The builder de-duplicates while it grows, so overlapping lists don't inflate peak memory.
 *  - Finalizing sorts/de-dupes in place and streams the file out in chunks (no full-size ByteBuffer copy).
 *  - Lookups are allocation-free.
 */
object WebsiteBlockerNative {
    private const val MAGIC = 0x314C4257
    private const val VERSION = 1
    private const val HEADER_BYTES = 24L
    private const val FNV_OFFSET = -3750763034362895579L // 14695981039346656037ULL
    private const val FNV_PRIME = 1099511628211L

    private const val READ_CHARS = 1 shl 16
    private const val WRITE_CHUNK_LONGS = 1 shl 13
    private const val DEDUPE_ON_GROW_MIN = 1 shl 20

    private val nextHandle = AtomicLong(1L)
    private val builders = ConcurrentHashMap<Long, Builder>()
    private val databases = ConcurrentHashMap<Long, Database>()

    private class Builder {
        var hashes = LongArray(1024)
        var size = 0

        fun add(hash: Long) {
            if (size == hashes.size) makeRoom()
            hashes[size++] = hash
        }

        /**
         * Large lists overlap heavily, so before doubling the array try to reclaim space by
         * sorting + de-duplicating. If that frees at least half the capacity we skip the growth.
         * Sorting only happens at (at most) each doubling point, so total extra cost stays bounded.
         */
        private fun makeRoom() {
            if (size >= DEDUPE_ON_GROW_MIN) {
                sortUnsigned(hashes, size)
                size = dedupe(hashes, size)
                if (size <= hashes.size / 2) return
            }
            val newCap = if (hashes.size > Int.MAX_VALUE / 2) Int.MAX_VALUE - 8 else hashes.size * 2
            hashes = hashes.copyOf(newCap)
        }
    }

    private class Database(val hashes: LongBuffer, val count: Int)

    // ---- builder ----

    fun nativeCreateBuilder(): Long {
        val id = nextHandle.getAndIncrement()
        builders[id] = Builder()
        return id
    }

    private const val EMPTY_STATS = "{\"added\":0,\"skipped\":0}"

    fun nativeAddText(builderPtr: Long, text: String): String {
        val builder = builders[builderPtr] ?: return EMPTY_STATS
        val stats = LongArray(2)
        synchronized(builder) {
            val chars = text.toCharArray()
            val consumed = consumeLines(builder, chars, 0, chars.size, stats)
            if (consumed < chars.size) handleLine(builder, chars, consumed, chars.size, stats)
        }
        return statsJson(stats)
    }

    /**
     * Streams a hosts file through a fixed char buffer. Unlike nativeAddText(file.readText()) this never
     * holds the whole file (tens of MB for large lists) as a single String, and unlike readLine() it
     * doesn't allocate a String per line.
     */
    fun nativeAddFile(builderPtr: Long, file: File): String {
        val builder = builders[builderPtr] ?: return EMPTY_STATS
        val stats = LongArray(2)
        synchronized(builder) { streamFile(builder, file, stats) }
        return statsJson(stats)
    }

    /** Counts valid domains in a hosts file without retaining any hashes (used for category display). */
    fun nativeCountFile(file: File): Long {
        val stats = LongArray(2)
        streamFile(null, file, stats)
        return stats[0]
    }

    private fun statsJson(stats: LongArray) = "{\"added\":${stats[0]},\"skipped\":${stats[1]}}"

    private fun streamFile(builder: Builder?, file: File, stats: LongArray) {
        file.inputStream().reader(Charsets.UTF_8).use { reader ->
            var buf = CharArray(READ_CHARS)
            var len = 0
            while (true) {
                if (len == buf.size) buf = buf.copyOf(buf.size * 2) // pathological very long line
                val n = reader.read(buf, len, buf.size - len)
                if (n < 0) break
                len += n
                val consumed = consumeLines(builder, buf, 0, len, stats)
                if (consumed > 0) {
                    System.arraycopy(buf, consumed, buf, 0, len - consumed)
                    len -= consumed
                }
            }
            if (len > 0) handleLine(builder, buf, 0, len, stats)
        }
    }

    /** Processes every complete line in [from, to); returns the index just past the last terminator. */
    private fun consumeLines(builder: Builder?, buf: CharArray, from: Int, to: Int, stats: LongArray): Int {
        var pos = from
        var i = from
        while (i < to) {
            val c = buf[i]
            if (c == '\n' || c == '\r') {
                handleLine(builder, buf, pos, i, stats)
                pos = i + 1
            }
            i++
        }
        return pos
    }

    private fun handleLine(builder: Builder?, buf: CharArray, from: Int, to: Int, stats: LongArray) {
        val result = processLine(builder, buf, from, to)
        if (result > 0) stats[0]++ else if (result < 0) stats[1]++
    }

    /**
     * Returns 1 if a domain was added, -1 if the line was skipped as invalid, 0 if ignored (blank/comment).
     * Hash is computed in the same pass that validates and lower-cases the token.
     */
    private fun processLine(builder: Builder?, buf: CharArray, from: Int, to: Int): Int {
        var start = from
        var end = to
        while (start < end && (buf[start] == ' ' || buf[start] == '\t')) start++
        while (end > start && isSpace(buf[end - 1])) end--
        if (end == start) return 0

        val first = buf[start]
        if (first == '#' || first == ';' || first == '!') return 0

        var tokStart = start
        var tokEnd = end
        val sep = indexOfBlank(buf, start, end)
        if (sep >= 0) {
            tokEnd = sep
            val rest = indexOfNonBlank(buf, sep, end)
            if (rest >= 0 && looksLikeIp(buf, start, sep)) {
                val e = indexOfBlank(buf, rest, end)
                tokStart = rest
                tokEnd = if (e < 0) end else e
            }
        }

        // normalize: trim whitespace, drop trailing dots
        while (tokStart < tokEnd && isSpace(buf[tokStart])) tokStart++
        while (tokEnd > tokStart && isSpace(buf[tokEnd - 1])) tokEnd--
        while (tokEnd > tokStart && buf[tokEnd - 1] == '.') tokEnd--
        if (tokEnd == tokStart) return -1

        var h = FNV_OFFSET
        var hasDot = false
        for (i in tokStart until tokEnd) {
            var c = buf[i]
            if (c == '.') {
                hasDot = true
            } else if (c in 'A'..'Z') {
                c += 32
            } else if (!(c in 'a'..'z' || c in '0'..'9' || c == '-')) {
                return -1
            }
            h = (h xor c.code.toLong()) * FNV_PRIME
        }
        if (!hasDot || isBoilerplate(h)) return -1
        builder?.add(h)
        return 1
    }

    fun nativeFinalizeBuilder(builderPtr: Long, outputPath: String): String {
        val builder = builders[builderPtr]
            ?: return "{\"success\":false,\"domainCount\":0,\"sizeBytes\":0}"
        var success = false
        var count = 0L
        var sizeBytes = 0L
        try {
            synchronized(builder) {
                // In place: no extra full-size copies of the hash array.
                sortUnsigned(builder.hashes, builder.size)
                builder.size = dedupe(builder.hashes, builder.size)
                val n = builder.size
                val hashes = builder.hashes

                val chunk = ByteBuffer.allocate(WRITE_CHUNK_LONGS * 8).order(ByteOrder.LITTLE_ENDIAN)
                val bytes = chunk.array()
                var checksum = FNV_OFFSET

                RandomAccessFile(File(outputPath), "rw").use { raf ->
                    raf.setLength(0)
                    raf.seek(HEADER_BYTES)
                    var i = 0
                    while (i < n) {
                        val take = minOf(WRITE_CHUNK_LONGS, n - i)
                        chunk.clear()
                        for (k in 0 until take) chunk.putLong(hashes[i + k])
                        val len = take * 8
                        for (b in 0 until len) {
                            checksum = (checksum xor (bytes[b].toLong() and 0xFF)) * FNV_PRIME
                        }
                        raf.write(bytes, 0, len)
                        i += take
                    }
                    val header = ByteBuffer.allocate(HEADER_BYTES.toInt()).order(ByteOrder.LITTLE_ENDIAN)
                    header.putInt(MAGIC).putInt(VERSION).putLong(n.toLong()).putLong(checksum)
                    raf.seek(0)
                    raf.write(header.array())
                }
                count = n.toLong()
                sizeBytes = HEADER_BYTES + count * 8
            }
            success = true
        } catch (_: Exception) {
            success = false
            count = 0L
            sizeBytes = 0L
        }
        return "{\"success\":$success,\"domainCount\":$count,\"sizeBytes\":$sizeBytes}"
    }

    fun nativeDestroyBuilder(builderPtr: Long) {
        builders.remove(builderPtr)
    }

    // ---- database ----

    /**
     * @param verifyChecksum when false the (O(file size)) checksum pass is skipped; the header and
     * size are still validated. Callers pass false only when the file is known to be unchanged since
     * it was last verified (see WebsiteBlockerEngine).
     */
    fun nativeLoadDatabase(path: String, verifyChecksum: Boolean = true): Long {
        return try {
            RandomAccessFile(path, "r").use { raf ->
                val size = raf.length()
                if (size < HEADER_BYTES) return 0L
                val map = raf.channel.map(FileChannel.MapMode.READ_ONLY, 0, size).order(ByteOrder.LITTLE_ENDIAN)
                val magic = map.getInt(0)
                val version = map.getInt(4)
                val count = map.getLong(8)
                val checksum = map.getLong(16)
                if (count < 0 || count > (size - HEADER_BYTES) / 8) return 0L
                if (magic != MAGIC || version != VERSION || HEADER_BYTES + count * 8 != size) return 0L
                if (count > Int.MAX_VALUE) return 0L

                if (verifyChecksum) {
                    var h = FNV_OFFSET
                    val reader = map.duplicate()
                    reader.position(HEADER_BYTES.toInt())
                    val chunk = ByteArray(1 shl 16)
                    while (reader.hasRemaining()) {
                        val n = minOf(chunk.size, reader.remaining())
                        reader.get(chunk, 0, n)
                        for (i in 0 until n) {
                            h = (h xor (chunk[i].toLong() and 0xFF)) * FNV_PRIME
                        }
                    }
                    if (h != checksum) return 0L
                }

                map.position(HEADER_BYTES.toInt())
                val longs = map.slice().order(ByteOrder.LITTLE_ENDIAN).asLongBuffer()
                val id = nextHandle.getAndIncrement()
                databases[id] = Database(longs, count.toInt())
                id
            }
        } catch (_: Exception) {
            0L
        }
    }

    /** Allocation-free: hashes the host (lower-cased on the fly) and each parent-domain suffix. */
    fun nativeIsBlocked(handle: Long, host: String): Boolean {
        val db = databases[handle] ?: return false
        var start = 0
        var end = host.length
        while (start < end && isSpace(host[start])) start++
        while (end > start && isSpace(host[end - 1])) end--
        while (end > start && host[end - 1] == '.') end--
        if (end == start) return false
        for (i in start until end) {
            val c = host[i]
            if (!(isAsciiAlnum(c) || c == '.' || c == '-')) return false
        }

        if (contains(db, fnvLower(host, start, end))) return true
        var dot = host.indexOf('.', start)
        while (dot in start until end) {
            val next = host.indexOf('.', dot + 1)
            if (next < 0 || next >= end) break // never test the bare TLD
            if (contains(db, fnvLower(host, dot + 1, end))) return true
            dot = next
        }
        return false
    }

    fun nativeDomainCount(handle: Long): Long = databases[handle]?.count?.toLong() ?: 0L

    fun nativeUnloadDatabase(handle: Long) {
        databases.remove(handle)
    }

    // ---- helpers ----

    private fun contains(db: Database, hash: Long): Boolean {
        val hashes = db.hashes
        var lo = 0
        var hi = db.count - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val c = java.lang.Long.compareUnsigned(hashes.get(mid), hash)
            if (c < 0) lo = mid + 1 else if (c > 0) hi = mid - 1 else return true
        }
        return false
    }

    private fun fnvLower(s: String, from: Int, to: Int): Long {
        var h = FNV_OFFSET
        for (i in from until to) {
            var c = s[i]
            if (c in 'A'..'Z') c += 32
            h = (h xor c.code.toLong()) * FNV_PRIME
        }
        return h
    }

    private fun sortUnsigned(a: LongArray, size: Int) {
        for (i in 0 until size) a[i] = a[i] xor Long.MIN_VALUE
        java.util.Arrays.sort(a, 0, size)
        for (i in 0 until size) a[i] = a[i] xor Long.MIN_VALUE
    }

    /** In-place de-dupe of a sorted prefix; returns the new size. */
    private fun dedupe(a: LongArray, size: Int): Int {
        if (size == 0) return 0
        var n = 1
        for (i in 1 until size) if (a[i] != a[n - 1]) a[n++] = a[i]
        return n
    }

    private fun isSpace(c: Char) = c == ' ' || c in '\t'..'\r'

    private fun isAsciiAlnum(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9'

    private fun indexOfBlank(s: CharArray, from: Int, to: Int): Int {
        for (i in from until to) if (s[i] == ' ' || s[i] == '\t') return i
        return -1
    }

    private fun indexOfNonBlank(s: CharArray, from: Int, to: Int): Int {
        for (i in from until to) if (s[i] != ' ' && s[i] != '\t') return i
        return -1
    }

    private fun looksLikeIp(s: CharArray, from: Int, to: Int): Boolean {
        if (from >= to) return false
        var punct = false
        for (i in from until to) {
            val c = s[i]
            if (c == '.' || c == ':') punct = true
            else if (!(c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F')) return false
        }
        return punct
    }

    private fun hashOf(s: String): Long = fnvLower(s, 0, s.length)

    private val boilerplateHashes: LongArray = arrayOf(
        "0.0.0.0", "localhost", "localhost.localdomain", "local", "broadcasthost",
        "ip6-localhost", "ip6-loopback", "ip6-allnodes", "ip6-allrouters",
        "ip6-localnet", "ip6-mcastprefix"
    ).map(::hashOf).toLongArray()

    private fun isBoilerplate(hash: Long): Boolean {
        for (b in boilerplateHashes) if (b == hash) return true
        return false
    }
}
