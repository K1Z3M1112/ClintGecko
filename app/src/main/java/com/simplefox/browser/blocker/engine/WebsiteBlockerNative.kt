package com.simplefox.browser.blocker.engine

import java.io.File
import java.io.FileOutputStream
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
 */
object WebsiteBlockerNative {
    private const val MAGIC = 0x314C4257
    private const val VERSION = 1
    private const val HEADER_BYTES = 24L
    private const val FNV_OFFSET = -3750763034362895579L // 14695981039346656037ULL
    private const val FNV_PRIME = 1099511628211L

    private val nextHandle = AtomicLong(1L)
    private val builders = ConcurrentHashMap<Long, Builder>()
    private val databases = ConcurrentHashMap<Long, Database>()

    private class Builder {
        var hashes = LongArray(1024)
        var size = 0
        fun add(hash: Long) {
            if (size == hashes.size) hashes = hashes.copyOf(size * 2)
            hashes[size++] = hash
        }
    }

    private class Database(val hashes: LongBuffer, val count: Long)

    // ---- builder ----

    fun nativeCreateBuilder(): Long {
        val id = nextHandle.getAndIncrement()
        builders[id] = Builder()
        return id
    }

    fun nativeAddText(builderPtr: Long, text: String): String {
        val builder = builders[builderPtr] ?: return "{\"added\":0,\"skipped\":0}"
        var added = 0L
        var skipped = 0L
        synchronized(builder) {
            val len = text.length
            var pos = 0
            while (pos < len) {
                var lineEnd = text.indexOf('\n', pos)
                if (lineEnd < 0) lineEnd = len
                var start = pos
                var end = lineEnd
                pos = lineEnd + 1

                while (start < end && (text[start] == ' ' || text[start] == '\t')) start++
                while (end > start && isSpace(text[end - 1])) end--
                if (end == start) continue

                val first = text[start]
                if (first == '#' || first == ';' || first == '!') continue

                val line = text.substring(start, end)
                val sep = indexOfBlank(line, 0)
                val token: String = if (sep < 0) {
                    line
                } else {
                    val firstToken = line.substring(0, sep)
                    val rest = indexOfNonBlank(line, sep)
                    val second = if (rest < 0) "" else {
                        val e = indexOfBlank(line, rest)
                        line.substring(rest, if (e < 0) line.length else e)
                    }
                    if (looksLikeIp(firstToken) && second.isNotEmpty()) second else firstToken
                }

                val domain = normalizeDomain(token)
                if (domain.isEmpty() || domain.indexOf('.') < 0 || isBoilerplate(domain)) {
                    skipped++
                    continue
                }
                builder.add(fnv1a(domain, 0, domain.length))
                added++
            }
        }
        return "{\"added\":$added,\"skipped\":$skipped}"
    }

