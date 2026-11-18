package com.simplefox.browser.blocker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object WebsiteBlockerDownloader {
    private const val PROGRESS_INTERVAL_NS = 100_000_000L
    private val client = OkHttpClient()

    sealed class Result {
        data class Success(val etag: String?, val lastModified: String?, val bytesWritten: Long) : Result()
        object NotModified : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun download(
        url: String,
        destination: File,
        etag: String?,
        lastModified: String?,
        onProgress: (bytesRead: Long, contentLength: Long) -> Unit
    ): Result = withContext(Dispatchers.IO) {
        val tempFile = File(destination.parentFile, "${destination.name}.tmp")
        try {
            val requestBuilder = Request.Builder().url(url)
            if (!etag.isNullOrBlank()) requestBuilder.header("If-None-Match", etag)
            if (!lastModified.isNullOrBlank()) requestBuilder.header("If-Modified-Since", lastModified)

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 304) return@withContext Result.NotModified
                if (!response.isSuccessful) return@withContext Result.Failure("HTTP ${response.code}")

                val body = response.body
                val contentLength = body.contentLength()
                var bytesRead = 0L
                var lastReportNs = 0L
                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesRead += read
                            // Progress writes Compose state; throttle so large lists don't trigger thousands of recompositions.
                            val now = System.nanoTime()
                            if (now - lastReportNs >= PROGRESS_INTERVAL_NS) {
                                lastReportNs = now
                                onProgress(bytesRead, contentLength)
                            }
                        }
                    }
                }

                onProgress(bytesRead, contentLength)

                val saved = runCatching {
                    java.nio.file.Files.move(
                        tempFile.toPath(), destination.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                    )
                }.isSuccess || run {
                    if (destination.exists()) destination.delete()
                    tempFile.renameTo(destination)
                }
                if (!saved) {
                    tempFile.delete()
                    return@withContext Result.Failure("Failed to save file")
                }

                Result.Success(
                    etag = response.header("ETag"),
                    lastModified = response.header("Last-Modified"),
                    bytesWritten = bytesRead
                )
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            Result.Failure(e.message ?: "Download failed")
        }
    }
}
