package com.simplefox.browser.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SetupUiState(
    initialPage: Int,
    initialTheme: String,
    initialLanguage: String,
    initialAddressBarPosition: String,
    initialScrollHideMode: String,
    initialHideStatusBar: Boolean, initialHideSystemNavigation: Boolean,
    initialEngine: String,
    initialCustomEngineName: String, initialCustomEngineUrl: String
) {
    var currentPage by mutableStateOf(initialPage)
    var consentChecked by mutableStateOf(false)

    var theme by mutableStateOf(initialTheme)
    var language by mutableStateOf(initialLanguage)

    var addressBarPosition by mutableStateOf(initialAddressBarPosition)
    var scrollHideMode by mutableStateOf(initialScrollHideMode)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)

    var engine by mutableStateOf(initialEngine)
    var customEngineName by mutableStateOf(initialCustomEngineName)
    var customEngineUrl by mutableStateOf(initialCustomEngineUrl)


    var confirmDialogConfig by mutableStateOf<com.simplefox.browser.ui.listscreen.ConfirmDialogConfig?>(null)
}
