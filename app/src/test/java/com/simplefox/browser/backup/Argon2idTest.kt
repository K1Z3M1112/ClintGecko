package com.simplefox.browser.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class Argon2idTest {

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    @Test
    fun blake2b_abc() {
        assertEquals(
            "ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d17d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923",
            hex(Blake2b.digest("abc".toByteArray(), 64))
        )
    }

    @Test
    fun argon2id_rfc9106_vector() {
        val tag = Argon2id.hash(
            password = ByteArray(32) { 1 }, salt = ByteArray(16) { 2 },
            memoryKB = 32, iterations = 3, parallelism = 4, outLen = 32,
            secret = ByteArray(8) { 3 }, associatedData = ByteArray(12) { 4 }
        )
        assertEquals("0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659", hex(tag))
    }
}
