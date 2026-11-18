package com.simplefox.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.LruCache
import com.simplefox.browser.util.PreferenceManager
import com.simplefox.browser.settings.datasaver.DataSaverMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Small, bounded favicon loader. All downloaded images are decoded to icon-sized bitmaps. */
object FaviconCache {

    private const val MAX_ICON_SIDE = 128
    private const val MAX_DOWNLOAD_BYTES = 768 * 1024
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 4000
    private const val MEMORY_CACHE_BYTES = 4 * 1024 * 1024

    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    // A bounded queue prevents a long list of uncached favicons from creating an unbounded
    // number of threads or retaining hundreds of pending requests.
    private val executor = ThreadPoolExecutor(
        2, 2, 30L, TimeUnit.SECONDS,
        ArrayBlockingQueue(48),
        { runnable -> Thread(runnable, "favicon-loader").apply { priority = Thread.MIN_PRIORITY } },
        ThreadPoolExecutor.AbortPolicy()
    ).apply { allowCoreThreadTimeOut(true) }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val inFlight = ConcurrentHashMap<String, MutableList<(Bitmap?) -> Unit>>()

    /** Synchronous in-memory lookup so composables can show a cached icon on the first frame. */
    fun peek(faviconUrl: String): Bitmap? =
        if (faviconUrl.isEmpty()) null else memoryCache.get(keyFor(faviconUrl))

    fun faviconUrlFor(pageUrl: String): String {
        return runCatching {
            val host = URL(pageUrl).host
            if (host.isBlank()) "" else "https://$host/favicon.ico"
        }.getOrDefault("")
    }

    private fun isNetworkBlocked(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return DataSaverMode.isActive(prefs) && prefs.getBoolean("data_saver_disable_favicons", true)
    }