    fun nativeFinalizeBuilder(builderPtr: Long, outputPath: String): String {
        val builder = builders[builderPtr]
            ?: return "{\"success\":false,\"domainCount\":0,\"sizeBytes\":0}"
        var success = false
        var count = 0L
        var sizeBytes = 0L
        try {
            val sorted = synchronized(builder) { builder.hashes.copyOf(builder.size) }
            sortUnsigned(sorted)
            val unique = dedupe(sorted)
            count = unique.size.toLong()

            val dataBytes = count * 8
            val buf = ByteBuffer.allocate((HEADER_BYTES + dataBytes).toInt()).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(MAGIC).putInt(VERSION).putLong(count).putLong(0L)
            val dataStart = buf.position()
            for (h in unique) buf.putLong(h)
            var checksum = FNV_OFFSET
            val arr = buf.array()
            for (i in dataStart until arr.size) {
                checksum = (checksum xor (arr[i].toLong() and 0xFF)) * FNV_PRIME
            }
            buf.putLong(16, checksum)
            FileOutputStream(File(outputPath)).use { it.write(arr) }
            sizeBytes = HEADER_BYTES + dataBytes
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

    fun nativeLoadDatabase(path: String): Long {
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

                var h = FNV_OFFSET
                for (i in HEADER_BYTES.toInt() until size.toInt()) {
                    h = (h xor (map.get(i).toLong() and 0xFF)) * FNV_PRIME
                }
                if (h != checksum) return 0L

                map.position(HEADER_BYTES.toInt())
                val longs = map.slice().order(ByteOrder.LITTLE_ENDIAN).asLongBuffer()
                val id = nextHandle.getAndIncrement()
                databases[id] = Database(longs, count)
                id
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun nativeIsBlocked(handle: Long, host: String): Boolean {
        val db = databases[handle] ?: return false
        val n = normalizeDomain(host)
        if (n.isEmpty()) return false
        if (contains(db, fnv1a(n, 0, n.length))) return true
        var dot = n.indexOf('.')
        while (dot >= 0) {
            if (n.indexOf('.', dot + 1) < 0) break
            if (contains(db, fnv1a(n, dot + 1, n.length))) return true
            dot = n.indexOf('.', dot + 1)
        }
        return false
    }

    fun nativeDomainCount(handle: Long): Long = databases[handle]?.count ?: 0L

    fun nativeUnloadDatabase(handle: Long) {
        databases.remove(handle)
    }

    // ---- helpers ----

    private fun contains(db: Database, hash: Long): Boolean {
        var lo = 0
        var hi = db.count.toInt() - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val c = java.lang.Long.compareUnsigned(db.hashes.get(mid), hash)
            if (c < 0) lo = mid + 1 else if (c > 0) hi = mid - 1 else return true
        }
        return false
    }

    private fun fnv1a(s: CharSequence, from: Int, to: Int): Long {
        var h = FNV_OFFSET
        for (i in from until to) {
            h = (h xor (s[i].code.toLong() and 0xFF)) * FNV_PRIME
        }
        return h
    }

    private fun sortUnsigned(a: LongArray) {
        for (i in a.indices) a[i] = a[i] xor Long.MIN_VALUE
        a.sort()
        for (i in a.indices) a[i] = a[i] xor Long.MIN_VALUE
    }

    private fun dedupe(a: LongArray): LongArray {
        if (a.isEmpty()) return a
        var n = 1
        for (i in 1 until a.size) if (a[i] != a[n - 1]) a[n++] = a[i]
        return a.copyOf(n)
    }

    private fun isSpace(c: Char) = c == ' ' || c in '\t'..'\r'

    private fun isAsciiAlnum(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9'

    private fun indexOfBlank(s: String, from: Int): Int {
        for (i in from until s.length) if (s[i] == ' ' || s[i] == '\t') return i
        return -1
    }

    private fun indexOfNonBlank(s: String, from: Int): Int {
        for (i in from until s.length) if (s[i] != ' ' && s[i] != '\t') return i
        return -1
    }

    private fun looksLikeIp(token: String): Boolean {
        if (token.isEmpty()) return false
        for (c in token) {
            if (!(c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' || c == '.' || c == ':')) return false
        }
        return token.indexOf('.') >= 0 || token.indexOf(':') >= 0
    }

    private val boilerplate = setOf(
        "0.0.0.0", "localhost", "localhost.localdomain", "local", "broadcasthost",
        "ip6-localhost", "ip6-loopback", "ip6-allnodes", "ip6-allrouters",
        "ip6-localnet", "ip6-mcastprefix"
    )

    private fun isBoilerplate(d: String) = d in boilerplate

    private fun normalizeDomain(raw: String): String {
        var start = 0
        var end = raw.length
        while (start < end && isSpace(raw[start])) start++
        while (end > start && isSpace(raw[end - 1])) end--
        while (end > start && raw[end - 1] == '.') end--
        val sb = StringBuilder(end - start)
        for (i in start until end) {
            val c = raw[i]
            if (!(isAsciiAlnum(c) || c == '.' || c == '-')) return ""
            sb.append(if (c in 'A'..'Z') c + 32 else c)
        }
        return sb.toString()
    }
}
