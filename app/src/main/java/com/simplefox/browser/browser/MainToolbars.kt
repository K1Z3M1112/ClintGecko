package com.simplefox.browser.browser

import com.simplefox.browser.browser.delegates.*
import com.simplefox.browser.ui.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.simplefox.browser.ui.foundation.Icon
import com.simplefox.browser.ui.foundation.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplefox.browser.R
import com.simplefox.browser.ui.theme.LocalClintColors

/** Slides the top bar with the scroll-hide fraction and reports its measured height. Reads happen in draw/layout only. */
internal fun Modifier.topBarChrome(activity: MainActivity, state: MainUiState, ignoreEmpty: Boolean): Modifier = this
    .graphicsLayer {
        val contentBarHeight = (state.topBarFullHeightPx - state.statusBarInsetPx).toFloat()
        translationY = -state.topBarFraction * contentBarHeight
    }
    .onGloballyPositioned { coordinates ->
        val h = coordinates.size.height
        if ((!ignoreEmpty || h > 0) && state.topBarFullHeightPx != h) {
            state.topBarFullHeightPx = h
            activity.swipeRefreshView.setProgressViewOffset(false, h + 4, h + 72)
            activity.updateMainContentInsets()
        }
    }

/** Bottom counterpart of [topBarChrome]. */
internal fun Modifier.bottomBarChrome(activity: MainActivity, state: MainUiState, ignoreEmpty: Boolean): Modifier = this
    .graphicsLayer { translationY = state.bottomBarFraction * state.bottomBarFullHeightPx }
    .onGloballyPositioned { coordinates ->
        val h = coordinates.size.height
        if ((!ignoreEmpty || h > 0) && state.bottomBarFullHeightPx != h) {
            state.bottomBarFullHeightPx = h
            activity.updateMainContentInsets()
        }
    }

@Composable
internal fun TopToolbar(
    activity: MainActivity,
    state: MainUiState,
    statusBarPaddingPx: Int,
    onTabCountClick: () -> Unit,
    onOpenTabGroup: (String) -> Unit,
    showTabStrip: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .topBarChrome(activity, state, ignoreEmpty = true)
            .background(colors.surface)
            .padding(top = with(density) { statusBarPaddingPx.toDp() })
    ) {
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
        AddressBarRow(
            activity = activity,
            isIncognito = state.isIncognito,
            addressBarText = state.addressBarTextTop,
            isSecure = state.addressBarSecureTop,
            tabCountText = state.tabCountText,
            activeTabId = state.activeTabId,
            onAddressBarClick = { activity.openSearchOverlay(isBottom = false) },
            onTabCountClick = onTabCountClick,
            showHomeButton = state.showHomeButton,
            onHomeClick = { activity.navGoHome() },
            onSwipeTabChange = { direction -> activity.onSwipeTabChange(direction) },
            onSiteInfoClick = { if (!activity.openSiteInfo()) activity.openSearchOverlay(isBottom = false) },
            openTabsSwipeDirection = 1
        )
        PageLoadProgress(state)
    }
}

