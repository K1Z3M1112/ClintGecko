package com.simplefox.browser.browser.delegates

import android.webkit.JavascriptInterface
import com.simplefox.browser.browser.webview.ClintWebView
import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.browser.dialogs.DateTimePickerRequest
import com.simplefox.browser.browser.dialogs.DateTimePickerType
import java.lang.ref.WeakReference

class DateTimePickerBridge(private val activity: MainActivity, private val webView: ClintWebView) {

    @JavascriptInterface
    fun onDateTimeOpen(id: String, type: String, value: String) {
        activity.runOnUiThread {
            if (activity.tabManager.activeTab?.webView !== webView) return@runOnUiThread
            val pickerType = DateTimePickerType.fromInputType(type) ?: return@runOnUiThread
            activity.uiState.dateTimePickerRequest = DateTimePickerRequest(id, pickerType, value, WeakReference(webView))
        }
    }
}
