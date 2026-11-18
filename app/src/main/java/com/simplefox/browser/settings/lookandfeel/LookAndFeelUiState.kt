package com.simplefox.browser.settings.lookandfeel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class LookAndFeelDialog {
    THEME, ACCENT, SURFACE_INTENSITY, ADDRESS_BAR_POSITION, SCROLL_HIDE_MODE, EXIT_CONFIRMATION, POPUP_ALERT_STYLE, LANGUAGE,
    TAB_STRIP_POSITION, TAB_GROUP_BAR_POSITION, TAB_PREVIEW_RESOLUTION
}

class LookAndFeelUiState(
    initialTheme: String,
    initialAccent: String,
    initialIntensity: String,
    initialLanguage: String,
    initialScrollHideMode: String,
    initialAddressBarPosition: String,
    initialHideStatusBar: Boolean, initialHideSystemNavigation: Boolean,
    initialShowHomeButton: Boolean,
    initialShowTabStrip: Boolean,
    initialTabStripPosition: String,
    initialTabGroupsEnabled: Boolean,
    initialTabGroupBarPosition: String,
    initialTabPreviewResolution: String,
    initialExitConfirmation: String,
    initialPopupAlertStyle: String
) {
    var theme by mutableStateOf(initialTheme)
    var accent by mutableStateOf(initialAccent)
    var intensity by mutableStateOf(initialIntensity)
    var language by mutableStateOf(initialLanguage)

    var scrollHideMode by mutableStateOf(initialScrollHideMode)
    var addressBarPosition by mutableStateOf(initialAddressBarPosition)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)
    var showHomeButton by mutableStateOf(initialShowHomeButton)
    var showTabStrip by mutableStateOf(initialShowTabStrip)
    var tabStripPosition by mutableStateOf(initialTabStripPosition)
    var tabGroupsEnabled by mutableStateOf(initialTabGroupsEnabled)
    var tabGroupBarPosition by mutableStateOf(initialTabGroupBarPosition)
    var tabPreviewResolution by mutableStateOf(initialTabPreviewResolution)

    var exitConfirmation by mutableStateOf(initialExitConfirmation)
    var popupAlertStyle by mutableStateOf(initialPopupAlertStyle)

    var openDialog by mutableStateOf<LookAndFeelDialog?>(null)
}
