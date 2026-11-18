package com.simplefox.browser.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import com.simplefox.browser.ui.foundation.LocalTextStyle

data class ClintColors(
    val background: Color,
    val onSurface: Color,
    val secondaryText: Color,
    val cardBackground: Color,
    val buttonBackground: Color,
    val surfaceVariant: Color,
    val popupBackground: Color,
    val primary: Color,
    val iconTint: Color,
    val divider: Color,
    val popupText: Color,
    val surface: Color,
    val buttonIconTint: Color,
    val popupStroke: Color,
    val popupCheck: Color,
    val onPrimary: Color,
    val colorError: Color,
    val colorErrorContainer: Color,
    val colorOnErrorContainer: Color,
    val buttonTextColor: Color,
    val addressBarColor: Color,
    val isLight: Boolean
)

val LocalClintColors = compositionLocalOf<ClintColors> {
    error("ClintComposeTheme not applied")
}

private val LightColors = ClintColors(
    background = Color(0xFFFAFAFA),
    onSurface = Color(0xDE000000),
    secondaryText = Color(0x99000000),
    cardBackground = Color(0xFFFFFFFF),
    buttonBackground = Color(0xFF1A1A1A),
    surfaceVariant = Color(0x0A000000),
    popupBackground = Color(0xFFFFFFFF),
    primary = Color(0xFF1A1A1A),
    iconTint = Color(0xFF1A1A1A),
    divider = Color(0x1A000000),
    popupText = Color(0xDE000000),
    surface = Color(0xFFFFFFFF),
    buttonIconTint = Color(0xFFFFFFFF),
    popupStroke = Color(0x1A000000),
    popupCheck = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFFFFFF),
    colorError = Color(0xFFBA1A1A),
    colorErrorContainer = Color(0xFFFFDAD6),
    colorOnErrorContainer = Color(0xFF410002),
    buttonTextColor = Color(0xFFFFFFFF),
    addressBarColor = Color(0xFFF0F0F0),
    isLight = true
)

private val DarkColors = ClintColors(
    background = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    secondaryText = Color(0x99FFFFFF),
    cardBackground = Color(0xFF1E1E1E),
    buttonBackground = Color(0xFF505050),
    surfaceVariant = Color(0x1AFFFFFF),
    popupBackground = Color(0xFF1E1E1E),
    primary = Color(0xFFE0E0E0),
    iconTint = Color(0xFFE0E0E0),
    divider = Color(0x22FFFFFF),
    popupText = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    buttonIconTint = Color(0xFFFFFFFF),
    popupStroke = Color(0x33FFFFFF),
    popupCheck = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF121212),
    colorError = Color(0xFFFFB4AB),
    colorErrorContainer = Color(0xFF93000A),
    colorOnErrorContainer = Color(0xFFFFDAD6),
    buttonTextColor = Color(0xFFFFFFFF),
    addressBarColor = Color(0xFF2C2C2C),
    isLight = false
)

fun resolveClintColors(isLight: Boolean): ClintColors = if (isLight) LightColors else DarkColors

data class ThemeChrome(
    val background: Int,
    val statusBar: Int,
    val navigationBar: Int,
    val cardBackground: Int,
    val primary: Int,
    val lightStatusBar: Boolean,
    val lightNavigationBar: Boolean
)

fun resolveThemeChrome(theme: String): ThemeChrome {
    val isLight = ThemeMode.isLight(theme)
    val colors = resolveClintColors(isLight)
    val toolbar = colors.cardBackground.toArgb()
    return ThemeChrome(
        background = colors.background.toArgb(),
        statusBar = toolbar,
        navigationBar = toolbar,
        cardBackground = toolbar,
        primary = colors.primary.toArgb(),
        lightStatusBar = isLight,
        lightNavigationBar = isLight
    )
}

@Composable
fun ClintComposeTheme(theme: String, content: @Composable () -> Unit) {
    val clintColors = resolveClintColors(rememberIsLightTheme(theme))
    CompositionLocalProvider(
        LocalClintColors provides clintColors,
        LocalTextStyle provides TextStyle(color = clintColors.onSurface)
    ) {
        content()
    }
}
