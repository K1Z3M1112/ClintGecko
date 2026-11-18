package com.simplefox.browser.settings.lookandfeel

import com.simplefox.browser.ui.theme.rememberIsLightTheme
import com.simplefox.browser.ui.theme.resolveClintColors
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.simplefox.browser.ui.ClintRadioButton
import com.simplefox.browser.ui.foundation.Text
import com.simplefox.browser.ui.foundation.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.R
import com.simplefox.browser.ui.ClintDialog
import com.simplefox.browser.ui.scrollToSelection
import com.simplefox.browser.settings.common.SettingsPickerOptionBottomSpacing
import com.simplefox.browser.settings.common.SettingsPickerOptionContentPadding
import com.simplefox.browser.setup.AddressBarPreview
import com.simplefox.browser.setup.CheckSlot
import com.simplefox.browser.setup.DefaultChip
import com.simplefox.browser.setup.DrawableImage
import com.simplefox.browser.setup.ScrollHidePreview
import com.simplefox.browser.setup.SelectableCard
import com.simplefox.browser.setup.navBarSlotDescRes
import com.simplefox.browser.setup.navBarSlotTitleRes
import com.simplefox.browser.setup.scrollCardVisible
import com.simplefox.browser.ui.theme.LocalClintColors
import com.simplefox.browser.util.LocaleHelper

private val OptionContentPadding = SettingsPickerOptionContentPadding
private val OptionBottomSpacing = SettingsPickerOptionBottomSpacing

@Composable
private fun rememberBgSurface(theme: String): Pair<Color, Color> {
    val colors = resolveClintColors(rememberIsLightTheme(theme))
    return colors.background to colors.surface
}

