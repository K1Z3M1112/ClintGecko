package com.simplefox.browser.browser.delegates

import android.webkit.JavascriptInterface
import com.simplefox.browser.browser.webview.ClintWebView
import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.browser.dialogs.ColorPickerRequest
import java.lang.ref.WeakReference

class ColorPickerBridge(private val activity: MainActivity, private val webView: ClintWebView) {

    @JavascriptInterface
    fun onColorOpen(id: String, value: String) {
        activity.runOnUiThread {
            if (activity.tabManager.activeTab?.webView !== webView) return@runOnUiThread
            activity.uiState.colorPickerRequest = ColorPickerRequest(id, value, WeakReference(webView))
        }
    }
}
