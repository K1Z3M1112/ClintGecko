package com.simplefox.browser.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Like imePadding(), but pads by the keyboard's final height instead of its height on every animation frame.
 * The content is laid out twice per open/close instead of once per frame while the keyboard moves.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.imeTargetPadding(): Modifier = windowInsetsPadding(WindowInsets.imeAnimationTarget)
