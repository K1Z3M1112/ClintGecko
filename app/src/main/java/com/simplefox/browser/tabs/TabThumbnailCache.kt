package com.simplefox.browser.tabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import com.simplefox.browser.browser.webview.ClintWebView
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object TabThumbnailCache {
    private const val MAX_DIMENSION_PX = 240
    private const val BLUR_RADIUS_PX = 2
    const val DIR_NAME = "tab_thumbnails"
    private const val WEBP_QUALITY = 75

    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tab-thumbnail-io").apply { priority = Thread.MIN_PRIORITY }
    }

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 10L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private val incognitoCache = ConcurrentHashMap<String, Bitmap>()

    fun capture(tabId: String, webView: ClintWebView, isIncognito: Boolean = false) {
        if (webView.width <= 0 || webView.height <= 0) return
        val context = webView.context.applicationContext
        webView.captureBitmap { bitmap ->
            if (bitmap != null) {
                ioExecutor.execute {
                    // The raw capture is screen-sized (~10 MB). put() stores a downscaled copy, so release it right away.
                    try {
                        put(context, tabId, bitmap, isIncognito)
                    } finally {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                }
            }
        }
    }

    fun put(context: Context, tabId: String, source: Bitmap, isIncognito: Boolean = false) {
        if (source.width <= 0 || source.height <= 0) return
        runCatching {
            val bitmap = shrinkAndBlur(source)
            store(context.applicationContext, tabId, bitmap, isIncognito)
        }
    }

    /** Scales so the longest side is at most MAX_DIMENSION_PX, then applies a light blur. Never recycles [source]. */
    private fun shrinkAndBlur(source: Bitmap): Bitmap {
        var current = source
        // Halve repeatedly instead of one big jump; a single bilinear resize by >2x skips pixels and aliases.
        while (maxOf(current.width, current.height) >= MAX_DIMENSION_PX * 2) {
            val next = Bitmap.createScaledBitmap(current, (current.width / 2).coerceAtLeast(1), (current.height / 2).coerceAtLeast(1), true)
            if (current !== source) current.recycle()
            current = next
        }
        val longest = maxOf(current.width, current.height)
        if (longest > MAX_DIMENSION_PX) {
            val scale = MAX_DIMENSION_PX.toFloat() / longest
            val next = Bitmap.createScaledBitmap(
                current,
                (current.width * scale).toInt().coerceAtLeast(1),
                (current.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (current !== source) current.recycle()
            current = next
        }
        val blurred = boxBlur(current, BLUR_RADIUS_PX)
        if (current !== source) current.recycle()
        return blurred
    }

    /** Two-pass (horizontal + vertical) box blur. Cheap at thumbnail size and works on every API level. */
    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val first = IntArray(w * h)
        src.getPixels(first, 0, w, 0, 0, w, h)
        val second = IntArray(w * h)
        blurPass(first, second, w, h, radius, horizontal = true)
        blurPass(second, first, w, h, radius, horizontal = false)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(first, 0, w, 0, 0, w, h)
        return out
    }

    private fun blurPass(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val step = if (horizontal) 1 else w
        val div = 2 * radius + 1
        for (line in 0 until lines) {
            val base = if (horizontal) line * w else line
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            for (i in -radius..radius) {
                val p = src[base + i.coerceIn(0, len - 1) * step]
                a += p ushr 24
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
            }
            for (i in 0 until len) {
                dst[base + i * step] = ((a / div) shl 24) or ((r / div) shl 16) or ((g / div) shl 8) or (b / div)
                val add = src[base + (i + radius + 1).coerceAtMost(len - 1) * step]
                val rem = src[base + (i - radius).coerceAtLeast(0) * step]
                a += (add ushr 24) - (rem ushr 24)
                r += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                b += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }

    fun peek(tabId: String): Bitmap? = incognitoCache[tabId] ?: cache.get(tabId)

    fun get(context: Context, tabId: String): Bitmap? {
        peek(tabId)?.let { return it }
        val fromDisk = readFromDisk(context.applicationContext, tabId) ?: return null
        cache.put(tabId, fromDisk)
        return fromDisk
    }

    fun evict(context: Context, tabId: String) {
        cache.remove(tabId)
        incognitoCache.remove(tabId)
        val appContext = context.applicationContext
        ioExecutor.execute {
            cache.remove(tabId)
            incognitoCache.remove(tabId)
            diskFiles(appContext, tabId).forEach { it.delete() }
        }
    }

    fun clear() {
        cache.evictAll()
        incognitoCache.clear()
    }

    fun trimMemory(level: Int) {
        when {
            level >= 40 || level == 15 -> {
                cache.evictAll()
                incognitoCache.clear()
            }
            level >= 10 -> cache.trimToSize(cache.maxSize() / 2)
        }
    }

    fun pruneDisk(context: Context, keepTabIds: Set<String>) {
        val appContext = context.applicationContext
        ioExecutor.execute {
            runCatching {
                diskDir(appContext).listFiles()?.forEach { file ->
                    if (file.nameWithoutExtension !in keepTabIds) file.delete()
                }
            }
        }
    }

    private fun store(context: Context, tabId: String, bitmap: Bitmap, isIncognito: Boolean) {
        if (isIncognito) {
            incognitoCache[tabId] = bitmap
            return
        }
        cache.put(tabId, bitmap)
        ioExecutor.execute { writeToDisk(context, tabId, bitmap) }
    }

    private fun diskDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    private fun webpFile(context: Context, tabId: String): File =
        File(diskDir(context), "$tabId.webp")

    private fun legacyFile(context: Context, tabId: String): File =
        File(diskDir(context), "$tabId.png")

    private fun diskFiles(context: Context, tabId: String): List<File> =
        listOf(webpFile(context, tabId), legacyFile(context, tabId))

    @Suppress("DEPRECATION")
    private fun writeToDisk(context: Context, tabId: String, bitmap: Bitmap) {
        runCatching {
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            FileOutputStream(webpFile(context, tabId)).use { out ->
                bitmap.compress(format, WEBP_QUALITY, out)
            }
            legacyFile(context, tabId).delete()
        }
    }

    private fun readFromDisk(context: Context, tabId: String): Bitmap? {
        val file = diskFiles(context, tabId).firstOrNull { it.exists() } ?: return null
        return runCatching {
            val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
            // Files written by older versions are larger (up to 720 px) and sharp; bring them to the current spec.
            if (maxOf(decoded.width, decoded.height) <= MAX_DIMENSION_PX) return@runCatching decoded
            val reduced = shrinkAndBlur(decoded)
            decoded.recycle()
            reduced
        }.getOrNull()
    }
}
