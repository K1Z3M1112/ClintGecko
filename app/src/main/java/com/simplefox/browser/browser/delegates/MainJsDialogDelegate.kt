package com.simplefox.browser.browser.delegates

import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.browser.dialogs.JsDialogRequest

internal fun MainActivity.onJsDialogRequest(request: JsDialogRequest) {
    uiState.jsDialogRequest?.cancel()
    uiState.jsDialogRequest = request
}
