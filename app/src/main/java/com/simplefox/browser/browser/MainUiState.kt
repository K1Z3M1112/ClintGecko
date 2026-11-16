package com.simplefox.browser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal enum class AddressBarPosition { TOP, BOTTOM, SPLIT }

internal enum class SuggestionType { CLINT_PAGE, BOOKMARK, HISTORY, SUGGESTION }

internal data class SuggestionItem(
    val query: String,
    val displayText: String,
    val type: SuggestionType
)

internal class MainUiState {

    var addressBarPosition by mutableStateOf(AddressBarPosition.TOP)

    var addressBarTextTop by mutableStateOf("")
    var addressBarTextBottom by mutableStateOf("")
    var addressBarSecureTop by mutableStateOf(true)
    var addressBarSecureBottom by mutableStateOf(true)

    var searchOverlayOpen by mutableStateOf(false)
    var searchOverlayIsBottom by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var suggestions by mutableStateOf<List<SuggestionItem>>(emptyList())


    var pageLoadProgress by mutableIntStateOf(0)
    var isPageLoading by mutableStateOf(false)

    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isBookmarked by mutableStateOf(false)
    var hasActiveUrl by mutableStateOf(false)
    var isHomePage by mutableStateOf(false)
    var isDesktopMode by mutableStateOf(false)
    var homepageDesign by mutableStateOf("plain")
    var homepageImageVersion by mutableLongStateOf(0L)
    var homepageShowFavorites by mutableStateOf(true)
    var homepageShowQuickActions by mutableStateOf(true)
    var homepageShowRecent by mutableStateOf(true)
    var homepageCenterContent by mutableStateOf(true)
    var homepageImageFullBrightness by mutableStateOf(false)

    var bookmarkFolderDialogOpen by mutableStateOf(false)
    var bookmarkFolderTree by mutableStateOf<List<com.simplefox.browser.bookmarks.FolderTreeEntry>>(emptyList())
    var pendingBookmarkUrl by mutableStateOf("")
    var pendingBookmarkTitle by mutableStateOf("")

    var tabCountText by mutableStateOf("1")
    var isIncognito by mutableStateOf(false)
    var isMediaCaptureEnabled by mutableStateOf(true)
    var showHomeButton by mutableStateOf(true)
    var activeTabId by mutableStateOf<String?>(null)

    var topBarFraction by mutableFloatStateOf(0f)
    var bottomBarFraction by mutableFloatStateOf(0f)

    var topBarAnimating: Boolean = false
    var bottomBarAnimating: Boolean = false
    var barsTargetHidden: Boolean = false

    var topBarFullHeightPx by mutableIntStateOf(0)
    var bottomBarFullHeightPx by mutableIntStateOf(0)
    var statusBarInsetPx by mutableIntStateOf(0)
    var cachedStatusBarInsetPx by mutableIntStateOf(0)
    var navBarInsetPx by mutableIntStateOf(0)
    var systemNavInsetPx by mutableIntStateOf(0)
    var hideStatusBar by mutableStateOf(false)
    var hideSystemNavigation by mutableStateOf(false)

    var contentPaddingTopPx by mutableIntStateOf(0)
    var contentPaddingBottomPx by mutableIntStateOf(0)
    var imeInsetPx by mutableIntStateOf(0)

    var isFullscreen by mutableStateOf(false)
    var isShortcutFrameless by mutableStateOf(false)

    var imageLongPressRequest by mutableStateOf<com.simplefox.browser.browser.sheets.ImageLongPressRequest?>(null)
    var linkLongPressRequest by mutableStateOf<com.simplefox.browser.browser.sheets.LinkLongPressRequest?>(null)
    var contentPreviewRequest by mutableStateOf<com.simplefox.browser.browser.sheets.ContentPreviewRequest?>(null)
    var selectPickerRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.SelectPickerRequest?>(null)
    var jsDialogRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.JsDialogRequest?>(null)
    var dateTimePickerRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.DateTimePickerRequest?>(null)
    var colorPickerRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.ColorPickerRequest?>(null)
    var httpAuthRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.HttpAuthRequest?>(null)
    var sslWarningRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.SslWarningRequest?>(null)
    var httpsOnlyRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.HttpsOnlyRequest?>(null)

    var confirmDialogConfig by mutableStateOf<com.simplefox.browser.ui.listscreen.ConfirmDialogConfig?>(null)
    var conflictDialogRequest by mutableStateOf<com.simplefox.browser.downloads.DownloadConflictDialogRequest?>(null)
    var webPermissionDialogRequest by mutableStateOf<com.simplefox.browser.ui.WebPermissionDialogRequest?>(null)
    var popupAlertRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.PopupAlertRequest?>(null)
    var refreshLinkDialogRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.RefreshLinkDialogRequest?>(null)
    var cookiesControlRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.CookiesControlRequest?>(null)
    var siteInfoRequest by mutableStateOf<com.simplefox.browser.browser.siteinfo.SiteInfoRequest?>(null)
    var cookiesControlExportRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.CookiesControlExportRequest?>(null)
    var cookiesControlImportRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.CookiesControlImportRequest?>(null)
    var translateBarRequest by mutableStateOf<com.simplefox.browser.translation.TranslateBarRequest?>(null)
    var showTranslateButton by mutableStateOf(true)
    var profilePickerOpen by mutableStateOf(false)
    var tabGroupBar by mutableStateOf<com.simplefox.browser.tabs.TabGroupBarState?>(null)
    var tabMenuFocusGroupId by mutableStateOf<String?>(null)
    var tabsRevision by mutableIntStateOf(0)
    var tabStrip by mutableStateOf<com.simplefox.browser.tabs.TabStripState?>(null)
    var tabStripNewGroupTabId by mutableStateOf<String?>(null)
    var tabStripPosition by mutableStateOf(com.simplefox.browser.tabs.TabBarPlacement.FOLLOW)
    var tabGroupBarPosition by mutableStateOf(com.simplefox.browser.tabs.TabBarPlacement.FOLLOW)
    var createShortcutRequest by mutableStateOf<com.simplefox.browser.browser.dialogs.CreateShortcutRequest?>(null)
    var openInAppRequest by mutableStateOf<com.simplefox.browser.browser.webview.OpenInAppRequest?>(null)
    var websiteBlockedRequest by mutableStateOf<com.simplefox.browser.blocker.blockedpage.WebsiteBlockedRequest?>(null)
}
