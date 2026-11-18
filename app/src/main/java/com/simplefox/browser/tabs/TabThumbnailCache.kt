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
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object TabThumbnailCache {
    const val PREF_RESOLUTION = "tab_preview_resolution"
    const val RES_LOW = "low"
    const val RES_STANDARD = "standard"
    const val RES_HIGH = "high"
    const val RES_VERY_HIGH = "very_high"
    const val DEFAULT_RESOLUTION = RES_STANDARD

    /** Longest side, in pixels, of a stored tab preview for each resolution choice. */
    fun resolutionPx(key: String?): Int = when (key) {
        RES_LOW -> 120
        RES_HIGH -> 300
        RES_VERY_HIGH -> 480
        else -> 180
    }

    fun normalizeResolution(key: String?): String = when (key) {
        RES_LOW, RES_HIGH, RES_VERY_HIGH -> key
        else -> RES_STANDARD
    }

    @Volatile private var maxDimensionPx = resolutionPx(DEFAULT_RESOLUTION)
    private const val BASE_DIMENSION_PX = 180
    private const val MAX_CACHE_SCALE = 3

    const val DIR_NAME = "tab_thumbnails"
    private const val WEBP_QUALITY = 60
    private const val MEMORY_CACHE_BYTES = 4 * 1024 * 1024
    private const val INCOGNITO_CACHE_BYTES = 1024 * 1024
    private const val MAX_PRELOAD_THUMBNAILS = 9
    private const val MAX_PENDING_CAPTURES = 2

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
    private val pendingCaptureCount = AtomicInteger(0)

    // Keep thumbnail memory predictable even on devices with a large Java heap.
    private val cache = object : LruCache<String, Bitmap>(MEMORY_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    // Private thumbnails are memory-only and have a much smaller independent budget.
    private val incognitoCache = object : LruCache<String, Bitmap>(INCOGNITO_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

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
                // Rapid tab switching must not queue dozens of full-screen bitmaps.
                // Keep at most one running capture and two waiting captures.
                if (pendingCaptureCount.incrementAndGet() <= MAX_PENDING_CAPTURES + 1) {
                    runCatching {
                        processExecutor.execute {
                            try {
                                put(context, tabId, bitmap, isIncognito)
                            } finally {
                                if (!bitmap.isRecycled) bitmap.recycle()
                                pendingCaptureCount.decrementAndGet()
                            }
                        }
                    }.onFailure {
                        pendingCaptureCount.decrementAndGet()
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                } else {
                    pendingCaptureCount.decrementAndGet()
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
            mainHandler.post {
                mainHandler.removeCallbacks(timeout)
                if (finished.compareAndSet(false, true)) then()
            }
        }
    }

    /**
     * Re-reads the preview resolution setting. When it changed, the in-memory previews (made at the old size) are
     * dropped so every tab picks up the new size; previews on disk are rescaled when read or replaced on next capture.
     */
    @Synchronized
    fun refreshResolution(context: Context) {
        val key = normalizeResolution(
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
                .getString(PREF_RESOLUTION, DEFAULT_RESOLUTION)
        )
        val px = resolutionPx(key)
        if (px == maxDimensionPx) return
        maxDimensionPx = px
        // Memory budget grows with the chosen size (capped) so higher quality does not mean fewer cached tabs.
        val scale = (px.toFloat() / BASE_DIMENSION_PX).coerceIn(1f, MAX_CACHE_SCALE.toFloat())
        cache.resize((MEMORY_CACHE_BYTES * scale).toInt())
        incognitoCache.resize((INCOGNITO_CACHE_BYTES * scale).toInt())
        cache.evictAll()
        incognitoCache.evictAll()
    }

    fun put(context: Context, tabId: String, source: Bitmap, isIncognito: Boolean = false) {
        if (source.width <= 0 || source.height <= 0) return
        refreshResolution(context)
        runCatching {
            val bitmap = shrink(source)
            store(context.applicationContext, tabId, bitmap, isIncognito)
        }
    }

    /** Scales so the longest side is at most the chosen resolution. Never recycles [source] and never returns it. */
    private fun shrink(source: Bitmap): Bitmap {
        val maxDimension = maxDimensionPx
        var current = source
        // Halve repeatedly instead of one big jump; a single bilinear resize by >2x skips pixels and aliases.
        while (maxOf(current.width, current.height) >= maxDimension * 2) {
            val next = Bitmap.createScaledBitmap(current, (current.width / 2).coerceAtLeast(1), (current.height / 2).coerceAtLeast(1), true)
            if (current !== source) current.recycle()
            current = next
        }
        val longest = maxOf(current.width, current.height)
        if (longest > maxDimension) {
            val scale = maxDimension.toFloat() / longest
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

    fun peek(tabId: String): Bitmap? = incognitoCache.get(tabId) ?: cache.get(tabId)

    fun get(context: Context, tabId: String): Bitmap? {
        peek(tabId)?.let { return it }
        val fromDisk = readFromDisk(context.applicationContext, tabId) ?: return null
        cache.put(tabId, fromDisk)
        return fromDisk
    }

    /**
     * Preloads only a small window around the active tab. Loading every thumbnail here used to
     * decode and retain images for the entire session before the lazy grid was even visible.
     */
    fun preload(context: Context, tabIds: List<String>, focusedTabId: String? = null) {
        if (tabIds.isEmpty()) return
        val focusIndex = focusedTabId?.let { tabIds.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        val halfWindow = MAX_PRELOAD_THUMBNAILS / 2
        val start = (focusIndex - halfWindow).coerceAtLeast(0)
            .coerceAtMost((tabIds.size - MAX_PRELOAD_THUMBNAILS).coerceAtLeast(0))
        val end = (start + MAX_PRELOAD_THUMBNAILS).coerceAtMost(tabIds.size)
        val missing = tabIds.subList(start, end).filter { peek(it) == null }
        if (missing.isEmpty()) return
        val appContext = context.applicationContext
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
        incognitoCache.evictAll()
    }

    fun trimMemory(level: Int) {
        when {
            level >= 40 || level == 15 -> {
                cache.evictAll()
                incognitoCache.evictAll()
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
            incognitoCache.put(tabId, bitmap)
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
        refreshResolution(context)
        return runCatching {
            val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
            // Files written by older versions can be larger (larger than the current size); bring them to the current size.
            if (maxOf(decoded.width, decoded.height) <= maxDimensionPx) return@runCatching decoded
            val reduced = shrink(decoded)
            decoded.recycle()
            reduced
        }.getOrNull()
    }
}