@Composable
internal fun TopTabBarsAccessory(
    activity: MainActivity,
    state: MainUiState,
    statusBarPaddingPx: Int,
    showTabStrip: Boolean,
    onOpenTabGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    DisposableEffect(Unit) {
        onDispose {
            state.topBarFullHeightPx = 0
            activity.updateMainContentInsets()
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .topBarChrome(activity, state, ignoreEmpty = false)
            .background(colors.surface)
            .padding(top = with(density) { statusBarPaddingPx.toDp() })
    ) {
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
    }
}

@Composable
internal fun BottomTabBarsAccessory(
    activity: MainActivity,
    state: MainUiState,
    navBarPaddingPx: Int,
    showTabStrip: Boolean,
    onOpenTabGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    DisposableEffect(Unit) {
        onDispose {
            state.bottomBarFullHeightPx = 0
            activity.updateMainContentInsets()
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bottomBarChrome(activity, state, ignoreEmpty = false)
            .background(colors.surface)
            .padding(bottom = with(density) { navBarPaddingPx.toDp() })
    ) {
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
    }
}

@Composable
private fun TabStripSlot(activity: MainActivity, state: MainUiState, onOpenTabGroup: (String) -> Unit) {
    val strip = state.tabStrip
    AnimatedVisibility(
        visible = strip != null,
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(200))
    ) {
        val lastStrip = remember { mutableStateOf(strip) }
        if (strip != null) lastStrip.value = strip
        lastStrip.value?.let { shown ->
            com.simplefox.browser.tabs.TabStrip(
                state = shown,
                onSelectTab = { id -> activity.focusTabById(id) },
                onCloseTab = { id -> activity.closeTabFromStrip(id) },
                onCloseOtherTabs = { id -> activity.closeOtherTabsFromStrip(id) },
                onNewTab = { activity.openNewTabFromStrip() },
                onNewTabInGroup = { groupId -> activity.openNewTabInGroup(groupId) },
                onAddToNewGroup = { id -> state.tabStripNewGroupTabId = id },
                onRemoveFromGroup = { id -> activity.removeTabFromGroup(id) },
                onToggleGroup = { groupId -> activity.toggleTabGroupCollapsed(groupId) },
                onOpenGroup = onOpenTabGroup
            )
        }
    }
}

@Composable
private fun PageLoadProgress(state: MainUiState) {
    val colors = LocalClintColors.current
    val primary = colors.primary
    val shown = remember { androidx.compose.animation.core.Animatable(0f) }
    // Progress is animated outside composition and painted in the draw phase, so a load tick
    // redraws this 3dp strip only; it recomposes nothing.
    LaunchedEffect(state) {
        snapshotFlow { if (state.isPageLoading) maxOf(state.pageLoadProgress, 20) / 100f else 0f }
            .collectLatest { target ->
                if (target == 0f) shown.snapTo(0f)
                else shown.animateTo(target, tween(250))
            }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .drawBehind {
                if (state.isPageLoading) {
                    drawRect(primary, size = androidx.compose.ui.geometry.Size(size.width * shown.value, size.height))
                }
            }
    )
}

@Composable
internal fun BottomToolbar(
    activity: MainActivity,
    state: MainUiState,
    bottomPaddingPx: Int,
    onTabCountClick: () -> Unit,
    onOpenTabGroup: (String) -> Unit,
    showTabStrip: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    // The bar stays put when the keyboard opens (the keyboard just covers it), so nothing here depends on the IME.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bottomBarChrome(activity, state, ignoreEmpty = true)
            .background(colors.surface)
            .padding(bottom = with(density) { bottomPaddingPx.toDp() })
    ) {
        PageLoadProgress(state)
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
        AddressBarRow(
            activity = activity,
            isIncognito = state.isIncognito,
            addressBarText = state.addressBarTextBottom,
            isSecure = state.addressBarSecureBottom,
            tabCountText = state.tabCountText,
            activeTabId = state.activeTabId,
            onAddressBarClick = { activity.openSearchOverlay(isBottom = true) },
            onTabCountClick = onTabCountClick,
            showHomeButton = state.showHomeButton,
            onHomeClick = { activity.navGoHome() },
            onSwipeTabChange = { direction -> activity.onSwipeTabChange(direction) },
            onSiteInfoClick = { if (!activity.openSiteInfo()) activity.openSearchOverlay(isBottom = true) },
            openTabsSwipeDirection = -1,
            isBottomBar = true
        )
    }
}

@Composable
internal fun BottomNavBar(
    activity: MainActivity,
    state: MainUiState,
    navBarPaddingPx: Int,
    showTabStrip: Boolean,
    onOpenTabGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bottomBarChrome(activity, state, ignoreEmpty = true)
            .background(colors.surface)
    ) {
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
        BottomNavButtons(activity, state, navBarPaddingPx)
    }
}

@Composable
private fun BottomNavButtons(activity: MainActivity, state: MainUiState, navBarPaddingPx: Int) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(bottom = with(density) { navBarPaddingPx.toDp() }),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        NavIconButton(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), enabled = state.canGoBack) { activity.navGoBack() }
        NavIconButton(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.forward), enabled = state.canGoForward) { activity.navGoForward() }
        NavIconButton(androidx.compose.material.icons.Icons.Filled.Home, stringResource(R.string.home), enabled = true) { activity.navGoHome() }
        NavIconButton(
            if (state.isPageLoading) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.Filled.Refresh,
            stringResource(R.string.refresh),
            enabled = true
        ) { activity.navRefreshOrStop() }
        NavIconButton(
            if (state.isBookmarked) androidx.compose.material.icons.Icons.Filled.Bookmark else androidx.compose.material.icons.Icons.Filled.BookmarkBorder,
            stringResource(R.string.content_desc_bookmark),
            enabled = state.hasActiveUrl
        ) { activity.navToggleBookmark() }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavIconButton(
    iconRes: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalClintColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).fillMaxSize()
    ) {
        Icon(
            imageVector = iconRes,
            contentDescription = description,
            tint = colors.iconTint,
            modifier = Modifier.alpha(if (enabled) 1.0f else 0.38f)
        )
    }
}

@Composable

internal fun engineDisplayName(engine: String, customName: String): String = when (engine) {
    "brave" -> stringResource(R.string.engine_brave)
    "ecosia" -> stringResource(R.string.engine_ecosia)
    "google" -> stringResource(R.string.engine_google)
    "custom" -> customName.ifBlank { stringResource(R.string.engine_custom) }
    else -> stringResource(R.string.engine_duckduckgo)
}
