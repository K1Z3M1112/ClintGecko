package com.simplefox.browser.backup

/**
 * Argon2id (RFC 9106, version 0x13) implemented in-tree so the app does not need BouncyCastle.
 * Single-threaded: lanes are filled sequentially, which yields the same output as parallel fill.
 */
internal object Argon2id {

    private const val VERSION = 0x13
    private const val TYPE_ID = 2
    private const val BLOCK_WORDS = 128
    private const val SYNC_POINTS = 4
    private const val ADDRESSES_PER_BLOCK = 128
    private const val MAX_MEMORY_KB = 256 * 1024 // guards against hostile backup headers exhausting the heap

    fun hash(
        password: ByteArray,
        salt: ByteArray,
        memoryKB: Int,
        iterations: Int,
        parallelism: Int,
        outLen: Int,
        secret: ByteArray = ByteArray(0),
        associatedData: ByteArray = ByteArray(0)
    ): ByteArray {
        require(parallelism in 1..0xFFFFFF) { "parallelism" }
        require(iterations >= 1) { "iterations" }
        require(outLen >= 4) { "outLen" }
        require(memoryKB >= 8 * parallelism) { "memory" }
        require(memoryKB <= MAX_MEMORY_KB) { "memory too large" }

        val h0 = initialHash(password, salt, secret, associatedData, parallelism, outLen, memoryKB, iterations)

        val blockCount = 4 * parallelism * (memoryKB / (4 * parallelism))
        val laneLength = blockCount / parallelism
        val segmentLength = laneLength / SYNC_POINTS
        val memory = LongArray(blockCount * BLOCK_WORDS)

        val seed = ByteArray(h0.size + 8)
        System.arraycopy(h0, 0, seed, 0, h0.size)
        for (lane in 0 until parallelism) {
            putIntLE(seed, h0.size + 4, lane)
            for (col in 0..1) {
                putIntLE(seed, h0.size, col)
                val block = variableHash(seed, 1024)
                loadBlock(memory, (lane * laneLength + col) * BLOCK_WORDS, block)
            }
        }
        h0.fill(0)
        seed.fill(0)

        val addressBlock = LongArray(BLOCK_WORDS)
        val inputBlock = LongArray(BLOCK_WORDS)
        val zeroBlock = LongArray(BLOCK_WORDS)
        val scratchR = LongArray(BLOCK_WORDS)
        val scratchT = LongArray(BLOCK_WORDS)

        for (pass in 0 until iterations) {
            for (slice in 0 until SYNC_POINTS) {
                for (lane in 0 until parallelism) {
                    fillSegment(
                        memory, pass, slice, lane, parallelism, laneLength, segmentLength, blockCount, iterations,
                        addressBlock, inputBlock, zeroBlock, scratchR, scratchT
                    )
                }
            }
        }

        val finalBlock = LongArray(BLOCK_WORDS)
        for (lane in 0 until parallelism) {
            val base = (lane * laneLength + laneLength - 1) * BLOCK_WORDS
            for (w in 0 until BLOCK_WORDS) finalBlock[w] = finalBlock[w] xor memory[base + w]
        }
        val finalBytes = ByteArray(1024)
        storeBlock(finalBlock, 0, finalBytes)
        val tag = variableHash(finalBytes, outLen)

        memory.fill(0)
        finalBlock.fill(0)
        finalBytes.fill(0)
        return tag
    }

    private fun initialHash(
        password: ByteArray, salt: ByteArray, secret: ByteArray, ad: ByteArray,
        parallelism: Int, outLen: Int, memoryKB: Int, iterations: Int
    ): ByteArray {
        val input = java.io.ByteArrayOutputStream()
        fun int(v: Int) {
            val b = ByteArray(4); putIntLE(b, 0, v); input.write(b)
        }
        fun bytes(b: ByteArray) {
            int(b.size); input.write(b)
        }
        int(parallelism); int(outLen); int(memoryKB); int(iterations); int(VERSION); int(TYPE_ID)
        bytes(password); bytes(salt); bytes(secret); bytes(ad)
        val data = input.toByteArray()
        val digest = Blake2b.digest(data, 64)
        data.fill(0)
        return digest
    }

