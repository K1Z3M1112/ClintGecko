package com.simplefox.browser.ui.status

import com.simplefox.browser.ui.icons.Icons
import com.simplefox.browser.ui.foundation.DropdownMenu
import com.simplefox.browser.ui.foundation.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.simplefox.browser.R
import com.simplefox.browser.ui.listscreen.ListMenuItem
import com.simplefox.browser.ui.listscreen.PopupShape
import com.simplefox.browser.ui.theme.LocalClintColors

fun formatElapsedSeconds(totalSeconds: Long): String {
    val m = totalSeconds / 60L
    val s = totalSeconds % 60L
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

fun formatCompactCount(count: Long): String = when {
    count >= 1_000_000L -> "${count / 1_000_000L}M"
    count >= 1_000L -> "${count / 1_000L}k"
    else -> count.toString()
}

@Composable
fun FilterListItemOptionsMenu(
    expanded: Boolean,
    isLocal: Boolean,
    onDismiss: () -> Unit,
    onCheckUpdate: () -> Unit,
    onForceUpdate: () -> Unit,
    onRemove: () -> Unit,
    onCopyName: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = null
    ) {
        if (!isLocal) {
            ListMenuItem(Icons.Filled.Update, stringResource(R.string.filter_list_menu_check_update), false) { onDismiss(); onCheckUpdate() }
            ListMenuItem(Icons.Filled.RestartAlt, stringResource(R.string.filter_list_menu_force_update), false) { onDismiss(); onForceUpdate() }
            HorizontalDivider(color = colors.divider)
        }
        ListMenuItem(Icons.Filled.Delete, stringResource(R.string.filter_list_menu_remove), false) { onDismiss(); onRemove() }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(Icons.Filled.ContentCopy, stringResource(R.string.filter_list_menu_copy_name), false) { onDismiss(); onCopyName() }
        if (!isLocal) {
            ListMenuItem(Icons.Filled.ContentCopy, stringResource(R.string.filter_list_menu_copy_link), false) { onDismiss(); onCopyLink() }
            ListMenuItem(Icons.Filled.Share, stringResource(R.string.filter_list_menu_share_link), false) { onDismiss(); onShareLink() }
        }
    }
}
