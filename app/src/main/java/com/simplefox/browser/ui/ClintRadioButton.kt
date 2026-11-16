package com.simplefox.browser.ui

import com.simplefox.browser.ui.foundation.RadioButton
import com.simplefox.browser.ui.foundation.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.simplefox.browser.ui.theme.LocalClintColors

@Composable
fun ClintRadioButton(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    RadioButton(
        selected = selected,
        onClick = null,
        modifier = modifier,
        colors = RadioButtonDefaults.colors(
            selectedColor = colors.primary,
            unselectedColor = colors.secondaryText,
            disabledSelectedColor = colors.primary.copy(alpha = 0.38f),
            disabledUnselectedColor = colors.secondaryText.copy(alpha = 0.38f)
        )
    )
}
