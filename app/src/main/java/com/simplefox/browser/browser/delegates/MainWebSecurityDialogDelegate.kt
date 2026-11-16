package com.simplefox.browser.browser.delegates

import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.browser.dialogs.HttpAuthRequest
import com.simplefox.browser.browser.dialogs.HttpsOnlyRequest
import com.simplefox.browser.browser.dialogs.SslWarningRequest

internal fun MainActivity.onHttpAuthRequest(request: HttpAuthRequest) {
    uiState.httpAuthRequest?.cancel()
    uiState.httpAuthRequest = request
}

internal fun MainActivity.onSslWarningRequest(request: SslWarningRequest) {
    uiState.sslWarningRequest?.cancel()
    uiState.sslWarningRequest = request
}

internal fun MainActivity.onHttpsOnlyBlockedRequest(request: HttpsOnlyRequest) {
    uiState.httpsOnlyRequest?.cancel()
    uiState.httpsOnlyRequest = request
}
