package com.simplefox.browser.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.util.Locale

/**
 * Central place for validating anything that arrives from outside the app (other apps' intents,
 * `intent:` links coming from web pages, extension install sources) before it is acted on.
 */
object IntentSecurity {

    private val WEB_SCHEMES = setOf("http", "https")

    private const val GRANT_FLAGS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

    /** Schemes that must never be forwarded to another app from web content. */
    private val BLOCKED_FORWARD_SCHEMES = setOf("file", "content", "javascript", "data", "blob", "intent")

    /**
     * URL accepted from an external launch intent (ACTION_VIEW data / ACTION_SEND text).
     * Only web URLs, about:blank and plain search text are allowed; javascript:, data:, file:,
     * content:, view-source:, moz-extension: and other privileged schemes are dropped.
     */
    fun sanitizeLaunchUrl(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val scheme = runCatching { Uri.parse(text).scheme }.getOrNull()?.lowercase(Locale.US)
        return when {
            scheme == null -> text
            scheme in WEB_SCHEMES -> text
            text.equals("about:blank", ignoreCase = true) -> text
            else -> null
        }
    }

    /** True when [url] is a plain http(s) URL (used for `browser_fallback_url`). */
    fun isWebUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val scheme = runCatching { Uri.parse(url.trim()).scheme }.getOrNull()?.lowercase(Locale.US)
        return scheme in WEB_SCHEMES
    }

    /**
     * Neutralises an Intent built from an untrusted `intent:` URI so a web page cannot use it to
     * reach this app's own components, grant itself URI permissions or smuggle in a selector.
     * Returns null when the intent must not be launched at all.
     */
    fun sanitizeExternalIntent(context: Context, intent: Intent): Intent? {
        if (intent.`package` == context.packageName) return null
        if (intent.component?.packageName == context.packageName) return null
        val dataScheme = intent.data?.scheme?.lowercase(Locale.US)
        if (dataScheme != null && dataScheme in BLOCKED_FORWARD_SCHEMES) return null
        intent.component = null
        intent.selector = null
        intent.sourceBounds = null
        intent.clipData = null
        intent.flags = intent.flags and GRANT_FLAGS.inv()
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        return intent
    }

    /** True when [file] lives inside this app's private storage (profile, databases, prefs…). */
    fun isInPrivateAppStorage(context: Context, file: File): Boolean {
        val canonical = runCatching { file.canonicalPath }.getOrNull() ?: return true
        val roots = listOfNotNull(
            context.applicationInfo.dataDir,
            context.applicationInfo.deviceProtectedDataDir,
            context.filesDir?.parent
        )
        return roots.any { root ->
            val r = runCatching { File(root).canonicalPath }.getOrNull() ?: return@any false
            canonical == r || canonical.startsWith(r + File.separator)
        }
    }
}
