package com.simplefox.browser.setup

import com.simplefox.browser.ui.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import com.simplefox.browser.ui.foundation.Surface
import androidx.compose.runtime.Composable
import com.simplefox.browser.ui.foundation.Text
import com.simplefox.browser.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import com.simplefox.browser.ui.foundation.LocalTextStyle
import com.simplefox.browser.ui.theme.ClintColors
import com.simplefox.browser.ui.theme.LocalClintColors
import com.simplefox.browser.ui.theme.rememberIsLightTheme
import com.simplefox.browser.ui.theme.resolveClintColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

private val WideScreenBreakpointDp = 600
private val CenteredContentMaxWidth = 480.dp
private const val SetupPageCount = 4

/**
 * Themes everything under it from the setup state itself, so picking a theme
 * recolors the screen immediately without recreating the activity or touching preferences.
 */
@Composable
fun SetupTheme(state: SetupUiState, content: @Composable () -> Unit) {
    val isLight = rememberIsLightTheme(state.theme)
    val colors: ClintColors = remember(isLight) { resolveClintColors(isLight) }
    CompositionLocalProvider(
        LocalClintColors provides colors,
        LocalTextStyle provides TextStyle(color = colors.onSurface),
        content = content
    )
}

@Composable
fun SetupScreen(
    state: SetupUiState,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onHideStatusBarToggled: (Boolean) -> Unit,
    onHideSystemNavigationToggled: (Boolean) -> Unit,
    onThemeSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onAddressBarPositionSelected: (String) -> Unit,
    onScrollHideModeSelected: (String) -> Unit,
    onEngineSelected: (String) -> Unit,
    onCustomEngineSaved: (name: String, url: String) -> Unit,
    onContinueFromWelcome: () -> Unit,
    onNextFromLayoutPage: () -> Unit,
    onNextFromEnginePage: () -> Unit,
) {
    run {
        val colors = LocalClintColors.current
        Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
            val insets = WindowInsets.systemBars.asPaddingValues()
            val layoutDirection = LocalLayoutDirection.current
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = insets.calculateStartPadding(layoutDirection),
                        end = insets.calculateEndPadding(layoutDirection),
                        top = if (state.hideStatusBar) 0.dp else insets.calculateTopPadding(),
                        bottom = if (state.hideSystemNavigation) 0.dp else insets.calculateBottomPadding()
                    )
            ) {
                val isWideScreen = LocalConfiguration.current.screenWidthDp >= WideScreenBreakpointDp

                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Box(
                            Modifier.then(if (isWideScreen) Modifier.widthIn(max = CenteredContentMaxWidth) else Modifier.fillMaxWidth())
                        ) {
                            SetupStepDots(
                                total = SetupPageCount,
                                current = state.currentPage.coerceIn(0, SetupPageCount - 1),
                                primary = colors.primary,
                                track = colors.surfaceVariant,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = state.currentPage,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        label = "setupPage"
                    ) { page ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .then(if (isWideScreen) Modifier.widthIn(max = CenteredContentMaxWidth) else Modifier.fillMaxSize())
                            ) {
                                when (page) {
                                    0 -> SetupWelcomePage(
                                        language = state.language,
                                        hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                                        onLanguageSelected = onLanguageSelected,
                                        consentChecked = state.consentChecked,
                                        onConsentCheckedChange = { state.consentChecked = it },
                                        onPrivacyClick = onPrivacyClick,
                                        onTermsClick = onTermsClick
                                    )
                                    1 -> SetupThemePage(
                                        theme = state.theme,
                                        onThemeSelected = onThemeSelected
                                    )
                                    2 -> SetupLayoutPage(
                                        addressBarPosition = state.addressBarPosition,
                                        scrollHideMode = state.scrollHideMode,
                                        hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                                        onAddressBarPositionSelected = onAddressBarPositionSelected,
                                        onScrollHideModeSelected = onScrollHideModeSelected,
                                        onHideStatusBarToggled = onHideStatusBarToggled, onHideSystemNavigationToggled = onHideSystemNavigationToggled
                                    )
                                    else -> SetupEnginePage(
                                        engine = state.engine,
                                        customName = state.customEngineName,
                                        customUrl = state.customEngineUrl,
                                        hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                                        onEngineSelected = onEngineSelected,
                                        onCustomEngineSaved = onCustomEngineSaved
                                    )
                                }
                            }
                        }
                    }

                    SetupFooter(
                        page = state.currentPage,
                        canContinue = state.currentPage != 0 || state.consentChecked,
                        isWideScreen = isWideScreen,
                        onBack = { if (state.currentPage > 0) state.currentPage -= 1 },
                        onContinue = {
                            when (state.currentPage) {
                                0 -> onContinueFromWelcome()
                                1 -> state.currentPage = 2
                                2 -> onNextFromLayoutPage()
                                else -> onNextFromEnginePage()
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Back and Next stay pinned at the bottom so they never scroll out of reach on long pages. */
@Composable
private fun SetupFooter(
    page: Int,
    canContinue: Boolean,
    isWideScreen: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val colors = LocalClintColors.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier
                .then(if (isWideScreen) Modifier.widthIn(max = CenteredContentMaxWidth) else Modifier.fillMaxWidth())
                .padding(horizontal = 28.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (page > 0) {
                Text(
                    stringResource(R.string.back),
                    color = colors.secondaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 20.dp, vertical = 15.dp)
                )
            }
            SetupPrimaryButton(
                text = stringResource(if (page == 0) R.string.setup_terms_continue else R.string.next),
                onClick = onContinue,
                backgroundColor = colors.buttonBackground,
                enabled = canContinue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
