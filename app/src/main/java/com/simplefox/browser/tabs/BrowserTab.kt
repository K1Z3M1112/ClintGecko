package com.simplefox.browser.tabs

import com.simplefox.browser.browser.webview.ClintWebView
import com.simplefox.browser.profiles.WebProfiles
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Tab",
    var url: String = "",
    val isIncognito: Boolean = false,
    val isRefreshLinkTab: Boolean = false,
    val openerTabId: String? = null,
    val shortcutId: String? = null,
    val previousTabId: String? = null,
    var pendingUrl: String? = null,
    val profileId: String = WebProfiles.DEFAULT_ID,
    var groupId: String? = null,
    val webView: ClintWebView
) {
    var lastActiveAt: Long = System.currentTimeMillis()

    /** How often the user switched to this tab this session; feeds the keep-alive score. */
    var visitCount: Int = 0

    init {
        WebProfiles.registerTab(id, profileId)
    }
}

/** The address worth saving/reopening for this tab, or null when it has nothing but a blank page. */
fun BrowserTab.restorableUrl(): String? =
    listOf(pendingUrl, webView.url, url).firstOrNull { !it.isNullOrEmpty() && it != "about:blank" }
