package com.simplefox.browser.ui
import androidx.compose.material.icons.filled.Check

import androidx.compose.foundation.layout.size
import com.simplefox.browser.ui.foundation.Icon
import com.simplefox.browser.ui.foundation.Switch
import com.simplefox.browser.ui.foundation.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.simplefox.browser.ui.theme.LocalClintColors

/**
 * Click handler of the row that hosts a switch. Rows (e.g. SettingsRow) provide it so that a
 * ClintSwitch placed inside them is itself tappable and triggers the same action as the row.
 */
val LocalSwitchRowToggle = compositionLocalOf<(() -> Unit)?> { null }

@Composable
fun ClintSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val colors = LocalClintColors.current
    val rowToggle = LocalSwitchRowToggle.current
    val handler: ((Boolean) -> Unit)? = onCheckedChange ?: rowToggle?.let { toggle -> { _: Boolean -> toggle() } }
    Switch(
        checked = checked,
        onCheckedChange = handler,
        modifier = modifier,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onSurface,
            checkedTrackColor = colors.primary,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = colors.primary,
            uncheckedThumbColor = colors.secondaryText,
            uncheckedTrackColor = colors.surfaceVariant,
            uncheckedBorderColor = colors.divider,
            uncheckedIconColor = colors.surfaceVariant,
            disabledCheckedThumbColor = colors.onSurface.copy(alpha = 0.38f),
            disabledCheckedTrackColor = colors.primary.copy(alpha = 0.38f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledCheckedIconColor = colors.primary.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = colors.secondaryText.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = colors.surfaceVariant.copy(alpha = 0.38f),
            disabledUncheckedBorderColor = colors.divider.copy(alpha = 0.38f),
            disabledUncheckedIconColor = colors.surfaceVariant.copy(alpha = 0.38f)
        )
    )
}
