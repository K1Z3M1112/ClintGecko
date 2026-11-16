package com.simplefox.browser.browser.delegates

import com.simplefox.browser.R
import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.browser.dialogs.PopupAlertRequest
import com.simplefox.browser.ui.showClintDestinationSnackbar

internal fun MainActivity.showPopupAlertDialog(newUrl: String, isIncognito: Boolean, sourceTabId: String? = null) {
    if (uiState.isFullscreen) return
    val sourceTab = sourceTabId?.let { id -> tabManager.tabs.firstOrNull { it.id == id } } ?: tabManager.activeTab
    val sourceUrl = sourceTab?.webView?.url
    val sourceHost = sourceUrl
        ?.let { android.net.Uri.parse(it).host?.takeIf { h -> h.isNotEmpty() } ?: it }
        ?: getString(R.string.popup_alert_source_unknown)

    val onAllow: () -> Unit = onAllow@{
        if (com.simplefox.browser.settings.datasaver.DataSaverMode.isSingleTabActive(prefs) &&
            sourceTabId != null && sourceTabId != tabManager.activeTab?.id) return@onAllow
        val uri = android.net.Uri.parse(newUrl)
        val scheme = uri.scheme?.lowercase()
        val activeWebView = tabManager.activeTab?.webView
        val client = activeWebView?.webViewClient
        if (scheme == "http" || scheme == "https") {
            if (client == null || !client.tryOpenInApp(activeWebView, uri)) {
                openNewTabOrReuse(isIncognito, newUrl, sourceTabId)
            }
        } else {
            openNewTabOrReuse(isIncognito, newUrl, sourceTabId)
        }
    }

    if (isSameOrigin(sourceUrl, newUrl)) {
        onAllow()
        return
    }

    if (prefs.getString("popup_alert_style", "dialog") == "snackbar") {
        uiState.popupAlertRequest = null
        showClintDestinationSnackbar(
            message = getString(R.string.popup_alert_snackbar_message, sourceHost),
            destination = newUrl,
            actionLabel = getString(R.string.popup_alert_snackbar_action),
            onAction = onAllow
        )
        return
    }

    uiState.popupAlertRequest = PopupAlertRequest(
        sourceHost = sourceHost,
        newUrl = newUrl,
        onAllow = onAllow
    )
}

private fun isSameOrigin(sourceUrl: String?, targetUrl: String): Boolean {
    val source = sourceUrl?.let { android.net.Uri.parse(it) } ?: return false
    val target = android.net.Uri.parse(targetUrl)
    val sourceScheme = source.scheme?.lowercase() ?: return false
    val targetScheme = target.scheme?.lowercase() ?: return false
    if (sourceScheme != targetScheme || (sourceScheme != "http" && sourceScheme != "https")) return false
    val sourceHost = source.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
    val targetHost = target.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
    return sourceHost == targetHost && effectivePort(source, sourceScheme) == effectivePort(target, targetScheme)
}

private fun effectivePort(uri: android.net.Uri, scheme: String): Int =
    uri.port.takeIf { it != -1 } ?: if (scheme == "https") 443 else 80
