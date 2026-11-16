package com.simplefox.browser.browser.delegates

import android.widget.Toast
import com.simplefox.browser.R
import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.browser.dialogs.RefreshLinkDialogRequest
import com.simplefox.browser.downloads.ClintDownloadManager

internal fun MainActivity.showRefreshLinkDownloadDialog(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    session: MainActivity.RefreshLinkSession,
    pageUrl: String = referer
) {
    uiState.refreshLinkDialogRequest = RefreshLinkDialogRequest(
        existingFilename = session.filename,
        onUpdateExisting = {
            ClintDownloadManager.updateDownloadUrl(session.downloadId, url, referer, cookies, pageUrl)
            Toast.makeText(this, getString(R.string.refresh_link_updated_toast, session.filename), Toast.LENGTH_SHORT).show()
            refreshLinkSession = null
        },
        onAddNew = { showDownloadDialog(url, filename, userAgent, referer, cookies, pageUrl = pageUrl) }
    )
}
