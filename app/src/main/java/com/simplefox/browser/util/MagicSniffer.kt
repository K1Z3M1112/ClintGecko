package com.simplefox.browser.util

/**
 * Small content sniffer for the formats that commonly show up in downloads and are not covered
 * by the explicit checks in DownloadFileHelper. Returns a file extension or null.
 */
object MagicSniffer {

    fun detectExtension(b: ByteArray): String? {
        if (b.size < 4) return null
        if (startsWith(b, 0x50, 0x4B, 0x03, 0x04)) return zipFlavour(b)
        return signatures.firstOrNull { (offset, magic, _) -> matches(b, offset, magic) }?.third
            ?: tarOrText(b)
    }

    private val signatures: List<Triple<Int, ByteArray, String>> = listOf(
        Triple(0, bytes(0x49, 0x49, 0x2A, 0x00), "tif"),
        Triple(0, bytes(0x4D, 0x4D, 0x00, 0x2A), "tif"),
        Triple(0, bytes(0x00, 0x00, 0x01, 0x00), "ico"),
        Triple(0, bytes(0x77, 0x4F, 0x46, 0x46), "woff"),
        Triple(0, bytes(0x77, 0x4F, 0x46, 0x32), "woff2"),
        Triple(0, bytes(0x00, 0x01, 0x00, 0x00, 0x00), "ttf"),
        Triple(0, bytes(0x4F, 0x54, 0x54, 0x4F), "otf"),
        Triple(0, "SQLite format 3".toByteArray(Charsets.US_ASCII), "sqlite"),
        Triple(0, bytes(0x7F, 0x45, 0x4C, 0x46), "elf"),
        Triple(0, bytes(0x4D, 0x5A), "exe"),
        Triple(0, bytes(0x42, 0x5A, 0x68), "bz2"),
        Triple(0, bytes(0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00), "xz"),
        Triple(0, bytes(0x28, 0xB5, 0x2F, 0xFD), "zst"),
        Triple(0, "{\\rtf".toByteArray(Charsets.US_ASCII), "rtf"),
        Triple(0, "8BPS".toByteArray(Charsets.US_ASCII), "psd"),
        Triple(0, bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1), "doc"),
        Triple(0, "wvpk".toByteArray(Charsets.US_ASCII), "wv"),
        Triple(0, "MThd".toByteArray(Charsets.US_ASCII), "mid"),
        Triple(0, "FORM".toByteArray(Charsets.US_ASCII), "aiff"),
        Triple(0, "OggS".toByteArray(Charsets.US_ASCII), "ogg"),
        Triple(0, "dex\n".toByteArray(Charsets.US_ASCII), "dex")
    )

    /** OOXML / ODF / EPUB / JAR / APK are all ZIP containers; the first entry names identify them. */
    private fun zipFlavour(b: ByteArray): String {
        val nameLen = u16le(b, 26)
        val extraLen = u16le(b, 28)
        val nameEnd = 30 + nameLen
        if (nameLen <= 0 || nameEnd > b.size) return "zip"
        val name = String(b, 30, nameLen, Charsets.ISO_8859_1)
        return when {
            name == "mimetype" -> {
                val dataStart = nameEnd + extraLen
                val head = if (dataStart < b.size) String(b, dataStart, minOf(60, b.size - dataStart), Charsets.ISO_8859_1) else ""
                when {
                    head.contains("application/epub+zip") -> "epub"
                    head.contains("opendocument.text") -> "odt"
                    head.contains("opendocument.spreadsheet") -> "ods"
                    head.contains("opendocument.presentation") -> "odp"
                    else -> "zip"
                }
            }
            name.startsWith("word/") -> "docx"
            name.startsWith("xl/") -> "xlsx"
            name.startsWith("ppt/") -> "pptx"
            name == "AndroidManifest.xml" -> "apk"
            name.startsWith("META-INF/") -> if (containsAscii(b, "AndroidManifest.xml")) "apk" else "jar"
            name == "[Content_Types].xml" -> when {
                containsAscii(b, "word/") -> "docx"
                containsAscii(b, "xl/") -> "xlsx"
                containsAscii(b, "ppt/") -> "pptx"
                else -> "zip"
            }
            else -> "zip"
        }
    }

    private fun tarOrText(b: ByteArray): String? {
        if (b.size >= 262 && matches(b, 257, "ustar".toByteArray(Charsets.US_ASCII))) return "tar"
        var start = 0
        if (b.size >= 3 && (b[0].toInt() and 0xFF) == 0xEF && (b[1].toInt() and 0xFF) == 0xBB && (b[2].toInt() and 0xFF) == 0xBF) start = 3
        while (start < b.size && b[start].toInt().toChar().isWhitespace()) start++
        val head = String(b, start, minOf(256, b.size - start), Charsets.ISO_8859_1).lowercase()
        return when {
            head.startsWith("<!doctype html") || head.startsWith("<html") -> "html"
            head.startsWith("<svg") || (head.startsWith("<?xml") && head.contains("<svg")) -> "svg"
            head.startsWith("<?xml") -> "xml"
            else -> null
        }
    }

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    private fun startsWith(b: ByteArray, vararg v: Int): Boolean =
        b.size >= v.size && v.indices.all { (b[it].toInt() and 0xFF) == v[it] }

    private fun matches(b: ByteArray, offset: Int, magic: ByteArray): Boolean {
        if (b.size < offset + magic.size) return false
        for (i in magic.indices) if (b[offset + i] != magic[i]) return false
        return true
    }

    private fun u16le(b: ByteArray, off: Int): Int =
        if (off + 1 < b.size) (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8) else 0

    private fun containsAscii(b: ByteArray, needle: String): Boolean {
        val n = needle.toByteArray(Charsets.US_ASCII)
        outer@ for (i in 0..b.size - n.size) {
            for (j in n.indices) if (b[i + j] != n[j]) continue@outer
            return true
        }
        return false
    }
}
