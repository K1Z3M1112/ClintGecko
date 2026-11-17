package com.simplefox.browser.setup

import com.simplefox.browser.ui.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.simplefox.browser.ui.theme.ClintComposeTheme
import com.simplefox.browser.ui.theme.LocalClintColors

private val WideScreenBreakpointDp = 600
private val CenteredContentMaxWidth = 480.dp
private const val SetupPageCount = 4

@Composable
fun SetupScreen(
    state: SetupUiState,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
    onHideStatusBarToggled: (Boolean) -> Unit,
    onHideSystemNavigationToggled: (Boolean) -> Unit,
    onThemeSelected: (String) -> Unit,
    onAccentSelected: (String) -> Unit,
    onIntensitySelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onAddressBarPositionSelected: (String) -> Unit,
    onScrollHideModeSelected: (String) -> Unit,
    onEngineSelected: (String) -> Unit,
    onCustomEngineSaved: (name: String, url: String) -> Unit,
    onContinueFromWelcome: () -> Unit,
    onNextFromLayoutPage: () -> Unit,
    onNextFromEnginePage: () -> Unit,
) {
    ClintComposeTheme(theme = state.theme) {
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
                        transitionSpec = {
                            (slideInHorizontally(animationSpec = tween(300)) { width -> width })
                                .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width })
                        },
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
                                        onTermsClick = onTermsClick,
                                        onContinue = onContinueFromWelcome
                                    )
                                    1 -> SetupThemePage(
                                        scrollState = state.themePageScrollState,
                                        theme = state.theme,
                                        accent = state.accent,
                                        intensity = state.intensity,
                                        onThemeSelected = onThemeSelected,
                                        onAccentSelected = onAccentSelected,
                                        onIntensitySelected = onIntensitySelected,
                                        onNext = { state.currentPage = 2 }
                                    )
                                    2 -> SetupLayoutPage(
                                        addressBarPosition = state.addressBarPosition,
                                        scrollHideMode = state.scrollHideMode,
                                        hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                                        theme = state.theme,
                                        accent = state.accent,
                                        onAddressBarPositionSelected = onAddressBarPositionSelected,
                                        onScrollHideModeSelected = onScrollHideModeSelected,
                                        onHideStatusBarToggled = onHideStatusBarToggled, onHideSystemNavigationToggled = onHideSystemNavigationToggled,
                                        onNext = onNextFromLayoutPage
                                    )
                                    else -> SetupEnginePage(
                                        engine = state.engine,
                                        customName = state.customEngineName,
                                        customUrl = state.customEngineUrl,
                                        hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                                        onEngineSelected = onEngineSelected,
                                        onCustomEngineSaved = onCustomEngineSaved,
                                        onNext = onNextFromEnginePage
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
