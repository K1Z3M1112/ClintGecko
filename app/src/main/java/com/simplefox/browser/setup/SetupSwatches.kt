package com.simplefox.browser.setup
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.getValue

import com.simplefox.browser.ui.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.simplefox.browser.ui.foundation.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
@Composable
fun AccentSwatch(bg: Color, surface: Color, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 22.dp)
                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(surface)
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 6.dp, top = 6.dp, end = 14.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent)
        )
    }
}

private val PreviewWidth = 52.dp
private val PreviewHeight = 64.dp
private val BarHeight = 14.dp

@Composable
private fun BoxScope.ContentLinesStack(onSurface: Color, topPad: Dp, bottomPad: Dp) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(start = 6.dp, end = 6.dp, top = topPad, bottom = bottomPad),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(3.dp).alpha(0.15f).background(onSurface))
            Box(Modifier.padding(top = 4.dp).width(22.dp).height(3.dp).alpha(0.15f).background(onSurface))
            Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(3.dp).alpha(0.15f).background(onSurface))
        }
    }
}

private fun Modifier.translateYByBarHeight(translationFraction: () -> Float): Modifier =
    this.graphicsLayer { translationY = translationFraction() * BarHeight.toPx() }

private val NoFraction: () -> Float = { 0f }

@Composable
private fun BoxScope.Pill(color: Color, atTop: Boolean, translationFraction: () -> Float = NoFraction) {
    Box(
        Modifier
            .align(if (atTop) Alignment.TopCenter else Alignment.BottomCenter)
            .padding(top = if (atTop) 4.dp else 0.dp, bottom = if (atTop) 0.dp else 4.dp)
            .translateYByBarHeight(translationFraction)
            .width(28.dp)
            .height(5.dp)
            .alpha(0.35f)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}

@Composable
private fun BoxScope.NavDotsRow(onSurface: Color, translationFraction: () -> Float = NoFraction) {
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(BarHeight)
            .translateYByBarHeight(translationFraction),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { i ->
            Box(
                Modifier
                    .padding(end = if (i < 3) 3.dp else 0.dp)
                    .size(5.dp)
                    .alpha(0.25f)
                    .background(onSurface, RectangleShape)
            )
        }
    }
}

fun topBarColorFor(position: String, surface: Color, bg: Color) = if (position != "bottom") surface else bg
fun bottomBarColorFor(position: String, surface: Color, bg: Color) = if (position != "top") surface else bg

@Composable
fun AddressBarPreview(position: String, bg: Color, surface: Color, onSurface: Color) {
    Box(
        Modifier
            .size(PreviewWidth, PreviewHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
    ) {
        when (position) {
            "top" -> {
                Box(Modifier.fillMaxWidth().height(BarHeight).align(Alignment.TopCenter).background(surface))
                Pill(onSurface, atTop = true)
                ContentLinesStack(onSurface, topPad = 20.dp, bottomPad = 8.dp)
            }
            "bottom" -> {
                ContentLinesStack(onSurface, topPad = 8.dp, bottomPad = 20.dp)
                Box(Modifier.fillMaxWidth().height(BarHeight).align(Alignment.BottomCenter).background(surface))
                Pill(onSurface, atTop = false)
            }
            else -> {
                Box(Modifier.fillMaxWidth().height(BarHeight).align(Alignment.TopCenter).background(surface))
                Pill(onSurface, atTop = true)
                ContentLinesStack(onSurface, topPad = 20.dp, bottomPad = 20.dp)
                Box(Modifier.fillMaxWidth().height(BarHeight).align(Alignment.BottomCenter).background(surface))
                NavDotsRow(onSurface)
            }
        }
    }
}

fun sanitizeScrollHideMode(mode: String, position: String): String = when (position) {
    "top" -> if (mode == "navigation_bar" || mode == "both") "off" else mode
    "bottom" -> if (mode == "search_bar" || mode == "both") "off" else mode
    else -> mode
}

fun navBarSlotTitleRes(position: String) =
    if (position == "bottom") com.simplefox.browser.R.string.nested_scroll_search_bar
    else com.simplefox.browser.R.string.nested_scroll_nav_bar

fun navBarSlotDescRes(position: String) =
    if (position == "bottom") com.simplefox.browser.R.string.nested_scroll_search_bar_desc
    else com.simplefox.browser.R.string.nested_scroll_nav_bar_desc

fun scrollCardVisible(kind: String, position: String): Boolean = when (kind) {
    "off" -> true
    "search_bar" -> position != "bottom"
    "navigation_bar" -> position != "top"
    "both" -> position == "split"
    else -> true
}

@Composable
fun ScrollHidePreview(
    kind: String,
    addressBarPosition: String,
    bg: Color,
    surface: Color,
    onSurface: Color,
    animate: Boolean
) {
    val topColor = topBarColorFor(addressBarPosition, surface, bg)
    val bottomColor = bottomBarColorFor(addressBarPosition, surface, bg)
    val pillAtTop = addressBarPosition != "bottom"
    val navDotsVisible = addressBarPosition == "split"

    val fraction = if (animate) 1f else 0f
    val f: () -> Float = { fraction }

    val moveTop = kind == "search_bar" || kind == "both"
    val moveBottom = kind == "navigation_bar" || kind == "both"
    val topFraction: () -> Float = if (moveTop) ({ -f() }) else NoFraction
    val bottomFraction: () -> Float = if (moveBottom) f else NoFraction
    val pillFraction = if (pillAtTop) topFraction else bottomFraction
    val dotsFraction = bottomFraction

    Box(
        Modifier
            .size(PreviewWidth, PreviewHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .align(Alignment.TopCenter)
                .translateYByBarHeight(topFraction)
                .background(topColor)
        )
        Pill(onSurface, atTop = pillAtTop, translationFraction = pillFraction)
        ContentLinesStack(onSurface, topPad = 20.dp, bottomPad = 20.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .align(Alignment.BottomCenter)
                .translateYByBarHeight(bottomFraction)
                .background(bottomColor)
        )
        if (navDotsVisible) NavDotsRow(onSurface, translationFraction = dotsFraction)
    }
}

@Composable
fun AccentDot(
    color: Color,
    selected: Boolean,
    ringColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    val scale by animateFloatAsState(targetValue = if (selected) 1.1f else 1f, label = "accentDotScale")
    val checkTint = if (color.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(color)
            .then(if (selected) Modifier.border(2.5.dp, ringColor, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
