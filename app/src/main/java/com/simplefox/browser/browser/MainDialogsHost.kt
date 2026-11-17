package com.simplefox.browser.browser

import com.simplefox.browser.browser.delegates.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.simplefox.browser.R

@Composable
internal fun MainDialogsHost(
    activity: MainActivity,
    state: MainUiState,
    effectiveStatusBarPx: Int,
    effectiveNavBarPx: Int
) {
    state.imageLongPressRequest?.let { req ->
        com.simplefox.browser.browser.sheets.ImageLongPressSheet(request = req, activity = activity, onDismiss = { state.imageLongPressRequest = null })
    }
    state.linkLongPressRequest?.let { req ->
        com.simplefox.browser.browser.sheets.LinkLongPressSheet(request = req, activity = activity, onDismiss = { state.linkLongPressRequest = null })
    }
    state.contentPreviewRequest?.let { req ->
        com.simplefox.browser.browser.sheets.ContentPreviewSheet(request = req, activity = activity, onDismiss = { state.contentPreviewRequest = null })
    }

    val hideStatusBarPref = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_status_bar", false) }
    val hideSystemNavigationPref = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_system_navigation", false) }
    com.simplefox.browser.ui.listscreen.ConfirmDialogHost(state.confirmDialogConfig, hideStatusBarPref, hideSystemNavigationPref) { state.confirmDialogConfig = null }
    state.conflictDialogRequest?.let { req ->
        com.simplefox.browser.downloads.DownloadConflictDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.conflictDialogRequest = null }
    }
    state.webPermissionDialogRequest?.let { req ->
        com.simplefox.browser.ui.WebPermissionDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.webPermissionDialogRequest = null }
    }
    state.selectPickerRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.SelectPickerDialog(req, hideStatusBarPref, hideSystemNavigationPref) {
            req.onCancel?.invoke()
            if (state.selectPickerRequest === req) state.selectPickerRequest = null
        }
    }
    state.dateTimePickerRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.DateTimePickerDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.dateTimePickerRequest = null }
    }
    state.colorPickerRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.ColorPickerDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.colorPickerRequest = null }
    }
    state.jsDialogRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.JsDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.jsDialogRequest = null }
    }
    state.httpAuthRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.HttpAuthDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.httpAuthRequest = null }
    }
    state.sslWarningRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.SslWarningDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.sslWarningRequest = null }
    }
    state.httpsOnlyRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.HttpsOnlyDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.httpsOnlyRequest = null }
    }
    if (!state.isFullscreen) {
        state.popupAlertRequest?.let { req ->
            com.simplefox.browser.browser.dialogs.PopupAlertDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.popupAlertRequest = null }
        }
    }
    state.refreshLinkDialogRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.RefreshLinkDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.refreshLinkDialogRequest = null }
    }
    state.siteInfoRequest?.let { req ->
        com.simplefox.browser.browser.siteinfo.SiteInfoSheet(
            request = req,
            hideStatusBar = hideStatusBarPref,
            hideSystemNavigation = hideSystemNavigationPref,
            onPermissionChange = { type, newState -> activity.changeSiteInfoPermission(req.host, type, newState) },
            onResetPermissions = { activity.resetSiteInfoPermissions(req.host) },
            onTrackingProtectionChange = { enabled -> activity.setSiteInfoTrackingProtection(req.host, enabled) },
            onCookiesClick = { activity.openSiteInfoCookies() },
            onSiteSettingsClick = { activity.openSiteInfoSiteSettings() },
            onDismiss = { state.siteInfoRequest = null }
        )
    }
    state.cookiesControlRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.CookiesControlDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.cookiesControlRequest = null }
    }
    state.cookiesControlExportRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.CookiesControlExportDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.cookiesControlExportRequest = null }
    }
    state.cookiesControlImportRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.CookiesControlImportDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.cookiesControlImportRequest = null }
    }
    if (state.profilePickerOpen) {
        com.simplefox.browser.profiles.ProfilePickerDialog(
            profiles = remember { com.simplefox.browser.profiles.ProfileRepository.all(activity) },
            hideStatusBar = hideStatusBarPref,
            hideSystemNavigation = hideSystemNavigationPref,
            onPick = { id -> state.profilePickerOpen = false; activity.onNewTabInProfile(id) },
            onManage = {
                state.profilePickerOpen = false
                activity.startActivity(android.content.Intent(activity, com.simplefox.browser.profiles.ProfilesActivity::class.java))
            },
            onDismiss = { state.profilePickerOpen = false }
        )
    }
    state.tabStripNewGroupTabId?.let { tabId ->
        com.simplefox.browser.tabs.TabGroupEditorDialog(
            title = stringResource(R.string.tab_group_new),
            positiveLabel = stringResource(R.string.tab_group_create),
            initialName = "",
            initialColor = remember(tabId) { com.simplefox.browser.tabs.TabGroupColor.next(activity.tabManager.groups.map { it.color }) },
            hideStatusBar = hideStatusBarPref,
            hideSystemNavigation = hideSystemNavigationPref,
            onDismiss = { state.tabStripNewGroupTabId = null },
            onConfirm = { name, color ->
                state.tabStripNewGroupTabId = null
                activity.addTabToNewGroup(tabId, name, color)
            }
        )
    }
    state.createShortcutRequest?.let { req ->
        com.simplefox.browser.browser.dialogs.CreateShortcutDialog(req, activity, hideStatusBarPref, hideSystemNavigationPref) { state.createShortcutRequest = null }
    }
    state.openInAppRequest?.let { req ->
        com.simplefox.browser.browser.webview.OpenInAppDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.openInAppRequest = null }
    }
    if (state.bookmarkFolderDialogOpen) {
        com.simplefox.browser.bookmarks.MoveToFolderDialog(
            tree = state.bookmarkFolderTree,
            hideStatusBar = hideStatusBarPref, hideSystemNavigation = hideSystemNavigationPref,
            onDismiss = { activity.dismissBookmarkFolderDialog() },
            onSelect = { folderId -> activity.confirmPendingBookmark(folderId) },
            title = stringResource(R.string.bookmarks_save_to_title)
        )
    }
    state.websiteBlockedRequest?.let { req ->
        com.simplefox.browser.blocker.blockedpage.WebsiteBlockedOverlay(
            request = req,
            statusBarPaddingPx = effectiveStatusBarPx,
            navBarPaddingPx = effectiveNavBarPx,
            onReturnToPrevious = { activity.dismissWebsiteBlockedOverlay() }
        )
    }
}