    fun load(context: Context, faviconUrl: String, cacheOnly: Boolean = false, onResult: (Bitmap?) -> Unit) {
        if (faviconUrl.isEmpty()) {
            onResult(null)
            return
        }
        val key = keyFor(faviconUrl)
        memoryCache.get(key)?.let { onResult(it); return }

        val appContext = context.applicationContext
        val allowNetwork = !cacheOnly && !isNetworkBlocked(appContext)
        enqueue(key, allowNetwork, onResult) {
            val file = diskFile(appContext, key)
            val cached = if (file.isFile) decodeFile(file) else null
            if (cached != null) {
                memoryCache.put(key, cached)
                cached
            } else if (!allowNetwork) {
                null
            } else {
                val bitmap = tryFetch(faviconUrl) ?: tryFetch(fallbackUrlFor(faviconUrl))
                if (bitmap != null) {
                    memoryCache.put(key, bitmap)
                    runCatching {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
                bitmap
            }
        }
    }

    fun loadMemoryOnly(context: Context, faviconUrl: String, onResult: (Bitmap?) -> Unit) {
        if (faviconUrl.isEmpty()) {
            onResult(null)
            return
        }
        val key = keyFor(faviconUrl)
        memoryCache.get(key)?.let { onResult(it); return }
        val appContext = context.applicationContext
        if (isNetworkBlocked(appContext)) {
            onResult(null)
            return
        }
        enqueue("memory:$key", true, onResult) {
            (tryFetch(faviconUrl) ?: tryFetch(fallbackUrlFor(faviconUrl)))?.also {
                memoryCache.put(key, it)
            }
        }
    }

    private fun enqueue(
        requestKey: String,
        allowNetwork: Boolean,
        callback: (Bitmap?) -> Unit,
        work: () -> Bitmap?
    ) {
        // Separate cache-only and network requests, so a cache-only lookup never suppresses
        // a later request that is explicitly allowed to use the network.
        val key = if (allowNetwork) requestKey else "cache:$requestKey"
        synchronized(inFlight) {
            val callbacks = inFlight[key]
            if (callbacks != null) {
                callbacks.add(callback)
                return
            }
            inFlight[key] = mutableListOf(callback)
        }
        try {
            executor.execute {
                val result = runCatching(work).getOrNull()
                val callbacks = synchronized(inFlight) { inFlight.remove(key).orEmpty() }
                mainHandler.post { callbacks.forEach { cb -> runCatching { cb(result) } } }
            }
        } catch (_: RejectedExecutionException) {
            val callbacks = synchronized(inFlight) { inFlight.remove(key).orEmpty() }
            mainHandler.post { callbacks.forEach { cb -> runCatching { cb(null) } } }
        }
    }

    fun evict(context: Context, pageUrl: String) {
        val faviconUrl = faviconUrlFor(pageUrl)
        if (faviconUrl.isEmpty()) return
        val key = keyFor(faviconUrl)
        memoryCache.remove(key)
        diskFile(context.applicationContext, key).delete()
    }

    /** Releases decoded icons early when Android reports memory pressure. */
    fun trimMemory(level: Int) {
        when {
            level >= 40 || level == 15 -> memoryCache.evictAll()
            level >= 10 -> memoryCache.trimToSize(MEMORY_CACHE_BYTES / 2)
        }
    }

    private fun tryFetch(url: String): Bitmap? {
        if (url.isEmpty()) return null
        if (url.startsWith("data:", ignoreCase = true)) return decodeDataUri(url)
        return runCatching {
            val connection = URL(url).openConnection().apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = true
            }
            if (connection is HttpURLConnection) {
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "image/avif,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5")
            }
            try {
                val length = connection.contentLengthLong
                if (length > MAX_DOWNLOAD_BYTES) return@runCatching null
                val bytes = connection.getInputStream().use { readLimited(it, MAX_DOWNLOAD_BYTES) } ?: return@runCatching null
                decodeBytes(bytes)
            } finally {
                (connection as? HttpURLConnection)?.disconnect()
            }
        }.getOrNull()
    }

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray? {
        val output = ByteArrayOutputStream(minOf(limit, 16 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun decodeDataUri(uri: String): Bitmap? = runCatching {
        val comma = uri.indexOf(',')
        if (comma < 0) return@runCatching null
        val header = uri.substring(0, comma)
        val payload = uri.substring(comma + 1)
        val bytes = if (header.contains(";base64", ignoreCase = true)) {
            if (payload.length > MAX_DOWNLOAD_BYTES * 2) return@runCatching null
            Base64.decode(payload, Base64.DEFAULT)
        } else {
            if (payload.length > MAX_DOWNLOAD_BYTES) return@runCatching null
            URLDecoder.decode(payload, "UTF-8").toByteArray()
        }
        if (bytes.size > MAX_DOWNLOAD_BYTES) return@runCatching null
        decodeBytes(bytes)
    }.getOrNull()

    private fun decodeFile(file: File): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        val options = sampleOptions(bounds.outWidth, bounds.outHeight)
        BitmapFactory.decodeFile(file.absolutePath, options)?.let(::fitIcon)
    }.getOrNull()

    private fun decodeBytes(bytes: ByteArray): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        val options = sampleOptions(bounds.outWidth, bounds.outHeight)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.let(::fitIcon)
    }.getOrNull()

    private fun sampleOptions(width: Int, height: Int): BitmapFactory.Options {
        var sample = 1
        while (maxOf(width / sample, height / sample) > MAX_ICON_SIDE * 2) sample *= 2
        return BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    }

    private fun fitIcon(source: Bitmap): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= MAX_ICON_SIDE) return source
        val scale = MAX_ICON_SIDE.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== source && !source.isRecycled) source.recycle()
        return scaled
    }

    private fun fallbackUrlFor(faviconUrl: String): String {
        return runCatching {
            val host = URL(faviconUrl).host
            "https://icons.duckduckgo.com/ip3/$host.ico"
        }.getOrDefault("")
    }

    private fun keyFor(url: String): String = MessageDigest.getInstance("MD5")
        .digest(url.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun diskFile(context: Context, key: String): File {
        val dir = File(context.cacheDir, "favicons")
        return File(dir, "$key.png")
    }
}
