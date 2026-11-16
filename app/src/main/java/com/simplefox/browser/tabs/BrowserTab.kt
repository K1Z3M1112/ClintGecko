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

    init {
        WebProfiles.registerTab(id, profileId)
    }
}