    /** H' from RFC 9106 section 3.3. */
    private fun variableHash(input: ByteArray, outLen: Int): ByteArray {
        val prefixed = ByteArray(4 + input.size)
        putIntLE(prefixed, 0, outLen)
        System.arraycopy(input, 0, prefixed, 4, input.size)
        if (outLen <= 64) return Blake2b.digest(prefixed, outLen)

        val out = ByteArray(outLen)
        var v = Blake2b.digest(prefixed, 64)
        System.arraycopy(v, 0, out, 0, 32)
        var pos = 32
        val r = (outLen + 31) / 32 - 2
        for (i in 2..r) {
            v = Blake2b.digest(v, 64)
            System.arraycopy(v, 0, out, pos, 32)
            pos += 32
        }
        val last = Blake2b.digest(v, outLen - 32 * r)
        System.arraycopy(last, 0, out, pos, last.size)
        return out
    }

    private fun fillSegment(
        memory: LongArray, pass: Int, slice: Int, lane: Int, lanes: Int,
        laneLength: Int, segmentLength: Int, blockCount: Int, passes: Int,
        addressBlock: LongArray, inputBlock: LongArray, zeroBlock: LongArray,
        scratchR: LongArray, scratchT: LongArray
    ) {
        val independent = pass == 0 && slice < SYNC_POINTS / 2
        var startIndex = 0
        if (independent) {
            inputBlock.fill(0)
            inputBlock[0] = pass.toLong()
            inputBlock[1] = lane.toLong()
            inputBlock[2] = slice.toLong()
            inputBlock[3] = blockCount.toLong()
            inputBlock[4] = passes.toLong()
            inputBlock[5] = TYPE_ID.toLong()
        }
        if (pass == 0 && slice == 0) {
            startIndex = 2
            if (independent) nextAddresses(addressBlock, inputBlock, zeroBlock, scratchR, scratchT)
        }

        var curr = lane * laneLength + slice * segmentLength + startIndex
        var prev = if (curr % laneLength == 0) curr + laneLength - 1 else curr - 1

        for (i in startIndex until segmentLength) {
            if (curr % laneLength == 1) prev = curr - 1

            val rand: Long
            if (independent) {
                if (i % ADDRESSES_PER_BLOCK == 0) nextAddresses(addressBlock, inputBlock, zeroBlock, scratchR, scratchT)
                rand = addressBlock[i % ADDRESSES_PER_BLOCK]
            } else {
                rand = memory[prev * BLOCK_WORDS]
            }

            var refLane = ((rand ushr 32) % lanes).toInt()
            if (pass == 0 && slice == 0) refLane = lane
            val refIndex = indexAlpha(pass, slice, i, refLane == lane, rand and 0xFFFFFFFFL, laneLength, segmentLength)
            val ref = refLane * laneLength + refIndex

            fillBlock(memory, prev * BLOCK_WORDS, ref * BLOCK_WORDS, curr * BLOCK_WORDS, pass > 0, scratchR, scratchT)
            curr++
            prev++
        }
    }

    private fun indexAlpha(
        pass: Int, slice: Int, index: Int, sameLane: Boolean, j1: Long, laneLength: Int, segmentLength: Int
    ): Int {
        val area: Int = if (pass == 0) {
            if (slice == 0) index - 1
            else if (sameLane) slice * segmentLength + index - 1
            else slice * segmentLength + (if (index == 0) -1 else 0)
        } else {
            if (sameLane) laneLength - segmentLength + index - 1
            else laneLength - segmentLength + (if (index == 0) -1 else 0)
        }
        var rel = (j1 * j1) ushr 32
        rel = area.toLong() - 1 - ((area.toLong() * rel) ushr 32)
        val start = if (pass != 0 && slice != SYNC_POINTS - 1) (slice + 1) * segmentLength else 0
        return ((start.toLong() + rel) % laneLength).toInt()
    }

