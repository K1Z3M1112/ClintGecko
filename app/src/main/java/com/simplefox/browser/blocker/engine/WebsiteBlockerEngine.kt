package com.simplefox.browser.blocker.engine

import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

object WebsiteBlockerEngine {
    @Volatile
    private var handle: Long = 0L

    @Volatile
    var isActive: Boolean = false
        private set

    @Volatile
    var domainCount: Long = 0L
        private set

    private val loadExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "WebsiteBlockerLoad").apply { isDaemon = true }
    }

    @Volatile
    private var pendingLoad: Future<*>? = null

    /** Loads and verifies the compiled database off the calling thread (app start, end of compile). */
    fun activateAsync(context: Context) {
        val appContext = context.applicationContext
        pendingLoad = loadExecutor.submit(Runnable { activate(appContext) })
    }

    /**
     * Waits for an in-flight async load (normally already finished, so this returns immediately) and
     * reports whether a database is active. This keeps early navigations from slipping past the blocker.
     */
    fun awaitActive(): Boolean {
        pendingLoad?.let { runCatching { it.get(3, TimeUnit.SECONDS) } }
        return isActive
    }

    @Synchronized
    fun activate(context: Context): Boolean {
        val file = WebsiteBlockerPaths.engineFile(context)
        if (!file.exists()) {
            deactivateLocked()
            return false
        }
        // The full checksum pass reads the whole (possibly 100MB+) file. Skip it when the file is
        // byte-for-byte the one we already verified (same size + mtime); header checks still run.
        val stampFile = WebsiteBlockerPaths.engineStampFile(context)
        val stamp = stampOf(file)
        val trusted = runCatching { stampFile.exists() && stampFile.readText() == stamp }.getOrDefault(false)
        val newHandle = WebsiteBlockerNative.nativeLoadDatabase(file.absolutePath, verifyChecksum = !trusted)
        if (newHandle == 0L) {
            runCatching { stampFile.delete() }
            deactivateLocked()
            return false
        }
        if (!trusted) runCatching { stampFile.writeText(stamp) }
        val oldHandle = handle
        handle = newHandle
        domainCount = WebsiteBlockerNative.nativeDomainCount(newHandle)
        isActive = true
        if (oldHandle != 0L) {
            WebsiteBlockerNative.nativeUnloadDatabase(oldHandle)
        }
        return true
    }

    private fun stampOf(file: java.io.File) = "${file.length()}:${file.lastModified()}"

    /** Called right after compile writes a fresh engine file whose checksum was computed while writing. */
    fun markVerified(context: Context) {
        val file = WebsiteBlockerPaths.engineFile(context)
        runCatching { WebsiteBlockerPaths.engineStampFile(context).writeText(stampOf(file)) }
    }

    private fun deactivateLocked() {
        if (handle != 0L) {
            WebsiteBlockerNative.nativeUnloadDatabase(handle)
        }
        handle = 0L
        isActive = false
        domainCount = 0L
    }

    fun isBlocked(host: String): Boolean {
        val activeHandle = handle
        if (activeHandle == 0L || host.isBlank()) return false
        return WebsiteBlockerNative.nativeIsBlocked(activeHandle, host)
    }
}
