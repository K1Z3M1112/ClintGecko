package com.simplefox.browser.ui.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

interface SnackbarVisuals {
    val message: String
    val actionLabel: String?
    val withDismissAction: Boolean
    val duration: SnackbarDuration
}

enum class SnackbarDuration { Short, Long, Indefinite }
enum class SnackbarResult { Dismissed, ActionPerformed }

class SnackbarData internal constructor(
    val visuals: SnackbarVisuals,
    private val completion: CompletableDeferred<SnackbarResult>
) {
    fun performAction() { if (!completion.isCompleted) completion.complete(SnackbarResult.ActionPerformed) }
    fun dismiss() { if (!completion.isCompleted) completion.complete(SnackbarResult.Dismissed) }
}

class SnackbarHostState {
    var currentSnackbarData by mutableStateOf<SnackbarData?>(null)
        private set

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult = showSnackbar(object : SnackbarVisuals {
        override val message = message
        override val actionLabel = actionLabel
        override val withDismissAction = withDismissAction
        override val duration = duration
    })

    suspend fun showSnackbar(visuals: SnackbarVisuals): SnackbarResult {
        currentSnackbarData?.dismiss()
        val completion = CompletableDeferred<SnackbarResult>()
        val data = SnackbarData(visuals, completion)
        currentSnackbarData = data
        return try {
            val timeout = when (visuals.duration) {
                SnackbarDuration.Short -> 2_000L
                SnackbarDuration.Long -> 4_000L
                SnackbarDuration.Indefinite -> null
            }
            if (timeout == null) completion.await()
            else withTimeoutOrNull(timeout) { completion.await() } ?: SnackbarResult.Dismissed
        } finally {
            if (currentSnackbarData === data) currentSnackbarData = null
        }
    }
}

@Composable
fun SnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier, snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(snackbarData = it) }) {
    val current = hostState.currentSnackbarData
    if (current != null) {
        androidx.compose.foundation.layout.Box(modifier) { snackbar(current) }
    }
}

@Composable
fun Snackbar(
    snackbarData: SnackbarData? = null,
    action: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = Color(0xFF303030),
    contentColor: Color = Color.White,
    actionColor: Color = Color(0xFFB0C8FF),
    content: (@Composable () -> Unit)? = null
) {
    Surface(shape = shape, color = containerColor, contentColor = contentColor) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                if (content != null) content()
                else Text(snackbarData?.visuals?.message.orEmpty())
            }
            if (action != null) action()
            else if (snackbarData?.visuals?.actionLabel != null) {
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(snackbarData.visuals.actionLabel ?: "", color = actionColor)
                }
            }
        }
    }
}
