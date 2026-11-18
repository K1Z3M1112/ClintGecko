package com.simplefox.browser.browser

import com.simplefox.browser.browser.delegates.*
import com.simplefox.browser.browser.home.ClintHomeScreen
import com.simplefox.browser.ui.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.simplefox.browser.tabs.TabMenuScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.viewinterop.AndroidView
import com.simplefox.browser.R
import com.simplefox.browser.ui.theme.LocalClintColors

@Composable
internal fun MainScreen(activity: MainActivity, state: MainUiState) {
    val density = LocalDensity.current
    val colors = LocalClintColors.current
    var tabSwitcherOpen by remember { mutableStateOf(false) }
    val hideStatusBar = state.hideStatusBar
    val hideSystemNavigation = state.hideSystemNavigation
    val rawStatusBarPx = WindowInsets.statusBars.getTop(density)
    val rawNavBarPx = WindowInsets.navigationBars.getBottom(density)
    @OptIn(ExperimentalLayoutApi::class)
    val imeTargetPx = WindowInsets.imeAnimationTarget.getBottom(density)

    LaunchedEffect(rawStatusBarPx) {
        if (rawStatusBarPx > 0) state.cachedStatusBarInsetPx = rawStatusBarPx
    }
    // While the search overlay is open, and for a moment after it closes, the keyboard height must not become the
    // web view's bottom padding: that relayouts the page twice (up, then back down) while the keyboard animates.
    // A plain holder is used on purpose; a state flag here would recompose all of MainScreen on open and close.
    val suppressIme = remember { booleanArrayOf(false) }
    val latestImeTargetPx by rememberUpdatedState(imeTargetPx)
    LaunchedEffect(imeTargetPx) {
        if (!state.searchOverlayOpen && !suppressIme[0]) {
            state.imeInsetPx = imeTargetPx
            activity.updateMainContentInsets()
        }
    }
    LaunchedEffect(state.searchOverlayOpen) {
        if (state.searchOverlayOpen) {
            suppressIme[0] = true
            state.imeInsetPx = 0
            activity.updateMainContentInsets()
        } else if (suppressIme[0]) {
            kotlinx.coroutines.delay(350)
            suppressIme[0] = false
            state.imeInsetPx = latestImeTargetPx
            activity.updateMainContentInsets()
        }
    }
    LaunchedEffect(rawNavBarPx) {
        if (rawNavBarPx > 0) state.navBarInsetPx = rawNavBarPx
    }
    val effectiveStatusBarPx = if (hideStatusBar) {
        0
    } else {
        rawStatusBarPx.takeIf { it > 0 } ?: state.cachedStatusBarInsetPx
    }
    val effectiveNavBarPx = if (hideSystemNavigation) 0 else state.navBarInsetPx
    val systemNavPx = if (hideSystemNavigation || state.isFullscreen) 0 else rawNavBarPx
    LaunchedEffect(systemNavPx) {
        state.systemNavInsetPx = systemNavPx
        activity.updateMainContentInsets()
    }
    LaunchedEffect(effectiveStatusBarPx) {
        state.statusBarInsetPx = effectiveStatusBarPx
        activity.updateMainContentInsets()
    }

    // The tab switcher is opaque, but the live GeckoView underneath kept rendering and being composited on every
    // frame while the grid scrolled. Hide it once the switcher has finished fading in, show it again right away.
    var webViewHidden by remember { mutableStateOf(false) }
    LaunchedEffect(tabSwitcherOpen) {
        if (tabSwitcherOpen) {
            kotlinx.coroutines.delay(260)
            webViewHidden = true
        } else {
            webViewHidden = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.surface)) {

        AndroidView(
            factory = { activity.swipeRefreshView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // MainScreen also recomposes for small browser chrome updates (for example, load progress).
                // Avoid re-applying identical View properties: setPadding can trigger a full Android
                // measure/layout pass that reaches the Gecko surface even when the insets did not move.
                val targetVisibility = if (webViewHidden) android.view.View.INVISIBLE else android.view.View.VISIBLE
                if (view.visibility != targetVisibility) view.visibility = targetVisibility

                val targetTop = state.contentPaddingTopPx
                val targetBottom = if (state.isFullscreen) 0 else maxOf(state.contentPaddingBottomPx, state.imeInsetPx)
                if (view.paddingLeft != 0 || view.paddingTop != targetTop ||
                    view.paddingRight != 0 || view.paddingBottom != targetBottom
                ) {
                    view.setPadding(0, targetTop, 0, targetBottom)
                }
            }
        )

        if (state.isHomePage && !state.isFullscreen) {
            ClintHomeScreen(
                activity = activity,
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .contentInsetsPadding(state)
            )
        }

        val openTabSwitcher: () -> Unit = {
            val tabs = activity.tabManager.tabs
            com.simplefox.browser.tabs.TabThumbnailCache.preload(
                activity,
                tabs.map { it.id },
                tabs.getOrNull(activity.tabManager.activeIndex)?.id
            )
            activity.captureActiveTabThen { tabSwitcherOpen = true }
        }
        val openTabGroup: (String) -> Unit = { groupId ->
            state.tabMenuFocusGroupId = groupId
            openTabSwitcher()
        }
        val tabStripAtTop = resolveTabBarAtTop(state.tabStripPosition, state.addressBarPosition)
        val chromeVisible = !state.isFullscreen && !state.isShortcutFrameless

        if (!state.isFullscreen && !state.isShortcutFrameless && (state.addressBarPosition == AddressBarPosition.TOP || state.addressBarPosition == AddressBarPosition.SPLIT)) {
            TopToolbar(
                activity = activity,
                state = state,
                statusBarPaddingPx = effectiveStatusBarPx,
                onTabCountClick = openTabSwitcher,
                onOpenTabGroup = openTabGroup,
                showTabStrip = tabStripAtTop,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        if (!state.isFullscreen && !state.isShortcutFrameless && state.addressBarPosition == AddressBarPosition.SPLIT) {
            BottomNavBar(
                activity = activity,
                state = state,
                navBarPaddingPx = effectiveNavBarPx,
                showTabStrip = !tabStripAtTop,
                onOpenTabGroup = openTabGroup,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        if (!state.isFullscreen && !state.isShortcutFrameless && state.addressBarPosition == AddressBarPosition.BOTTOM) {
            BottomToolbar(
                activity = activity,
                state = state,
                bottomPaddingPx = effectiveNavBarPx,
                onTabCountClick = openTabSwitcher,
                onOpenTabGroup = openTabGroup,
                showTabStrip = !tabStripAtTop,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        if (chromeVisible && state.addressBarPosition == AddressBarPosition.BOTTOM && tabStripAtTop) {
            TopTabBarsAccessory(
                activity = activity,
                state = state,
                statusBarPaddingPx = effectiveStatusBarPx,
                showTabStrip = tabStripAtTop,
                onOpenTabGroup = openTabGroup,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        if (chromeVisible && state.addressBarPosition == AddressBarPosition.TOP && !tabStripAtTop) {
            BottomTabBarsAccessory(
                activity = activity,
                state = state,
                navBarPaddingPx = effectiveNavBarPx,
                showTabStrip = !tabStripAtTop,
                onOpenTabGroup = openTabGroup,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        com.simplefox.browser.translation.TranslateBar(
            request = state.translateBarRequest,
            visible = !state.isFullscreen && !state.searchOverlayOpen && !tabSwitcherOpen,
            activeTabId = state.activeTabId,
            onTranslate = { view, from, to -> activity.translatePage(view, from, to) },
            onRestore = { view -> activity.restoreTranslatedPage(view) },
            onOpenSettings = { activity.openTranslationSettings() },
            onDismiss = { state.translateBarRequest = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .contentInsetsPadding(state, includeBottom = false, extraTop = 8.dp)
                .padding(horizontal = 12.dp)
        )

        AnimatedVisibility(
            visible = state.searchOverlayOpen,
            // Fade only: expand/shrink re-measures the whole overlay every frame while the keyboard is also animating.
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100)),
            // The field is always at the top while typing, whichever bar was tapped, so the keyboard never pushes it around.
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            SearchOverlay(
                // Read once per open. Observing it would recompose this whole scope on every keystroke.
                initialText = remember { androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation { state.searchQuery } },
                isBottom = false,
                hint = stringResource(
                    R.string.search_bar_hint,
                    engineDisplayName(
                        searchEngineFor(activity.prefs, state.isIncognito),
                        customSearchEngineName(activity.prefs, state.isIncognito)
                    )
                ),
                suggestions = state.suggestions,
                statusBarPaddingPx = effectiveStatusBarPx,
                navBarPaddingPx = effectiveNavBarPx,
                onQueryChange = { activity.onSearchQueryChanged(it) },
                onSubmit = { activity.onSearchSubmitted(it) },
                onVoiceSearch = { activity.handleVoiceSearchTap() },
                onClose = { activity.closeSearchOverlay() },
                onSuggestionClick = { activity.onSuggestionChosen(it) },
                onSuggestionDelete = { activity.onSuggestionHistoryDelete(it) },
                modifier = Modifier.fillMaxSize(),
                incognitoKeyboard = state.isIncognito && com.simplefox.browser.privacy.IncognitoKeyboard.isEnabled(activity)
            )
        }

        AnimatedVisibility(
            visible = tabSwitcherOpen,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.94f, animationSpec = tween(160))
        ) {
            TabMenuScreen(activity = activity, onDismiss = { tabSwitcherOpen = false })
        }

        // Dialog/sheet requests live in their own scope so a web-view event (JS dialog, auth, permission…)
        // never recomposes the bars, and the bars never recompose the dialogs.
        MainDialogsHost(activity, state, effectiveStatusBarPx, effectiveNavBarPx)

        if (!state.isFullscreen && effectiveStatusBarPx > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(with(density) { effectiveStatusBarPx.toDp() })
                    .background(colors.surface)
            )
        }
    }
}

/**
 * Pads a child by the web-view content insets. The insets are read while laying out instead of while
 * composing, so a change (bars hiding/showing on scroll) relayouts only this child and never recomposes MainScreen.
 */
private fun Modifier.contentInsetsPadding(
    state: MainUiState,
    includeBottom: Boolean = true,
    extraTop: androidx.compose.ui.unit.Dp = 0.dp
): Modifier = this.layout { measurable, constraints ->
    val top = state.contentPaddingTopPx + extraTop.roundToPx()
    val bottom = if (includeBottom) state.contentPaddingBottomPx else 0
    val placeable = measurable.measure(constraints.offset(vertical = -(top + bottom)))
    layout(
        constraints.constrainWidth(placeable.width),
        constraints.constrainHeight(placeable.height + top + bottom)
    ) { placeable.placeRelative(0, top) }
}

internal fun resolveTabBarAtTop(position: String, addressBarPosition: AddressBarPosition): Boolean =
    when (com.simplefox.browser.tabs.TabBarPlacement.normalize(position)) {
        com.simplefox.browser.tabs.TabBarPlacement.TOP -> true
        com.simplefox.browser.tabs.TabBarPlacement.BOTTOM -> false
        else -> addressBarPosition != AddressBarPosition.BOTTOM
    }
