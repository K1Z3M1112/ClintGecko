package com.simplefox.browser.browser.delegates

import android.content.Intent
import android.net.Uri
import androidx.preference.PreferenceManager
import com.simplefox.browser.R
import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.browser.webview.ClintWebView
import com.simplefox.browser.downloads.ClintDownloadManager
import com.simplefox.browser.downloads.DEFAULT_SPEED_LIMIT_UNIT
import com.simplefox.browser.downloads.DownloadCategories
import com.simplefox.browser.downloads.DownloadFileHelper
import com.simplefox.browser.downloads.DownloadsActivity
import com.simplefox.browser.downloads.MegaDownloadEntry
import com.simplefox.browser.downloads.mega.MegaDownloadDialog
import com.simplefox.browser.downloads.mega.MegaFileInfo
import com.simplefox.browser.downloads.mega.MegaFolderSubmission
import com.simplefox.browser.downloads.mega.MegaLink
import com.simplefox.browser.downloads.mega.MegaSource
import com.simplefox.browser.downloads.resolveSpeedLimitBytesPerSec
import com.simplefox.browser.settings.downloads.DownloadSettingsKeys
import com.simplefox.browser.ui.showClintSnackbar
import com.simplefox.browser.ui.theme.ClintComposeTheme
import java.util.WeakHashMap

private val offeredMegaLinks = WeakHashMap<ClintWebView, String>()

internal fun MainActivity.maybeOfferMegaDownload(webView: ClintWebView, url: String) {
    val link = MegaLink.parse(url)
    if (link == null) {
        offeredMegaLinks.remove(webView)
        return
    }
    if (tabManager.activeTab?.webView !== webView) return
    if (offeredMegaLinks[webView] == link.identity) return
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    if (!prefs.getBoolean(DownloadSettingsKeys.PREF_DETECT_MEGA_LINKS, DownloadSettingsKeys.DEFAULT_DETECT_MEGA_LINKS)) return
    val manager = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_MANAGER, DownloadSettingsKeys.DEFAULT_DOWNLOAD_MANAGER)
    if (manager != null && manager != DownloadSettingsKeys.DEFAULT_DOWNLOAD_MANAGER) return
    if (overlayContent != null) return
    offeredMegaLinks[webView] = link.identity
    showMegaDownload(link, webView.settings.userAgentString, allowQuick = false)
}

internal fun MainActivity.showMegaDownload(link: MegaLink, userAgent: String, allowQuick: Boolean = true) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val theme = prefs.getString("app_theme", "system") ?: "system"
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
    val locationMode = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT
    val customUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) }
    val unmeteredOnly = prefs.getBoolean(DownloadSettingsKeys.PREF_UNMETERED_ONLY, DownloadSettingsKeys.DEFAULT_UNMETERED_ONLY)

    overlayContent = {
        ClintComposeTheme(theme = theme) {
            MegaDownloadDialog(
                hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                link = link,
                userAgent = userAgent,
                initialLocationMode = locationMode,
                initialCustomUri = customUri,
                initialUnmeteredOnly = unmeteredOnly,
                onLaunchFolderPicker = { onPicked ->
                    downloadDialogFolderPickerCallback = { uri ->
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        onPicked(uri)
                    }
                    launchDownloadDialogFolderPicker()
                },
                onFileResolved = { info ->
                    if (link is MegaLink.File) showMegaFileDialog(link, info, userAgent, allowQuick)
                },
                onDownloadFolder = { submission ->
                    overlayContent = null
                    startMegaFolderDownload(submission, userAgent)
                },
                onDismiss = { overlayContent = null }
            )
        }
    }
}

