package com.simplefox.browser.browser.delegates

import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.simplefox.browser.R
import com.simplefox.browser.backup.AuthGateResult
import com.simplefox.browser.backup.BackupAuthGate
import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.browser.cookiescontrol.CookiesControlStore
import com.simplefox.browser.browser.dialogs.CookiesControlExportRequest
import com.simplefox.browser.browser.dialogs.CookiesControlImportRequest
import com.simplefox.browser.browser.dialogs.CookiesControlRequest
import com.simplefox.browser.browser.webview.ClintWebView
import com.simplefox.browser.ui.listscreen.ConfirmDialogConfig
import com.simplefox.browser.ui.showClintSnackbar
import kotlinx.coroutines.launch

internal data class CookiesControlTarget(val webView: ClintWebView, val url: String, val host: String) {
    val storeId: String? get() = webView.cookieStoreId
}

internal fun MainActivity.openCookiesControl() {
    val tab = tabManager.activeTab ?: return
    if (tab.isIncognito) return
    val webView = tab.webView
    val url = webView.url ?: return
    if (!url.startsWith("http://") && !url.startsWith("https://")) return
    val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return
    val target = CookiesControlTarget(webView, url, host)
    lifecycleScope.launch {
        val count = CookiesControlStore.count(url, target.storeId)
        uiState.cookiesControlRequest = CookiesControlRequest(
            host = host,
            cookieCount = count,
            onDelete = { confirmDeleteCookies(target) },
            onExport = { startCookiesControlExport(target) },
            onImport = { startCookiesControlImport(target) }
        )
    }
}

private fun MainActivity.confirmDeleteCookies(target: CookiesControlTarget) {
    uiState.confirmDialogConfig = ConfirmDialogConfig(
        title = getString(R.string.cookies_control_delete_confirm_title),
        message = getString(R.string.cookies_control_delete_confirm_message, target.host),
        positiveLabel = getString(R.string.action_delete),
        onPositive = {
            lifecycleScope.launch {
                val result = CookiesControlStore.deleteAll(target.url, target.storeId)
                val message = if (result.remaining > 0) {
                    getString(R.string.cookies_control_delete_partial, result.deleted, result.remaining)
                } else {
                    getString(R.string.cookies_control_delete_done, target.host)
                }
                showClintSnackbar(message)
                target.webView.reload()
            }
        },
        negativeLabel = getString(R.string.action_cancel)
    )
}

private fun MainActivity.requireCookiesControlAuth(subtitleRes: Int, onSuccess: () -> Unit) {
    lifecycleScope.launch {
        val result = BackupAuthGate.authenticate(
            this@requireCookiesControlAuth,
            getString(R.string.backup_auth_title),
            getString(subtitleRes)
        )
        when (result) {
            is AuthGateResult.Success -> onSuccess()
            is AuthGateResult.NotAvailable -> {
                uiState.confirmDialogConfig = ConfirmDialogConfig(
                    title = getString(R.string.backup_auth_unavailable_title),
                    message = getString(R.string.cookies_control_auth_unavailable_message),
                    positiveLabel = getString(R.string.action_ok)
                )
            }
            else -> Unit
        }
    }
}

private fun MainActivity.startCookiesControlExport(target: CookiesControlTarget) {
    lifecycleScope.launch {
        if (CookiesControlStore.count(target.url, target.storeId) == 0) {
            showClintSnackbar(getString(R.string.cookies_control_export_none))
            return@launch
        }
        requireCookiesControlAuth(R.string.cookies_control_auth_subtitle_export) {
            lifecycleScope.launch {
                val content = CookiesControlStore.buildExport(target.url, target.storeId)
                if (content == null) {
                    showClintSnackbar(getString(R.string.cookies_control_export_none))
                } else {
                    uiState.cookiesControlExportRequest = CookiesControlExportRequest(
                        host = target.host,
                        cookies = content,
                        onCopied = { showClintSnackbar(getString(R.string.cookies_control_export_copied)) }
                    )
                }
            }
        }
    }
}

private fun MainActivity.startCookiesControlImport(target: CookiesControlTarget) {
    requireCookiesControlAuth(R.string.cookies_control_auth_subtitle_import) {
        uiState.cookiesControlImportRequest = CookiesControlImportRequest(
            host = target.host,
            onImport = { text -> importCookies(target, text) }
        )
    }
}

private fun MainActivity.importCookies(target: CookiesControlTarget, text: String) {
    lifecycleScope.launch {
        val result = runCatching { CookiesControlStore.import(target.url, target.storeId, text) }.getOrNull()
        when {
            result == null -> showClintSnackbar(getString(R.string.cookies_control_import_failed))
            result.imported == 0 -> showClintSnackbar(getString(R.string.cookies_control_import_none))
            else -> {
                val message = if (result.skipped > 0) {
                    getString(R.string.cookies_control_import_done_skipped, result.imported, result.skipped)
                } else {
                    getString(R.string.cookies_control_import_done, result.imported)
                }
                showClintSnackbar(message)
                target.webView.reload()
            }
        }
    }
}
