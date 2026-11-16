package com.simplefox.browser.ui

import androidx.compose.runtime.Composable

interface OverlayHostActivity {
    var overlayContent: (@Composable () -> Unit)?
}