private fun MainActivity.showMegaFileDialog(link: MegaLink.File, info: MegaFileInfo, userAgent: String, allowQuick: Boolean) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    mountDownloadRequestDialog(
        allowQuick = allowQuick,
        url = link.url,
        onCopyLink = { copyDownloadRequestLink(link.url) },
        initialFilename = info.name,
        contentLengthBytes = info.size,
        checkStorage = true,
        showOptions = true,
        initialLocationMode = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT,
        initialCustomUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) },
        initialRetryEnabled = prefs.getBoolean(DownloadSettingsKeys.PREF_RETRY_ENABLED, DownloadSettingsKeys.DEFAULT_RETRY_ENABLED),
        initialUnmeteredOnly = prefs.getBoolean(DownloadSettingsKeys.PREF_UNMETERED_ONLY, DownloadSettingsKeys.DEFAULT_UNMETERED_ONLY),
        initialSplitParts = prefs.getInt(DownloadSettingsKeys.PREF_SPLIT_PARTS, DownloadSettingsKeys.DEFAULT_SPLIT_PARTS),
        initialMultithreadingParts = prefs.getInt(DownloadSettingsKeys.PREF_MULTITHREADING_PARTS, DownloadSettingsKeys.DEFAULT_MULTITHREADING_PARTS),
        initialSpeedLimitAmount = prefs.getInt(DownloadSettingsKeys.PREF_SPEED_LIMIT_AMOUNT, DownloadSettingsKeys.DEFAULT_SPEED_LIMIT_AMOUNT),
        initialSpeedLimitUnit = prefs.getString(DownloadSettingsKeys.PREF_SPEED_LIMIT_UNIT, DEFAULT_SPEED_LIMIT_UNIT) ?: DEFAULT_SPEED_LIMIT_UNIT,
        onSubmit = { submission, dismiss, _ ->
            dismiss()
            ClintDownloadManager.enqueueMega(
                this,
                listOf(MegaDownloadEntry(MegaSource(link.url, link.handle, link.key), submission.filename, info.size, info.downloadUrl)),
                userAgent,
                submission.retryEnabled, submission.unmeteredOnly, submission.splitParts, submission.multithreadingParts,
                submission.speedLimitBytesPerSec, submission.locationMode, submission.customLocationUri,
                submission.scheduledStartAtMillis, submission.categorizeEnabled,
                startPaused = submission.startPaused
            )
            if (submission.startPaused) showDownloadAddedSnackbar(submission.filename)
            else showDownloadStartedSnackbar(submission.filename, submission.locationMode, submission.customLocationUri)
        }
    )
}

private fun MainActivity.startMegaFolderDownload(submission: MegaFolderSubmission, userAgent: String) {
    if (submission.entries.isEmpty()) {
        if (submission.skipped > 0) showClintSnackbar(message = getString(R.string.mega_all_skipped))
        return
    }
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    ClintDownloadManager.enqueueMega(
        this, submission.entries, userAgent,
        retryEnabled = prefs.getBoolean(DownloadSettingsKeys.PREF_RETRY_ENABLED, DownloadSettingsKeys.DEFAULT_RETRY_ENABLED),
        unmeteredOnly = submission.unmeteredOnly,
        splitParts = prefs.getInt(DownloadSettingsKeys.PREF_SPLIT_PARTS, DownloadSettingsKeys.DEFAULT_SPLIT_PARTS),
        multithreadingParts = prefs.getInt(DownloadSettingsKeys.PREF_MULTITHREADING_PARTS, DownloadSettingsKeys.DEFAULT_MULTITHREADING_PARTS),
        speedLimitBytesPerSec = resolveSpeedLimitBytesPerSec(
            this,
            prefs.getInt(DownloadSettingsKeys.PREF_SPEED_LIMIT_AMOUNT, DownloadSettingsKeys.DEFAULT_SPEED_LIMIT_AMOUNT),
            prefs.getString(DownloadSettingsKeys.PREF_SPEED_LIMIT_UNIT, DEFAULT_SPEED_LIMIT_UNIT) ?: DEFAULT_SPEED_LIMIT_UNIT
        ),
        locationMode = submission.locationMode,
        customLocationUri = submission.customLocationUri,
        categorizeEnabled = DownloadCategories.isEnabled(this),
        startPaused = submission.startPaused
    )
    if (DownloadFileHelper.isCustomLocationAccessible(this, submission.locationMode, submission.customLocationUri)) {
        val count = submission.entries.size
        showClintSnackbar(
            message = resources.getQuantityString(
                if (submission.startPaused) R.plurals.mega_folder_added_paused else R.plurals.mega_folder_started, count, count
            ),
            actionLabel = getString(R.string.download_started_view_action),
            onAction = { DownloadsActivity.open(this) }
        )
    }
}
