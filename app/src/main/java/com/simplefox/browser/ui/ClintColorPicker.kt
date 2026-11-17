package com.simplefox.browser.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import com.simplefox.browser.ui.foundation.OutlinedTextField
import com.simplefox.browser.ui.foundation.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.R
import com.simplefox.browser.ui.theme.LocalClintColors
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private fun colorHexDigits(color: Color): String =
    String.format(Locale.ROOT, "%06X", color.toArgb() and 0xFFFFFF)

private fun hsvColor(hue: Float, saturation: Float, value: Float): Color =
    Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))

@Composable
fun ClintColorPickerDialog(
    initial: Color,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onPicked: (Color) -> Unit
) {
    val colors = LocalClintColors.current
    val initialHsv = remember(initial) { FloatArray(3).also { AndroidColor.colorToHSV(initial.toArgb(), it) } }
    var hue by remember(initial) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initial) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initial) { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember(initial) { mutableStateOf(colorHexDigits(initial)) }
    var exactColor by remember(initial) { mutableStateOf<Color?>(null) }
    val current = exactColor ?: hsvColor(hue, saturation, brightness)

    fun updateFromUser(newHue: Float, newSaturation: Float, newBrightness: Float = brightness) {
        hue = newHue.coerceIn(0f, 360f)
        saturation = newSaturation.coerceIn(0f, 1f)
        brightness = newBrightness.coerceIn(0f, 1f)
        exactColor = null
        hexText = colorHexDigits(hsvColor(hue, saturation, brightness))
    }

    fun onHexChanged(raw: String) {
        val digits = raw.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }.take(6).uppercase(Locale.ROOT)
        hexText = digits
        if (digits.length == 6) {
            val parsed = Color(0xFF000000L or digits.toLong(16))
            val hsv = FloatArray(3).also { AndroidColor.colorToHSV(parsed.toArgb(), it) }
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
            exactColor = parsed
        }
    }

    fun resetToInitial() {
        hue = initialHsv[0]
        saturation = initialHsv[1]
        brightness = initialHsv[2]
        exactColor = initial
        hexText = colorHexDigits(initial)
    }

    ClintPickerDialogShell(
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        onConfirm = {
            onPicked(current)
            onDismiss()
        },
        expandable = true
    ) { wide ->
        val wheel: @Composable () -> Unit = {
            Box(Modifier.widthIn(max = if (wide) 240.dp else 280.dp).fillMaxWidth().aspectRatio(1f).padding(8.dp)) {
                Canvas(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            fun update(point: Offset) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = point.x - center.x
                                val dy = point.y - center.y
                                val radius = min(size.width, size.height) / 2f
                                val distance = sqrt(dx * dx + dy * dy)
                                val angle = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                                updateFromUser(angle, (distance / radius).coerceIn(0f, 1f))
                            }
                            detectTapGestures { update(it) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                val radius = min(size.width, size.height) / 2f
                                val distance = sqrt(dx * dx + dy * dy)
                                val angle = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                                updateFromUser(angle, (distance / radius).coerceIn(0f, 1f))
                            }
                        }
                ) {
                    val radius = min(size.width, size.height) / 2f - 2.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), center = center, radius = radius),
                        radius = radius,
                        center = center
                    )
                    val angle = hue * Math.PI.toFloat() / 180f
                    val markerRadius = saturation * radius
                    val marker = Offset(center.x + cos(angle) * markerRadius, center.y + sin(angle) * markerRadius)
                    drawCircle(Color.White, 8.dp.toPx(), marker, style = Stroke(2.dp.toPx()))
                    drawCircle(Color.Black.copy(alpha = .7f), 8.dp.toPx(), marker, style = Stroke(1.dp.toPx()))
                }
            }
        }
        val controls: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text(stringResource(R.string.color_picker_brightness), color = colors.secondaryText, fontSize = 12.sp)
                Canvas(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).height(36.dp)
                        .pointerInput(hue, saturation) {
                            fun update(x: Float) { updateFromUser(hue, saturation, (x / size.width).coerceIn(0f, 1f)) }
                            detectTapGestures { update(it.x) }
                        }
                        .pointerInput(hue, saturation) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                updateFromUser(hue, saturation, (change.position.x / size.width).coerceIn(0f, 1f))
                            }
                        }
                ) {
                    val y = size.height / 2f
                    val left = 4.dp.toPx()
                    val right = size.width - left
                    val c = hsvColor(hue, saturation, 1f)
                    drawLine(Brush.horizontalGradient(listOf(Color.Black, c)), Offset(left, y), Offset(right, y), strokeWidth = 12.dp.toPx(), cap = StrokeCap.Round)
                    val x = left + (right - left) * brightness
                    drawCircle(Color.White, 9.dp.toPx(), Offset(x, y))
                    drawCircle(colors.primary, 9.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewShape = RoundedCornerShape(12.dp)
                    Row(Modifier.width(72.dp).height(56.dp).clip(previewShape).border(1.dp, colors.divider, previewShape)) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(initial).clickable { resetToInitial() })
                        Box(Modifier.weight(1f).fillMaxHeight().background(current))
                    }
                    OutlinedTextField(
                        state = rememberSyncedTextFieldState(
                            value = hexText,
                            onValueChange = { value -> onHexChanged(value) }
                        ),
                        modifier = Modifier.weight(1f),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        label = { Text(stringResource(R.string.color_picker_hex_label)) },
                        prefix = { Text("#") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }
        }
        PickerTitle(stringResource(R.string.color_picker_select_color))
        if (wide) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { wheel() }
                Box(Modifier.weight(1f)) { controls() }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) { wheel() }
            controls()
        }
        Spacer(Modifier.height(8.dp))
    }
}
