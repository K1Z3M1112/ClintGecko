package com.simplefox.browser.downloads
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert

import com.simplefox.browser.R

import com.simplefox.browser.ui.foundation.DropdownMenu
import com.simplefox.browser.ui.foundation.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.simplefox.browser.ui.listscreen.ListMenuItem
import com.simplefox.browser.ui.listscreen.ListSortOrder
import com.simplefox.browser.ui.listscreen.PopupShape
import com.simplefox.browser.ui.theme.LocalClintColors

@Composable
fun DownloadsSortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    sortKey: DownloadSortKey,
    sortOrder: ListSortOrder,
    onSortByName: () -> Unit,
    onSortByDate: () -> Unit,
    onSortBySize: () -> Unit,
    onSortByStatus: () -> Unit,
    onSortAscending: () -> Unit,
    onSortDescending: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = null
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Abc, stringResource(R.string.downloads_sort_by_name), sortKey == DownloadSortKey.NAME) {
            onDismiss(); onSortByName()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.DateRange, stringResource(R.string.downloads_sort_by_date), sortKey == DownloadSortKey.DATE) {
            onDismiss(); onSortByDate()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.FormatSize, stringResource(R.string.downloads_sort_by_size), sortKey == DownloadSortKey.SIZE) {
            onDismiss(); onSortBySize()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.SwapVert, stringResource(R.string.downloads_sort_by_status), sortKey == DownloadSortKey.STATUS) {
            onDismiss(); onSortByStatus()
        }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ArrowUpward, stringResource(R.string.history_sort_ascending), sortOrder == ListSortOrder.ASCENDING) {
            onDismiss(); onSortAscending()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ArrowDownward, stringResource(R.string.history_sort_descending), sortOrder == ListSortOrder.DESCENDING) {
            onDismiss(); onSortDescending()
        }
    }
}

@Composable
fun DownloadsOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onResumeAll: () -> Unit,
    onPauseAll: () -> Unit,
    onSettings: () -> Unit,
    onDownloadLog: (() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = null
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.PlayArrow, stringResource(R.string.downloads_resume_all), false) { onDismiss(); onResumeAll() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Pause, stringResource(R.string.downloads_pause_all), false) { onDismiss(); onPauseAll() }
        HorizontalDivider(color = colors.divider)
        if (onDownloadLog != null) {
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.BugReport, stringResource(R.string.download_log_title), false) { onDismiss(); onDownloadLog() }
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Settings, stringResource(R.string.download_settings_title), false) { onDismiss(); onSettings() }
    }
}

data class DownloadItemActions(
    val onOpen: (DownloadItem) -> Unit,
    val onShare: (DownloadItem) -> Unit,
    val onOpenFolder: (DownloadItem) -> Unit,
    val onRename: (DownloadItem) -> Unit,
    val onRedownload: (DownloadItem) -> Unit,
    val onRedownloadOptions: (DownloadItem) -> Unit,
    val onChangeSettings: (DownloadItem) -> Unit,
    val onUpdateLink: (DownloadItem) -> Unit,
    val onUpdateLinkInBrowser: (DownloadItem) -> Unit,
    val onRemove: (DownloadItem) -> Unit,
    val onCopyLink: (DownloadItem) -> Unit,
    val onCopyFilename: (DownloadItem) -> Unit,
    val onCopyPath: (DownloadItem) -> Unit,
    val onProperties: (DownloadItem) -> Unit
)

