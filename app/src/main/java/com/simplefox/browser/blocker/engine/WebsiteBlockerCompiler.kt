package com.simplefox.browser.blocker.engine

import android.content.Context
import com.simplefox.browser.blocker.WebsiteBlockerCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

object WebsiteBlockerCompiler {

    suspend fun compile(
        context: Context,
        categories: List<WebsiteBlockerCategory>,
        additionalWebsiteHosts: List<String>,
        onProgress: suspend (WebsiteBlockerCompileProgress) -> Unit
    ): WebsiteBlockerCompileResult = withContext(Dispatchers.IO) {
        val activeCategories = categories.filter { it.isEnabled && it.isDownloaded }
        val totalItems = activeCategories.size + 1
        var processed = 0

        val builderPtr = WebsiteBlockerNative.nativeCreateBuilder()
        if (builderPtr == 0L) {
            return@withContext WebsiteBlockerCompileResult(false, 0, 0, "Failed to initialize compiler")
        }

        try {
            for (category in activeCategories) {
                ensureActive()
                onProgress(WebsiteBlockerCompileProgress(category.id, processed, totalItems))
                val file = WebsiteBlockerPaths.categoryFile(context, category.id)
                if (file.exists()) {
                    WebsiteBlockerNative.nativeAddFile(builderPtr, file)
                }
                processed++
            }

            onProgress(WebsiteBlockerCompileProgress("additional", processed, totalItems))
            val additionalText = additionalWebsiteHosts.joinToString("\n")
            val additionalFile = WebsiteBlockerPaths.additionalWebsitesFile(context)
            additionalFile.writeText(additionalText)
            if (additionalText.isNotBlank()) {
                WebsiteBlockerNative.nativeAddText(builderPtr, additionalText)
            }
            processed++

            val tempFile = WebsiteBlockerPaths.engineTempFile(context)
            if (tempFile.exists()) tempFile.delete()

            val resultJson = JSONObject(
                WebsiteBlockerNative.nativeFinalizeBuilder(builderPtr, tempFile.absolutePath)
            )
            val success = resultJson.optBoolean("success", false)
            val domainCount = resultJson.optLong("domainCount", 0L)
            val sizeBytes = resultJson.optLong("sizeBytes", 0L)

            if (!success || !tempFile.exists()) {
                return@withContext WebsiteBlockerCompileResult(false, 0, 0, "Failed to write compiled database")
            }

            val engineFile = WebsiteBlockerPaths.engineFile(context)
            // Atomic replace: a crash mid-swap must never leave the device without a database.
            val replaced = runCatching {
                java.nio.file.Files.move(
                    tempFile.toPath(), engineFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
                )
            }.isSuccess || run {
                if (engineFile.exists()) engineFile.delete()
                tempFile.renameTo(engineFile)
            }
            if (!replaced) {
                return@withContext WebsiteBlockerCompileResult(false, 0, 0, "Failed to activate compiled database")
            }

            CompiledWebsiteBlockerManifest(
                compiledAt = System.currentTimeMillis(),
                domainCount = domainCount,
                sizeBytes = sizeBytes,
                enabledCategoryIds = activeCategories.map { it.id }.toSet(),
                additionalWebsitesCount = additionalWebsiteHosts.size
            ).write(WebsiteBlockerPaths.manifestFile(context))

            WebsiteBlockerCompileResult(true, domainCount, sizeBytes)
        } finally {
            WebsiteBlockerNative.nativeDestroyBuilder(builderPtr)
        }
    }
}