@Composable
fun ThemeSelectorDialog(current: String, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()
    ClintDialog(title = stringResource(R.string.pref_app_theme_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        data class ThemeOption(val key: String, val titleRes: Int, val descRes: Int, val drawableRes: Int)
        listOf(
            ThemeOption("system", R.string.theme_system, R.string.theme_system_desc, R.drawable.theme_swatch_system),
            ThemeOption("dark", R.string.theme_dark, R.string.theme_dark_desc, R.drawable.theme_swatch_dark),
            ThemeOption("light", R.string.theme_light, R.string.theme_light_desc, R.drawable.theme_swatch_light)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, current == option.key)
            ) {
                DrawableImage(option.drawableRes, modifier = Modifier.size(44.dp))
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun AddressBarPositionDialog(
    current: String,
    theme: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val (bg, surface) = rememberBgSurface(theme)
    val onSurface = colors.onSurface
    val scrollState = rememberScrollState()

    ClintDialog(title = stringResource(R.string.pref_address_bar_position_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        data class AddrOption(val key: String, val titleRes: Int, val descRes: Int)
        listOf(
            AddrOption("top", R.string.address_bar_position_top, R.string.address_bar_position_top_desc),
            AddrOption("bottom", R.string.address_bar_position_bottom, R.string.address_bar_position_bottom_desc),
            AddrOption("split", R.string.address_bar_position_split, R.string.address_bar_position_split_desc)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, current == option.key)
            ) {
                AddressBarPreview(option.key, bg, surface, onSurface)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun TabBarPositionDialog(
    title: String,
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()
    ClintDialog(title = title, hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        listOf(
            Triple(com.simplefox.browser.tabs.TabBarPlacement.FOLLOW, R.string.tab_bar_position_follow, R.string.tab_bar_position_follow_desc),
            Triple(com.simplefox.browser.tabs.TabBarPlacement.TOP, R.string.address_bar_position_top, R.string.tab_bar_position_top_desc),
            Triple(com.simplefox.browser.tabs.TabBarPlacement.BOTTOM, R.string.address_bar_position_bottom, R.string.tab_bar_position_bottom_desc)
        ).forEach { (key, titleRes, descRes) ->
            SelectableCard(
                selected = current == key, onClick = { onSelect(key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, current == key)
            ) {
                Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                    Text(stringResource(titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == key, colors.primary)
            }
        }
    }
}

fun tabPreviewResolutionSummaryRes(key: String): Int = when (com.simplefox.browser.tabs.TabThumbnailCache.normalizeResolution(key)) {
    com.simplefox.browser.tabs.TabThumbnailCache.RES_LOW -> R.string.tab_preview_resolution_low
    com.simplefox.browser.tabs.TabThumbnailCache.RES_HIGH -> R.string.tab_preview_resolution_high
    com.simplefox.browser.tabs.TabThumbnailCache.RES_VERY_HIGH -> R.string.tab_preview_resolution_very_high
    else -> R.string.tab_preview_resolution_standard
}

@Composable
fun TabPreviewResolutionDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()
    val selectedKey = com.simplefox.browser.tabs.TabThumbnailCache.normalizeResolution(current)
    ClintDialog(
        title = stringResource(R.string.pref_tab_preview_resolution_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss, scrollState = scrollState
    ) {
        listOf(
            Triple(com.simplefox.browser.tabs.TabThumbnailCache.RES_LOW, R.string.tab_preview_resolution_low, R.string.tab_preview_resolution_low_desc),
            Triple(com.simplefox.browser.tabs.TabThumbnailCache.RES_STANDARD, R.string.tab_preview_resolution_standard, R.string.tab_preview_resolution_standard_desc),
            Triple(com.simplefox.browser.tabs.TabThumbnailCache.RES_HIGH, R.string.tab_preview_resolution_high, R.string.tab_preview_resolution_high_desc),
            Triple(com.simplefox.browser.tabs.TabThumbnailCache.RES_VERY_HIGH, R.string.tab_preview_resolution_very_high, R.string.tab_preview_resolution_very_high_desc)
        ).forEach { (key, titleRes, descRes) ->
            SelectableCard(
                selected = selectedKey == key, onClick = { onSelect(key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, selectedKey == key)
            ) {
                Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                    Text(stringResource(titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(selectedKey == key, colors.primary)
            }
        }
    }
}

@Composable
fun ScrollHideModeDialog(
    current: String,
    addressBarPosition: String,
    theme: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val (bg, surface) = rememberBgSurface(theme)
    val onSurface = colors.onSurface
    val scrollState = rememberScrollState()

    ClintDialog(title = stringResource(R.string.pref_nested_scroll_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        listOf("off", "search_bar", "navigation_bar", "both").forEach { kind ->
            if (scrollCardVisible(kind, addressBarPosition)) {
                val (titleRes, descRes) = when (kind) {
                    "off" -> R.string.nested_scroll_off to R.string.nested_scroll_off_desc
                    "search_bar" -> R.string.nested_scroll_search_bar to R.string.nested_scroll_search_bar_desc
                    "navigation_bar" -> navBarSlotTitleRes(addressBarPosition) to navBarSlotDescRes(addressBarPosition)
                    else -> R.string.nested_scroll_both to R.string.nested_scroll_both_desc
                }
                val selected = when (kind) {
                    "search_bar" -> current == "search_bar" && addressBarPosition != "bottom"
                    "navigation_bar" -> (current == "search_bar" && addressBarPosition == "bottom") ||
                        (current == "navigation_bar" && addressBarPosition == "split")
                    else -> current == kind
                }
                SelectableCard(
                    selected = selected, onClick = { onSelect(kind) },
                    cardBackground = colors.surfaceVariant, primary = colors.primary,
                    contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                    modifier = Modifier.scrollToSelection(scrollState, selected)
                ) {
                    ScrollHidePreview(kind, addressBarPosition, bg, surface, onSurface, animate = kind != "off")
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                        Text(stringResource(titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    CheckSlot(selected, colors.primary)
                }
            }
        }
    }
}

@Composable
fun ExitConfirmationDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }
    val scrollState = rememberScrollState()

    ClintDialog(
        title = stringResource(R.string.exit_confirmation_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        scrollState = scrollState,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        data class ExitOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            ExitOption("off", R.string.exit_confirmation_off, R.string.exit_confirmation_off_desc, false),
            ExitOption("toast", R.string.exit_confirmation_toast, R.string.exit_confirmation_toast_desc, true),
            ExitOption("dialog", R.string.exit_confirmation_dialog, R.string.exit_confirmation_dialog_desc, false)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, sel)
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.showDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}

@Composable
fun PopupAlertStyleDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()

    ClintDialog(
        title = stringResource(R.string.pref_popup_alert_style_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        scrollState = scrollState
    ) {
        data class StyleOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            StyleOption("dialog", R.string.popup_alert_style_dialog, R.string.popup_alert_style_dialog_desc, true),
            StyleOption("snackbar", R.string.popup_alert_style_snackbar, R.string.popup_alert_style_snackbar_desc, false)
        ).forEach { option ->
            val sel = current == option.key
            SelectableCard(
                selected = sel, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, sel)
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.showDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}

@Composable
fun LanguageSelectorDialog(current: String, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()

    ClintDialog(title = stringResource(R.string.pref_language_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        val systemSelected = current == LocaleHelper.LANGUAGE_SYSTEM
        SelectableCard(
            selected = systemSelected, onClick = { onSelect(LocaleHelper.LANGUAGE_SYSTEM) },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
            modifier = Modifier.scrollToSelection(scrollState, systemSelected)
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(stringResource(R.string.language_system), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.language_system_desc), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            CheckSlot(systemSelected, colors.primary)
        }
        languageOptions.forEach { option ->
            val selected = current == option.tag
            SelectableCard(
                selected = selected, onClick = { onSelect(option.tag) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, selected)
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(stringResource(option.nameRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (option.tag == LocaleHelper.BASE_LANGUAGE_TAG) {
                        Text(
                            stringResource(R.string.language_base_desc),
                            color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                CheckSlot(selected, colors.primary)
            }
        }
    }
}
