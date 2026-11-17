package com.simplefox.browser.tabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import com.simplefox.browser.browser.webview.ClintWebView
import java.io.File
import java.io.FileOutputStream
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object TabThumbnailCache {
    private const val MAX_DIMENSION_PX = 240
    const val DIR_NAME = "tab_thumbnails"
    private const val WEBP_QUALITY = 75

    // Disk I/O and bitmap processing use separate threads so a slow disk write never delays a fresh thumbnail.
    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tab-thumbnail-io").apply { priority = Thread.MIN_PRIORITY }
    }
    private val processExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tab-thumbnail-process")
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Emits the tab id every time its thumbnail changes, so the tab switcher can refresh instantly. */
    private val _updates = MutableSharedFlow<String>(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val updates: SharedFlow<String> = _updates

    // Latest bitmap waiting to be written to disk per tab, so rapid captures collapse into a single write.
    private val pendingDisk = ConcurrentHashMap<String, Bitmap>()

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 10L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private val incognitoCache = ConcurrentHashMap<String, Bitmap>()

    fun capture(tabId: String, webView: ClintWebView, isIncognito: Boolean = false) {
        captureThen(tabId, webView, isIncognito) {}
    }

    /**
     * Grabs the tab's frame right now and runs [then] as soon as the raw frame is in hand (before the view is
     * detached or deactivated), not after the downscale/disk work. If the page cannot be captured, or the
     * compositor does not answer within [timeoutMs], [then] still runs so switching tabs is never blocked.
     */
    fun captureThen(
        tabId: String,
        webView: ClintWebView,
        isIncognito: Boolean = false,
        timeoutMs: Long = 150L,
        then: () -> Unit
    ) {
        if (!webView.canCaptureThumbnail) {
            then()
            return
        }
        val context = webView.context.applicationContext
        val finished = AtomicBoolean(false)
        val timeout = Runnable { if (finished.compareAndSet(false, true)) then() }
        mainHandler.postDelayed(timeout, timeoutMs)
        webView.captureBitmap { bitmap ->
            if (bitmap != null) {
                // Raw capture is screen-sized (~10 MB): process and release it off the main thread.
                processExecutor.execute {
                    try {
                        put(context, tabId, bitmap, isIncognito)
                    } finally {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                }
            }
            mainHandler.post {
                mainHandler.removeCallbacks(timeout)
                if (finished.compareAndSet(false, true)) then()
            }
        }
    }

    fun put(context: Context, tabId: String, source: Bitmap, isIncognito: Boolean = false) {
        if (source.width <= 0 || source.height <= 0) return
        runCatching {
            val bitmap = shrink(source)
            store(context.applicationContext, tabId, bitmap, isIncognito)
        }
    }

    /** Scales so the longest side is at most MAX_DIMENSION_PX. Never recycles [source] and never returns it. */
    private fun shrink(source: Bitmap): Bitmap {
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
        // The caller recycles [source], so never hand it back as the stored thumbnail.
        return if (current === source) source.copy(Bitmap.Config.ARGB_8888, false) else current
    }

    fun peek(tabId: String): Bitmap? = incognitoCache[tabId] ?: cache.get(tabId)

    fun get(context: Context, tabId: String): Bitmap? {
        peek(tabId)?.let { return it }
        val fromDisk = readFromDisk(context.applicationContext, tabId) ?: return null
        cache.put(tabId, fromDisk)
        return fromDisk
    }

    /** Decodes saved thumbnails into memory ahead of time so the tab switcher's first frame already has them. */
    fun preload(context: Context, tabIds: List<String>) {
        val appContext = context.applicationContext
        val missing = tabIds.filter { peek(it) == null }
        if (missing.isEmpty()) return
        ioExecutor.execute {
            missing.forEach { id ->
                if (peek(id) == null) readFromDisk(appContext, id)?.let { cache.put(id, it) }
            }
        }
    }

    fun evict(context: Context, tabId: String) {
        cache.remove(tabId)
        incognitoCache.remove(tabId)
        pendingDisk.remove(tabId)
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
            _updates.tryEmit(tabId)
            return
        }
        cache.put(tabId, bitmap)
        _updates.tryEmit(tabId)
        val hadPending = pendingDisk.put(tabId, bitmap) != null
        if (!hadPending) {
            ioExecutor.execute {
                val latest = pendingDisk.remove(tabId) ?: return@execute
                writeToDisk(context, tabId, latest)
            }
        }
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
            // Files written by older versions can be larger (up to 720 px); bring them to the current size.
            if (maxOf(decoded.width, decoded.height) <= MAX_DIMENSION_PX) return@runCatching decoded
            val reduced = shrink(decoded)
            decoded.recycle()
            reduced
        }.getOrNull()
    }
}