    private fun nextAddresses(
        address: LongArray, input: LongArray, zero: LongArray, r: LongArray, t: LongArray
    ) {
        input[6]++
        compress(zero, 0, input, 0, address, 0, false, r, t)
        compress(zero, 0, address, 0, address, 0, false, r, t)
    }

    private fun fillBlock(
        memory: LongArray, prevOff: Int, refOff: Int, nextOff: Int, xor: Boolean, r: LongArray, t: LongArray
    ) {
        compress(memory, prevOff, memory, refOff, memory, nextOff, xor, r, t)
    }

    /** G(X, Y): R = X ^ Y; Z = P(R); out = Z ^ R (xor'd into old out when [xor]). */
    private fun compress(
        x: LongArray, xOff: Int, y: LongArray, yOff: Int, out: LongArray, outOff: Int,
        xor: Boolean, r: LongArray, t: LongArray
    ) {
        for (w in 0 until BLOCK_WORDS) r[w] = x[xOff + w] xor y[yOff + w]
        if (xor) {
            for (w in 0 until BLOCK_WORDS) t[w] = r[w] xor out[outOff + w]
        } else {
            System.arraycopy(r, 0, t, 0, BLOCK_WORDS)
        }

        for (i in 0 until 8) {
            val b = 16 * i
            perm(r, b, b + 1, b + 2, b + 3, b + 4, b + 5, b + 6, b + 7, b + 8, b + 9, b + 10, b + 11, b + 12, b + 13, b + 14, b + 15)
        }
        for (i in 0 until 8) {
            val b = 2 * i
            perm(
                r, b, b + 1, b + 16, b + 17, b + 32, b + 33, b + 48, b + 49,
                b + 64, b + 65, b + 80, b + 81, b + 96, b + 97, b + 112, b + 113
            )
        }
        for (w in 0 until BLOCK_WORDS) out[outOff + w] = r[w] xor t[w]
    }

    private fun perm(
        v: LongArray, i0: Int, i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int,
        i8: Int, i9: Int, i10: Int, i11: Int, i12: Int, i13: Int, i14: Int, i15: Int
    ) {
        gb(v, i0, i4, i8, i12)
        gb(v, i1, i5, i9, i13)
        gb(v, i2, i6, i10, i14)
        gb(v, i3, i7, i11, i15)
        gb(v, i0, i5, i10, i15)
        gb(v, i1, i6, i11, i12)
        gb(v, i2, i7, i8, i13)
        gb(v, i3, i4, i9, i14)
    }

    private fun gb(v: LongArray, ia: Int, ib: Int, ic: Int, id: Int) {
        var a = v[ia]; var b = v[ib]; var c = v[ic]; var d = v[id]
        a = a + b + 2L * (a and 0xFFFFFFFFL) * (b and 0xFFFFFFFFL)
        d = java.lang.Long.rotateRight(d xor a, 32)
        c = c + d + 2L * (c and 0xFFFFFFFFL) * (d and 0xFFFFFFFFL)
        b = java.lang.Long.rotateRight(b xor c, 24)
        a = a + b + 2L * (a and 0xFFFFFFFFL) * (b and 0xFFFFFFFFL)
        d = java.lang.Long.rotateRight(d xor a, 16)
        c = c + d + 2L * (c and 0xFFFFFFFFL) * (d and 0xFFFFFFFFL)
        b = java.lang.Long.rotateRight(b xor c, 63)
        v[ia] = a; v[ib] = b; v[ic] = c; v[id] = d
    }

    private fun loadBlock(dst: LongArray, off: Int, src: ByteArray) {
        for (w in 0 until BLOCK_WORDS) dst[off + w] = getLongLE(src, w * 8)
    }

    private fun storeBlock(src: LongArray, off: Int, dst: ByteArray) {
        for (w in 0 until BLOCK_WORDS) putLongLE(dst, w * 8, src[off + w])
    }

    private fun putIntLE(b: ByteArray, off: Int, v: Int) {
        b[off] = v.toByte(); b[off + 1] = (v ushr 8).toByte()
        b[off + 2] = (v ushr 16).toByte(); b[off + 3] = (v ushr 24).toByte()
    }

