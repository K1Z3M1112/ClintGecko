package com.simplefox.browser.settings.lookandfeel

import androidx.compose.foundation.layout.padding
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.simplefox.browser.ui.icons.Icons
import com.simplefox.browser.ui.foundation.Icon
import com.simplefox.browser.ui.foundation.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.simplefox.browser.ui.theme.ClintColors
import com.simplefox.browser.ui.ClintSwitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.R
import com.simplefox.browser.settings.common.RowDivider
import com.simplefox.browser.settings.common.SettingsRow
import com.simplefox.browser.settings.common.SettingsScreenScaffold
import com.simplefox.browser.settings.common.SettingsSection
import com.simplefox.browser.settings.common.SettingsSectionLabel
import com.simplefox.browser.ui.theme.LocalClintColors

@Composable
fun LookAndFeelScreen(
    state: LookAndFeelUiState,
    onThemeSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onAddressBarPositionSelected: (String) -> Unit,
    onScrollHideModeSelected: (String) -> Unit,
    onHideStatusBarRowClicked: () -> Unit,
    onHideSystemNavigationRowClicked: () -> Unit,
    onShowHomeButtonRowClicked: () -> Unit,
    onShowTabStripRowClicked: () -> Unit,
    onTabStripPositionSelected: (String) -> Unit,
    onTabGroupsRowClicked: () -> Unit,
    onTabGroupBarPositionSelected: (String) -> Unit,
    onTabPreviewResolutionSelected: (String) -> Unit,
    onCustomizeMenuRowClicked: () -> Unit,
    onExitConfirmationConfirmed: (String) -> Unit,
    onPopupAlertStyleSelected: (String) -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold(
        overlay = {
            when (state.openDialog) {
                LookAndFeelDialog.THEME -> ThemeSelectorDialog(
                    current = state.theme, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onThemeSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.ADDRESS_BAR_POSITION -> AddressBarPositionDialog(
                    current = state.addressBarPosition, theme = state.theme, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onAddressBarPositionSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.SCROLL_HIDE_MODE -> ScrollHideModeDialog(
                    current = state.scrollHideMode, addressBarPosition = state.addressBarPosition, theme = state.theme,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation, onSelect = onScrollHideModeSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.EXIT_CONFIRMATION -> ExitConfirmationDialog(
                    current = state.exitConfirmation, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onExitConfirmationConfirmed, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.TAB_STRIP_POSITION -> TabBarPositionDialog(
                    title = stringResource(R.string.pref_tab_strip_position_title),
                    current = state.tabStripPosition, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onTabStripPositionSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.TAB_GROUP_BAR_POSITION -> TabBarPositionDialog(
                    title = stringResource(R.string.pref_tab_group_bar_position_title),
                    current = state.tabGroupBarPosition, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onTabGroupBarPositionSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.TAB_PREVIEW_RESOLUTION -> TabPreviewResolutionDialog(
                    current = state.tabPreviewResolution, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onTabPreviewResolutionSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.POPUP_ALERT_STYLE -> PopupAlertStyleDialog(
                    current = state.popupAlertStyle, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onPopupAlertStyleSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.LANGUAGE -> LanguageSelectorDialog(
                    current = state.language, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onLanguageSelected, onDismiss = { state.openDialog = null }
                )
                null -> {}
            }
        }
    ) {
        SettingsSectionLabel(stringResource(R.string.pref_category_language))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Translate,
                title = stringResource(R.string.pref_language_title),
                summary = languageSummaryText(state.language),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.LANGUAGE }
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_appearance))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Palette,
                title = stringResource(R.string.pref_app_theme_title),
                summary = stringResource(R.string.pref_app_theme_summary),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.THEME }
            )
        }
        DarkReaderColorHint(colors)

        SettingsSectionLabel(stringResource(R.string.pref_category_address_bar))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.WebAsset,
                title = stringResource(R.string.pref_address_bar_position_title),
                summary = stringResource(addressBarPositionSummaryRes(state.addressBarPosition)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.ADDRESS_BAR_POSITION }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.SwapVert,
                title = stringResource(R.string.pref_nested_scroll_title),
                summary = stringResource(nestedScrollSummaryRes(state.scrollHideMode)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.SCROLL_HIDE_MODE }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Home,
                title = stringResource(R.string.pref_show_home_button_title),
                summary = stringResource(R.string.pref_show_home_button_summary),
                colors = colors,
                onClick = onShowHomeButtonRowClicked,
                trailing = {
                    ClintSwitch(checked = state.showHomeButton)
                }
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_tabs))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Tab,
                title = stringResource(R.string.pref_tab_strip_title),
                summary = stringResource(R.string.pref_tab_strip_summary),
                colors = colors,
                onClick = onShowTabStripRowClicked,
                trailing = {
                    ClintSwitch(checked = state.showTabStrip)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.VerticalAlignTop,
                title = stringResource(R.string.pref_tab_strip_position_title),
                summary = stringResource(tabBarPositionSummaryRes(state.tabStripPosition)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.TAB_STRIP_POSITION }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.PhotoSizeSelectLarge,
                title = stringResource(R.string.pref_tab_preview_resolution_title),
                summary = stringResource(tabPreviewResolutionSummaryRes(state.tabPreviewResolution)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.TAB_PREVIEW_RESOLUTION }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Workspaces,
                title = stringResource(R.string.pref_tab_groups_title),
                summary = stringResource(R.string.pref_tab_groups_summary),
                colors = colors,
                onClick = onTabGroupsRowClicked,
                trailing = {
                    ClintSwitch(checked = state.tabGroupsEnabled)
                }
            )
        }

        SettingsSectionLabel(stringResource(R.string.menu))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Reorder,
                title = stringResource(R.string.menu_customization_title),
                summary = stringResource(R.string.menu_customization_settings_summary),
                colors = colors,
                onClick = onCustomizeMenuRowClicked
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_fullscreen))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Fullscreen,
                title = stringResource(R.string.hide_status_bar),
                summary = stringResource(R.string.hide_status_bar_summary),
                colors = colors,
                onClick = onHideStatusBarRowClicked,
                trailing = {
                    ClintSwitch(checked = state.hideStatusBar)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Fullscreen,
                title = stringResource(R.string.hide_system_navigation),
                summary = stringResource(R.string.hide_system_navigation_summary),
                colors = colors,
                onClick = onHideSystemNavigationRowClicked,
                trailing = {
                    ClintSwitch(checked = state.hideSystemNavigation)
                }
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_navigation))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Close,
                title = stringResource(R.string.exit_confirmation_title),
                summary = stringResource(exitConfirmationSummaryRes(state.exitConfirmation)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.EXIT_CONFIRMATION }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = com.simplefox.browser.ui.icons.Icons.Filled.Layers,
                title = stringResource(R.string.pref_popup_alert_style_title),
                summary = stringResource(popupAlertStyleSummaryRes(state.popupAlertStyle)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.POPUP_ALERT_STYLE }
            )
        }
    }
}

