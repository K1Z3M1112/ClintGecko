package com.simplefox.browser.downloads

import android.content.Intent
import android.net.Uri
import com.simplefox.browser.util.DocumentFile
import com.simplefox.browser.util.PreferenceManager
import com.simplefox.browser.R
import com.simplefox.browser.browser.delegates.PREF_BATTERY_OPT_ASKED
import com.simplefox.browser.settings.downloads.DownloadSettingsKeys
import java.io.File

internal fun resolveFilename(url: String, contentDisposition: String, contentType: String): String =
    DownloadFilenameResolver.resolve(url, contentDisposition, contentType)

internal fun DownloadsActivity.performManualDownload(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    if (submission.startPaused) {
        checkConflictAndEnqueueManual(submission, userAgent, onDismiss, onRename)
        return
    }
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val pm = getSystemService(android.os.PowerManager::class.java)
    if (!prefs.getBoolean(PREF_BATTERY_OPT_ASKED, false) && !pm.isIgnoringBatteryOptimizations(packageName)) {
        prefs.edit().putBoolean(PREF_BATTERY_OPT_ASKED, true).apply()
        uiState.confirmDialogConfig = com.simplefox.browser.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.battery_opt_rationale_title),
            message = getString(R.string.battery_opt_rationale_message),
            cancelable = false,
            positiveLabel = getString(R.string.action_allow),
            onPositive = {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                continueManualDownload(submission, userAgent, onDismiss, onRename)
            },
            negativeLabel = getString(R.string.action_not_now),
            onNegative = {
                continueManualDownload(submission, userAgent, onDismiss, onRename)
            }
        )
        return
    }
    continueManualDownload(submission, userAgent, onDismiss, onRename)
}

internal fun DownloadsActivity.confirmMeteredWarningThenProceed(
    unmeteredOnly: Boolean,
    onDecided: (effectiveUnmeteredOnly: Boolean) -> Unit
) {
    val cm = getSystemService(android.net.ConnectivityManager::class.java)
    val isMetered = cm?.isActiveNetworkMetered ?: false
    if (unmeteredOnly && isMetered) {
        uiState.confirmDialogConfig = com.simplefox.browser.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_metered_warning_title),
            message = getString(R.string.download_metered_warning_message),
            positiveLabel = getString(R.string.action_yes),
            onPositive = { onDecided(false) },
            negativeLabel = getString(R.string.action_no),
            onNegative = { onDecided(true) },
            neutralLabel = getString(R.string.action_cancel)
        )
        return
    }
    onDecided(unmeteredOnly)
}

private fun DownloadsActivity.continueManualDownload(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    confirmMeteredWarningThenProceed(submission.unmeteredOnly) { effectiveUnmeteredOnly ->
        checkConflictAndEnqueueManual(submission.copy(unmeteredOnly = effectiveUnmeteredOnly), userAgent, onDismiss, onRename)
    }
}

private fun DownloadsActivity.checkConflictAndEnqueueManual(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val existing = ClintDownloadManager.findActiveDownloadForUrl(submission.url)
    if (existing != null) {
        uiState.confirmDialogConfig = com.simplefox.browser.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_already_active_title),
            message = getString(R.string.download_already_active_message, existing.filename),
            positiveLabel = getString(R.string.action_download_anyway),
            onPositive = {
                checkFilenameConflictAndEnqueueManual(submission, userAgent, onDismiss, onRename)
            },
            negativeLabel = getString(R.string.action_cancel)
        )
        return
    }
    checkFilenameConflictAndEnqueueManual(submission, userAgent, onDismiss, onRename)
}

private fun DownloadsActivity.enqueueManualDownload(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit
) {
    if (DownloadFileHelper.isCustomLocationAccessible(this, submission.locationMode, submission.customLocationUri)) onDismiss()
    ClintDownloadManager.enqueue(
        this, submission.url, submission.filename, userAgent, "", "",
        submission.retryEnabled, submission.unmeteredOnly, submission.splitParts, submission.multithreadingParts,
        submission.speedLimitBytesPerSec, submission.locationMode, submission.customLocationUri, submission.scheduledStartAtMillis,
        submission.categorizeEnabled, startPaused = submission.startPaused
    )
}

private fun DownloadsActivity.checkFilenameConflictAndEnqueueManual(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val isSaf = submission.locationMode == DownloadSettingsKeys.MODE_CUSTOM
    val fileExists = if (isSaf) {
        val treeUri = submission.customLocationUri?.let { Uri.parse(it) } ?: DownloadFileHelper.getSafTreeUri(this)
        val docDir = treeUri?.let { DocumentFile.fromTreeUri(this, it) }
        val targetDir = docDir?.let { DownloadCategories.findExistingSafDir(submission.categorizeEnabled, it, submission.filename) }
        targetDir?.findFile(submission.filename) != null
    } else {
        val targetDir = DownloadCategories.resolveDir(submission.categorizeEnabled, DownloadFileHelper.resolveDownloadDir(), submission.filename)
        File(targetDir, submission.filename).exists()
    }
    if (!fileExists) {
        enqueueManualDownload(submission, userAgent, onDismiss)
        return
    }
    uiState.conflictDialogRequest = resolveDownloadConflict(this, !hasUnfinishedDownloadNamed(submission.filename), DownloadConflictDialogRequest(
        onAddDuplicate = {
            enqueueManualDownload(submission, userAgent, onDismiss)
        },
        onOverride = {
            deleteExistingManual(submission.filename, submission.locationMode, submission.customLocationUri, submission.categorizeEnabled)
            enqueueManualDownload(submission, userAgent, onDismiss)
        },
        onRename = onRename
    ))
}

private fun DownloadsActivity.deleteExistingManual(
    filename: String,
    locationMode: String,
    customLocationUri: String?,
    categorizeEnabled: Boolean
) {
    val matchingIds = ClintDownloadManager.downloadsFlow.value.filter { it.filename == filename }.map { it.id }
    matchingIds.forEach { ClintDownloadManager.remove(this, it, deleteFile = true) }
    val isSaf = locationMode == DownloadSettingsKeys.MODE_CUSTOM
    if (isSaf) {
        val treeUri = customLocationUri?.let { Uri.parse(it) } ?: DownloadFileHelper.getSafTreeUri(this)
        val docDir = treeUri?.let { DocumentFile.fromTreeUri(this, it) }
        val targetDir = docDir?.let { DownloadCategories.findExistingSafDir(categorizeEnabled, it, filename) }
        targetDir?.findFile(filename)?.delete()
    } else {
        val targetDir = DownloadCategories.resolveDir(categorizeEnabled, DownloadFileHelper.resolveDownloadDir(), filename)
        File(targetDir, filename).delete()
    }
}