    internal fun getLongLE(b: ByteArray, off: Int): Long {
        var r = 0L
        for (k in 7 downTo 0) r = (r shl 8) or (b[off + k].toLong() and 0xFF)
        return r
    }

    internal fun putLongLE(b: ByteArray, off: Int, v: Long) {
        for (k in 0 until 8) b[off + k] = (v ushr (8 * k)).toByte()
    }
}

/** Unkeyed BLAKE2b (RFC 7693) with variable digest length 1..64. */
internal object Blake2b {

    private val IV = longArrayOf(
        0x6a09e667f3bcc908L, -0x4498517a7b3558c5L, 0x3c6ef372fe94f82bL, -0x5ab00ac5a0e2c90fL,
        0x510e527fade682d1L, -0x64fa9773d4c193e1L, 0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L
    )

    private val SIGMA = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
        intArrayOf(14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3),
        intArrayOf(11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4),
        intArrayOf(7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8),
        intArrayOf(9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13),
        intArrayOf(2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9),
        intArrayOf(12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11),
        intArrayOf(13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10),
        intArrayOf(6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5),
        intArrayOf(10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0)
    )

    fun digest(data: ByteArray, outLen: Int): ByteArray {
        require(outLen in 1..64) { "outLen" }
        val h = IV.copyOf()
        h[0] = h[0] xor 0x01010000L xor outLen.toLong()

        val m = LongArray(16)
        val block = ByteArray(128)
        var offset = 0
        var counter = 0L
        // The final block is always compressed with the finalization flag, even when it is full or empty.
        while (data.size - offset > 128) {
            counter += 128
            System.arraycopy(data, offset, block, 0, 128)
            loadWords(block, m)
            compress(h, m, counter, false)
            offset += 128
        }
        val rest = data.size - offset
        block.fill(0)
        System.arraycopy(data, offset, block, 0, rest)
        loadWords(block, m)
        counter += rest
        compress(h, m, counter, true)

        val full = ByteArray(64)
        for (i in 0 until 8) Argon2id.putLongLE(full, i * 8, h[i])
        return if (outLen == 64) full else full.copyOf(outLen)
    }

    private fun loadWords(block: ByteArray, m: LongArray) {
        for (i in 0 until 16) m[i] = Argon2id.getLongLE(block, i * 8)
    }

    private fun compress(h: LongArray, m: LongArray, t: Long, last: Boolean) {
        val v = LongArray(16)
        System.arraycopy(h, 0, v, 0, 8)
        System.arraycopy(IV, 0, v, 8, 8)
        v[12] = v[12] xor t
        if (last) v[14] = v[14].inv()

        for (round in 0 until 12) {
            val s = SIGMA[round % 10]
            g(v, 0, 4, 8, 12, m[s[0]], m[s[1]])
            g(v, 1, 5, 9, 13, m[s[2]], m[s[3]])
            g(v, 2, 6, 10, 14, m[s[4]], m[s[5]])
            g(v, 3, 7, 11, 15, m[s[6]], m[s[7]])
            g(v, 0, 5, 10, 15, m[s[8]], m[s[9]])
            g(v, 1, 6, 11, 12, m[s[10]], m[s[11]])
            g(v, 2, 7, 8, 13, m[s[12]], m[s[13]])
            g(v, 3, 4, 9, 14, m[s[14]], m[s[15]])
        }
        for (i in 0 until 8) h[i] = h[i] xor v[i] xor v[i + 8]
    }

    private fun g(v: LongArray, a: Int, b: Int, c: Int, d: Int, x: Long, y: Long) {
        v[a] = v[a] + v[b] + x
        v[d] = java.lang.Long.rotateRight(v[d] xor v[a], 32)
        v[c] = v[c] + v[d]
        v[b] = java.lang.Long.rotateRight(v[b] xor v[c], 24)
        v[a] = v[a] + v[b] + y
        v[d] = java.lang.Long.rotateRight(v[d] xor v[a], 16)
        v[c] = v[c] + v[d]
        v[b] = java.lang.Long.rotateRight(v[b] xor v[c], 63)
    }
}