private fun nestedScrollSummaryRes(mode: String): Int = when (mode) {
    "off" -> R.string.nested_scroll_off
    "navigation_bar" -> R.string.nested_scroll_nav_bar
    "both" -> R.string.nested_scroll_both
    else -> R.string.nested_scroll_search_bar
}

private fun tabBarPositionSummaryRes(position: String): Int = when (position) {
    com.simplefox.browser.tabs.TabBarPlacement.TOP -> R.string.address_bar_position_top
    com.simplefox.browser.tabs.TabBarPlacement.BOTTOM -> R.string.address_bar_position_bottom
    else -> R.string.tab_bar_position_follow
}

private fun addressBarPositionSummaryRes(position: String): Int = when (position) {
    "top" -> R.string.address_bar_position_top
    "bottom" -> R.string.address_bar_position_bottom
    else -> R.string.address_bar_position_split
}

private fun exitConfirmationSummaryRes(value: String): Int = when (value) {
    "off" -> R.string.exit_confirmation_off
    "dialog" -> R.string.exit_confirmation_dialog
    else -> R.string.exit_confirmation_toast
}

private fun popupAlertStyleSummaryRes(value: String): Int =
    if (value == "snackbar") R.string.popup_alert_style_snackbar else R.string.popup_alert_style_dialog

@Composable
fun languageSummaryText(language: String): String {
    val option = languageOptions.firstOrNull { it.tag == language }
    return stringResource(option?.nameRes ?: R.string.language_system)
}

private fun Color.toHex(): String = String.format(java.util.Locale.ROOT, "#%06X", toArgb() and 0xFFFFFF)

@Composable
private fun DarkReaderColorHint(colors: ClintColors) {
    val context = LocalContext.current
    val copiedText = stringResource(R.string.download_props_copied)
    val entries = listOf(
        stringResource(R.string.pref_dark_reader_label_background) to colors.background,
        stringResource(R.string.pref_dark_reader_label_text) to colors.onSurface,
        stringResource(R.string.pref_dark_reader_label_scrollbar) to colors.primary,
        stringResource(R.string.pref_dark_reader_label_selection) to colors.primary
    )
    Text(
        text = stringResource(R.string.pref_dark_reader_theme_hint),
        color = colors.secondaryText,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 6.dp)
    )
    SettingsSection(colors.cardBackground, bottomSpacing = 10.dp) {
        entries.forEachIndexed { index, (label, color) ->
            if (index > 0) RowDivider(colors.divider)
            val hex = color.toHex()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText(label, hex))
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, colors.divider, CircleShape)
                )
                Text(
                    text = label,
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f).padding(start = 14.dp)
                )
                Text(
                    text = hex,
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.padding(start = 12.dp).size(18.dp)
                )
            }
        }
    }
    Text(
        text = stringResource(R.string.pref_dark_reader_detect_hint),
        color = colors.secondaryText,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 24.dp)
    )
}
