package com.simplefox.browser.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.SolidColor
import com.simplefox.browser.ui.theme.LocalClintColors
import kotlin.math.round

val LocalTextStyle = staticCompositionLocalOf { TextStyle.Default }
private val LocalCustomContentColor = compositionLocalOf { Color.Unspecified }

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val themeColor = LocalCustomContentColor.current.let { if (it != Color.Unspecified) it else LocalClintColors.current.onSurface }
    val resolvedStyle = style.merge(
        TextStyle(
            color = if (color != Color.Unspecified) color else themeColor,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight
        )
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = resolvedStyle,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow,
        onTextLayout = onTextLayout
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val themeColor = LocalCustomContentColor.current.let { if (it != Color.Unspecified) it else LocalClintColors.current.onSurface }
    val resolvedStyle = style.merge(
        TextStyle(
            color = if (color != Color.Unspecified) color else themeColor,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight
        )
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = resolvedStyle,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow,
        onTextLayout = onTextLayout
    )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalCustomContentColor.current.takeIf { it != Color.Unspecified } ?: LocalClintColors.current.iconTint
) {
    val painter = rememberVectorPainter(imageVector)
    Image(painter = painter, contentDescription = contentDescription, modifier = modifier, colorFilter = ColorFilter.tint(tint))
}

@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalCustomContentColor.current.takeIf { it != Color.Unspecified } ?: LocalClintColors.current.iconTint
) {
    Image(painter = painter, contentDescription = contentDescription, modifier = modifier, colorFilter = ColorFilter.tint(tint))
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    color: Color = LocalClintColors.current.cardBackground,
    contentColor: Color = LocalClintColors.current.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val surfaceModifier = modifier
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
        .then(if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, shape, clip = false) else Modifier)
        .clip(shape)
        .background(color)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
    CompositionLocalProvider(LocalCustomContentColor provides contentColor) {
        Box(surfaceModifier, content = { content() })
    }
}

class ButtonColors internal constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color
) {
    fun containerColor(enabled: Boolean) = if (enabled) containerColor else disabledContainerColor
    fun contentColor(enabled: Boolean) = if (enabled) contentColor else disabledContentColor
}

object ButtonDefaults {
    val TextButtonContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    @Composable fun buttonColors(
        containerColor: Color = LocalClintColors.current.buttonBackground,
        contentColor: Color = LocalClintColors.current.buttonTextColor,
        disabledContainerColor: Color = LocalClintColors.current.buttonBackground.copy(alpha = 0.12f),
        disabledContentColor: Color = LocalClintColors.current.secondaryText.copy(alpha = 0.7f)
    ) = ButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(22.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val bg = colors.containerColor(enabled)
    val fg = colors.contentColor(enabled)
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = bg,
        contentColor = fg,
        border = border
    ) {
        Row(
            Modifier.defaultMinSize(minHeight = 40.dp).padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalCustomContentColor provides LocalClintColors.current.primary) { content() }
    }
}

@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalCustomContentColor provides LocalClintColors.current.iconTint) { content() }
    }
}

class CardColors internal constructor(val containerColor: Color, val contentColor: Color, val disabledContainerColor: Color, val disabledContentColor: Color)
class CardElevation internal constructor(val defaultElevation: Dp)
object CardDefaults {
    @Composable fun cardColors(
        containerColor: Color = LocalClintColors.current.cardBackground,
        contentColor: Color = LocalClintColors.current.onSurface,
        disabledContainerColor: Color = containerColor,
        disabledContentColor: Color = contentColor
    ) = CardColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)
    fun cardElevation(defaultElevation: Dp = 0.dp) = CardElevation(defaultElevation)
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier),
        shape = shape,
        color = if (enabled) colors.containerColor else colors.disabledContainerColor,
        contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
        shadowElevation = elevation.defaultElevation,
        border = border
    ) { Column(content = content) }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp, color: Color = LocalClintColors.current.divider) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

@Composable
fun LinearProgressIndicator(
    progress: () -> Float = { Float.NaN },
    modifier: Modifier = Modifier,
    color: Color = LocalClintColors.current.primary,
    trackColor: Color = LocalClintColors.current.surfaceVariant,
    strokeCap: StrokeCap = StrokeCap.Round
) {
    val current = progress()
    if (current.isNaN()) {
        IndeterminateLinearProgressIndicator(modifier, color, trackColor, strokeCap)
    } else {
        Canvas(modifier.height(4.dp)) {
            val h = size.height
            val centerY = h / 2f
            val startX = if (strokeCap == StrokeCap.Round) centerY else 0f
            val endX = if (strokeCap == StrokeCap.Round) size.width - centerY else size.width
            if (endX > startX) {
                drawLine(trackColor, Offset(startX, centerY), Offset(endX, centerY), h, cap = strokeCap)
                val fraction = current.coerceIn(0f, 1f)
                val progressX = startX + (endX - startX) * fraction
                if (fraction > 0f) drawLine(color, Offset(startX, centerY), Offset(progressX, centerY), h, cap = strokeCap)
            }
        }
    }
}

@Composable
private fun IndeterminateLinearProgressIndicator(
    modifier: Modifier,
    color: Color,
    trackColor: Color,
    strokeCap: StrokeCap
) {
    val transition = rememberInfiniteTransition(label = "customLinearProgress")
    val animatedStart by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "indeterminateStart"
    )
    Canvas(modifier.height(4.dp)) {
        val h = size.height
        val centerY = h / 2f
        val chunkWidth = size.width * 0.28f
        val start = (animatedStart * (size.width + chunkWidth)).coerceIn(-chunkWidth, size.width)
        val end = (start + chunkWidth).coerceIn(0f, size.width)
        val trackStart = if (strokeCap == StrokeCap.Round) centerY else 0f
        val trackEnd = if (strokeCap == StrokeCap.Round) size.width - centerY else size.width
        if (trackEnd > trackStart) {
            drawLine(trackColor, Offset(trackStart, centerY), Offset(trackEnd, centerY), h, cap = strokeCap)
        }
        if (end > start) {
            drawLine(color, Offset(start, centerY), Offset(end, centerY), h, cap = strokeCap)
        }
    }
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = LocalClintColors.current.primary,
    trackColor: Color = Color.Transparent,
    strokeWidth: Dp = 4.dp
) {
    val transition = rememberInfiniteTransition(label = "customCircularProgress")
    val startAngle by transition.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "circularStartAngle"
    )
    val sweepAngle by transition.animateFloat(
        initialValue = 80f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "circularSweepAngle"
    )
    Canvas(modifier.size(40.dp)) {
        val sw = strokeWidth.toPx()
        val pad = sw / 2f
        val arcTopLeft = Offset(pad, pad)
        val arcSize = Size(size.width - sw, size.height - sw)
        if (trackColor != Color.Transparent) {
            drawArc(trackColor, 0f, 360f, false, arcTopLeft, arcSize, style = Stroke(sw))
        }
        drawArc(color, startAngle, sweepAngle, false, arcTopLeft, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = LocalClintColors.current.popupBackground,
    border: BorderStroke? = null,
    shadowElevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    if (expanded) {
        val density = LocalDensity.current
        val offsetPx = with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }
        val positionProvider = remember(offsetPx) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val preferredX = if (layoutDirection == LayoutDirection.Ltr) {
                        anchorBounds.right - popupContentSize.width
                    } else {
                        anchorBounds.left
                    }
                    val below = anchorBounds.bottom
                    val preferredY = if (below + popupContentSize.height <= windowSize.height) {
                        below
                    } else {
                        anchorBounds.top - popupContentSize.height
                    }
                    val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                    val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
                    return IntOffset(
                        (preferredX + offsetPx.x).coerceIn(0, maxX),
                        (preferredY + offsetPx.y).coerceIn(0, maxY)
                    )
                }
            }
        }
        Popup(popupPositionProvider = positionProvider, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
            Surface(
                modifier = modifier,
                shape = shape,
                color = containerColor,
                shadowElevation = shadowElevation,
                border = border
            ) {
                CompositionLocalProvider(LocalCustomContentColor provides LocalClintColors.current.popupText) {
                    Column(Modifier.widthIn(min = 180.dp).padding(vertical = 6.dp), content = content)
                }
            }
        }
    }
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leadingIcon != null) leadingIcon()
        Box(Modifier.weight(1f)) { text() }
        if (trailingIcon != null) trailingIcon()
    }
}

@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalClintColors.current.primary,
    contentColor: Color = LocalClintColors.current.onPrimary,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick).defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 0.dp
    ) { Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) { content() } }
}

@Composable
fun ExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    containerColor: Color = LocalClintColors.current.primary,
    contentColor: Color = LocalClintColors.current.onPrimary,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = shape, color = containerColor, contentColor = contentColor) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon()
            if (expanded) text()
        }
    }
}

class CheckboxColors internal constructor(val checkedColor: Color, val uncheckedColor: Color, val checkmarkColor: Color, val disabledCheckedColor: Color, val disabledUncheckedColor: Color)
object CheckboxDefaults {
    @Composable fun colors(
        checkedColor: Color = LocalClintColors.current.primary,
        uncheckedColor: Color = LocalClintColors.current.secondaryText,
        checkmarkColor: Color = LocalClintColors.current.onPrimary,
        disabledCheckedColor: Color = checkedColor.copy(alpha = .38f),
        disabledUncheckedColor: Color = uncheckedColor.copy(alpha = .38f),
        disabledIndeterminateColor: Color = checkedColor.copy(alpha = .38f)
    ) = CheckboxColors(checkedColor, uncheckedColor, checkmarkColor, disabledCheckedColor, disabledUncheckedColor)
}

@Composable
fun Checkbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true, colors: CheckboxColors = CheckboxDefaults.colors()) {
    val activeColor = if (enabled) colors.checkedColor else colors.disabledCheckedColor
    val inactiveColor = if (enabled) colors.uncheckedColor else colors.disabledUncheckedColor
    Canvas(
        modifier.size(24.dp)
            .semantics { role = Role.Checkbox; toggleableState = if (checked) ToggleableState.On else ToggleableState.Off }
            .clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) }
    ) {
        val s = size.minDimension
        val side = s * .72f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val shape = androidx.compose.ui.geometry.CornerRadius(s * .16f)
        if (checked) {
            drawRoundRect(activeColor, Offset(left, top), Size(side, side), shape)
            val p = Path().apply {
                moveTo(left + side * .22f, top + side * .52f)
                lineTo(left + side * .43f, top + side * .71f)
                lineTo(left + side * .79f, top + side * .30f)
            }
            drawPath(p, colors.checkmarkColor.copy(alpha = if (enabled) 1f else .38f), style = Stroke(s * .09f, cap = StrokeCap.Round))
        } else {
            drawRoundRect(inactiveColor.copy(alpha = .2f), Offset(left, top), Size(side, side), shape)
            drawRoundRect(inactiveColor, Offset(left, top), Size(side, side), shape, style = Stroke(s * .075f))
        }
    }
}

class RadioButtonColors internal constructor(val selectedColor: Color, val unselectedColor: Color, val disabledSelectedColor: Color, val disabledUnselectedColor: Color)
object RadioButtonDefaults {
    @Composable fun colors(
        selectedColor: Color = LocalClintColors.current.primary,
        unselectedColor: Color = LocalClintColors.current.secondaryText,
        disabledSelectedColor: Color = selectedColor.copy(alpha = .38f),
        disabledUnselectedColor: Color = unselectedColor.copy(alpha = .38f)
    ) = RadioButtonColors(selectedColor, unselectedColor, disabledSelectedColor, disabledUnselectedColor)
}

@Composable
fun RadioButton(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true, colors: RadioButtonColors = RadioButtonDefaults.colors()) {
    val activeColor = if (enabled) colors.selectedColor else colors.disabledSelectedColor
    val inactiveColor = if (enabled) colors.unselectedColor else colors.disabledUnselectedColor
    Canvas(
        modifier.size(24.dp)
            .semantics { role = Role.RadioButton; this.selected = selected }
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
    ) {
        val radius = size.minDimension * .36f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(if (selected) activeColor else inactiveColor, radius, center, style = Stroke(size.minDimension * .08f))
        if (selected) drawCircle(activeColor, radius * .52f, center)
    }
}

class SwitchColors internal constructor(
    val checkedTrackColor: Color,
    val uncheckedTrackColor: Color,
    val checkedThumbColor: Color,
    val uncheckedThumbColor: Color,
    val checkedIconColor: Color,
    val uncheckedIconColor: Color,
    val checkedBorderColor: Color,
    val uncheckedBorderColor: Color,
    val disabledCheckedTrackColor: Color,
    val disabledUncheckedTrackColor: Color,
    val disabledCheckedThumbColor: Color,
    val disabledUncheckedThumbColor: Color,
    val disabledCheckedIconColor: Color,
    val disabledUncheckedIconColor: Color,
    val disabledCheckedBorderColor: Color,
    val disabledUncheckedBorderColor: Color
)
object SwitchDefaults {
    val IconSize = 16.dp
    @Composable fun colors(
        checkedThumbColor: Color = LocalClintColors.current.onPrimary,
        checkedTrackColor: Color = LocalClintColors.current.primary,
        checkedBorderColor: Color = Color.Transparent,
        checkedIconColor: Color = LocalClintColors.current.primary,
        uncheckedThumbColor: Color = LocalClintColors.current.secondaryText,
        uncheckedTrackColor: Color = LocalClintColors.current.surfaceVariant,
        uncheckedBorderColor: Color = LocalClintColors.current.divider,
        uncheckedIconColor: Color = LocalClintColors.current.surfaceVariant,
        disabledCheckedThumbColor: Color = checkedThumbColor.copy(alpha = .38f),
        disabledCheckedTrackColor: Color = checkedTrackColor.copy(alpha = .38f),
        disabledCheckedBorderColor: Color = checkedBorderColor.copy(alpha = .38f),
        disabledCheckedIconColor: Color = checkedIconColor.copy(alpha = .38f),
        disabledUncheckedThumbColor: Color = uncheckedThumbColor.copy(alpha = .38f),
        disabledUncheckedTrackColor: Color = uncheckedTrackColor.copy(alpha = .38f),
        disabledUncheckedBorderColor: Color = uncheckedBorderColor.copy(alpha = .38f),
        disabledUncheckedIconColor: Color = uncheckedIconColor.copy(alpha = .38f)
    ) = SwitchColors(
        checkedTrackColor, uncheckedTrackColor, checkedThumbColor, uncheckedThumbColor,
        checkedIconColor, uncheckedIconColor, checkedBorderColor, uncheckedBorderColor,
        disabledCheckedTrackColor, disabledUncheckedTrackColor,
        disabledCheckedThumbColor, disabledUncheckedThumbColor,
        disabledCheckedIconColor, disabledUncheckedIconColor,
        disabledCheckedBorderColor, disabledUncheckedBorderColor
    )
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = null,
    colors: SwitchColors = SwitchDefaults.colors()
) {
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .semantics { role = Role.Switch; toggleableState = if (checked) ToggleableState.On else ToggleableState.Off }
            .clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val h = size.height
            val trackW = size.width * .82f
            val trackH = h * .58f
            val left = (size.width - trackW) / 2f
            val top = (h - trackH) / 2f
            val radius = trackH / 2f
            val track = when {
                enabled && checked -> colors.checkedTrackColor
                enabled -> colors.uncheckedTrackColor
                checked -> colors.disabledCheckedTrackColor
                else -> colors.disabledUncheckedTrackColor
            }
            val borderColor = when {
                enabled && checked -> colors.checkedBorderColor
                enabled -> colors.uncheckedBorderColor
                checked -> colors.disabledCheckedBorderColor
                else -> colors.disabledUncheckedBorderColor
            }
            drawRoundRect(track, Offset(left, top), Size(trackW, trackH), androidx.compose.ui.geometry.CornerRadius(radius))
            if (borderColor.alpha > 0f) {
                drawRoundRect(borderColor, Offset(left, top), Size(trackW, trackH), androidx.compose.ui.geometry.CornerRadius(radius), style = Stroke(1.dp.toPx()))
            }
            val thumbRadius = h * .35f
            val thumbX = if (checked) left + trackW - thumbRadius - 3.dp.toPx() else left + thumbRadius + 3.dp.toPx()
            val thumbColor = when {
                enabled && checked -> colors.checkedThumbColor
                enabled -> colors.uncheckedThumbColor
                checked -> colors.disabledCheckedThumbColor
                else -> colors.disabledUncheckedThumbColor
            }
            drawCircle(thumbColor, thumbRadius, Offset(thumbX, h / 2f))
        }
        if (thumbContent != null) {
            Box(
                Modifier.padding(horizontal = 10.dp).size(SwitchDefaults.IconSize),
                contentAlignment = Alignment.Center
            ) {
                val iconColor = when {
                    enabled && checked -> colors.checkedIconColor
                    enabled -> colors.uncheckedIconColor
                    checked -> colors.disabledCheckedIconColor
                    else -> colors.disabledUncheckedIconColor
                }
                CompositionLocalProvider(LocalCustomContentColor provides iconColor) { thumbContent() }
            }
        }
    }
}

class SliderColors internal constructor(
    val thumbColor: Color,
    val activeTrackColor: Color,
    val inactiveTrackColor: Color,
    val disabledThumbColor: Color,
    val disabledActiveTrackColor: Color,
    val disabledInactiveTrackColor: Color,
    val activeTickColor: Color,
    val inactiveTickColor: Color,
    val disabledActiveTickColor: Color,
    val disabledInactiveTickColor: Color
)
object SliderDefaults {
    @Composable fun colors(
        thumbColor: Color = LocalClintColors.current.primary,
        activeTrackColor: Color = LocalClintColors.current.primary,
        activeTickColor: Color = Color.White,
        inactiveTrackColor: Color = LocalClintColors.current.surfaceVariant,
        inactiveTickColor: Color = LocalClintColors.current.secondaryText,
        disabledThumbColor: Color = thumbColor.copy(alpha = .38f),
        disabledActiveTrackColor: Color = activeTrackColor.copy(alpha = .38f),
        disabledActiveTickColor: Color = activeTickColor.copy(alpha = .38f),
        disabledInactiveTrackColor: Color = inactiveTrackColor.copy(alpha = .38f),
        disabledInactiveTickColor: Color = inactiveTickColor.copy(alpha = .38f)
    ) = SliderColors(
        thumbColor, activeTrackColor, inactiveTrackColor,
        disabledThumbColor, disabledActiveTrackColor, disabledInactiveTrackColor,
        activeTickColor, inactiveTickColor, disabledActiveTickColor, disabledInactiveTickColor
    )
}

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val currentChange by rememberUpdatedState(onValueChange)
    val currentFinished by rememberUpdatedState(onValueChangeFinished)
    val low = valueRange.start
    val high = valueRange.endInclusive
    val fraction = if (high > low) ((value - low) / (high - low)).coerceIn(0f, 1f) else 0f
    fun valueAt(rawFraction: Float): Float {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val snapped = if (steps > 0) round(clamped * (steps + 1)) / (steps + 1).toFloat() else clamped
        return low + (high - low) * snapped
    }
    Canvas(
        modifier.height(40.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps) }
            .pointerInput(enabled, valueRange, steps) {
                if (enabled) detectTapGestures { point ->
                    val f = (point.x / size.width).coerceIn(0f, 1f)
                    currentChange(valueAt(f))
                    currentFinished?.invoke()
                }
            }
            .pointerInput(enabled, valueRange, steps) {
                if (enabled) detectDragGestures(
                    onDragEnd = { currentFinished?.invoke() },
                    onDrag = { change, _ ->
                        change.consume()
                        val f = (change.position.x / size.width).coerceIn(0f, 1f)
                        currentChange(valueAt(f))
                    }
                )
            }
    ) {
        val y = size.height / 2f
        val startX = 2.dp.toPx()
        val endX = size.width - 2.dp.toPx()
        val activeTrack = if (enabled) colors.activeTrackColor else colors.disabledActiveTrackColor
        val inactiveTrack = if (enabled) colors.inactiveTrackColor else colors.disabledInactiveTrackColor
        val thumb = if (enabled) colors.thumbColor else colors.disabledThumbColor
        drawLine(inactiveTrack, Offset(startX, y), Offset(endX, y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        val thumbX = startX + (endX - startX) * fraction
        drawLine(activeTrack, Offset(startX, y), Offset(thumbX, y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        if (steps > 0) {
            val tickCount = steps + 2
            repeat(tickCount) { index ->
                val position = index.toFloat() / (tickCount - 1)
                val tickX = startX + (endX - startX) * position
                val tickColor = when {
                    enabled && position <= fraction -> colors.activeTickColor
                    enabled -> colors.inactiveTickColor
                    position <= fraction -> colors.disabledActiveTickColor
                    else -> colors.disabledInactiveTickColor
                }
                drawLine(tickColor, Offset(tickX, y - 2.dp.toPx()), Offset(tickX, y + 2.dp.toPx()), strokeWidth = 1.dp.toPx())
            }
        }
        drawCircle(thumb, 7.dp.toPx(), Offset(thumbX, y))
    }
}

@Composable
fun OutlinedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.MultiLine(1, Int.MAX_VALUE),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    colors: TextFieldColors = TextFieldColors()
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when { isError -> LocalClintColors.current.colorError; focused -> LocalClintColors.current.primary; else -> LocalClintColors.current.divider }
    Column(modifier) {
        Column(
            Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (label != null) Box(Modifier.padding(bottom = 4.dp)) { CompositionLocalProvider(LocalCustomContentColor provides LocalClintColors.current.secondaryText) { label() } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prefix != null) prefix()
                BasicTextField(
                    state = state,
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                    enabled = enabled,
                    readOnly = readOnly,
                    lineLimits = lineLimits,
                    keyboardOptions = keyboardOptions,
                    textStyle = TextStyle(color = LocalClintColors.current.onSurface, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(LocalClintColors.current.primary)
                )
                if (suffix != null) suffix()
                if (trailingIcon != null) trailingIcon()
            }
        }
        if (supportingText != null) Box(Modifier.padding(start = 12.dp, top = 4.dp)) { supportingText() }
    }
}

@Composable
fun OutlinedSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.Hidden,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    colors: TextFieldColors = TextFieldColors()
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (isError) LocalClintColors.current.colorError else if (focused) LocalClintColors.current.primary else LocalClintColors.current.divider
    Column(modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (label != null) Box(Modifier.padding(bottom = 4.dp)) { label() }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicSecureTextField(
                state = state,
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                enabled = enabled,
                keyboardOptions = keyboardOptions,
                textStyle = TextStyle(color = LocalClintColors.current.onSurface, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(LocalClintColors.current.primary),
                textObfuscationMode = textObfuscationMode
            )
            if (trailingIcon != null) trailingIcon()
        }
    }
}

class TextFieldColors
object OutlinedTextFieldDefaults {
    @Composable fun colors(
        focusedTextColor: Color = Color.Unspecified,
        unfocusedTextColor: Color = Color.Unspecified,
        disabledTextColor: Color = Color.Unspecified,
        errorTextColor: Color = Color.Unspecified,
        focusedBorderColor: Color = Color.Unspecified,
        unfocusedBorderColor: Color = Color.Unspecified,
        disabledBorderColor: Color = Color.Unspecified,
        errorBorderColor: Color = Color.Unspecified,
        cursorColor: Color = Color.Unspecified,
        errorCursorColor: Color = Color.Unspecified,
        focusedLabelColor: Color = Color.Unspecified,
        unfocusedLabelColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        errorLabelColor: Color = Color.Unspecified,
        focusedTrailingIconColor: Color = Color.Unspecified,
        unfocusedTrailingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
        errorTrailingIconColor: Color = Color.Unspecified,
        dateTextFieldColors: TextFieldColors = TextFieldColors()
    ) = TextFieldColors()
}

enum class SheetValue { Hidden, PartiallyExpanded, Expanded }
class ModalBottomSheetState internal constructor(val skipPartiallyExpanded: Boolean, internal val confirmValueChange: (SheetValue) -> Boolean)
@Composable
fun rememberModalBottomSheetState(skipPartiallyExpanded: Boolean = false, confirmValueChange: (SheetValue) -> Boolean = { true }): ModalBottomSheetState = remember(skipPartiallyExpanded, confirmValueChange) { ModalBottomSheetState(skipPartiallyExpanded, confirmValueChange) }

object BottomSheetDefaults {
    @Composable fun DragHandle(color: Color = LocalClintColors.current.divider) {
        Box(Modifier.padding(vertical = 10.dp).size(width = 32.dp, height = 4.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = 640.dp,
    shape: Shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    containerColor: Color = LocalClintColors.current.popupBackground,
    contentColor: Color = LocalClintColors.current.onSurface,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = Color.Black.copy(alpha = .48f),
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxSize().background(scrimColor).clickable {
                if (sheetState.confirmValueChange(SheetValue.Hidden)) onDismissRequest()
            })
            Surface(
                modifier = modifier.fillMaxWidth().widthIn(max = sheetMaxWidth).heightIn(max = 720.dp),
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                shadowElevation = 0.dp
            ) {
                Column {
                    if (dragHandle != null) {
                        var draggedDistance = 0f
                        Box(
                            Modifier.fillMaxWidth().pointerInput(sheetState, onDismissRequest) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        draggedDistance = (draggedDistance + dragAmount).coerceAtLeast(0f)
                                    },
                                    onDragEnd = {
                                        if (draggedDistance > 72.dp.toPx() && sheetState.confirmValueChange(SheetValue.Hidden)) {
                                            onDismissRequest()
                                        }
                                        draggedDistance = 0f
                                    },
                                    onDragCancel = { draggedDistance = 0f }
                                )
                            },
                            contentAlignment = Alignment.Center
                        ) { dragHandle() }
                    }
                    Column(content = content)
                }
            }
        }
    }
}

class PullToRefreshState internal constructor() {
    var distance by mutableFloatStateOf(0f)
}
@Composable fun rememberPullToRefreshState() = remember { PullToRefreshState() }
object PullToRefreshDefaults {
    @Composable fun Indicator(state: PullToRefreshState, isRefreshing: Boolean, modifier: Modifier = Modifier, containerColor: Color = LocalClintColors.current.addressBarColor, color: Color = LocalClintColors.current.primary) {
        if (isRefreshing || state.distance > 0.2f) {
            Box(modifier.padding(top = 8.dp).size(40.dp).clip(CircleShape).background(containerColor), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = color, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    indicator: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val latestRefresh by rememberUpdatedState(onRefresh)
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }
    val nestedConnection = remember(state, isRefreshing, thresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!isRefreshing && available.y > 0f) {
                    state.distance = (state.distance + available.y / thresholdPx).coerceIn(0f, 1.5f)
                } else if (available.y < 0f) {
                    state.distance = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!isRefreshing && state.distance >= 1f) latestRefresh()
                state.distance = 0f
                return Velocity.Zero
            }
        }
    }
    Box(modifier.nestedScroll(nestedConnection)) {
        content()
        indicator()
    }
}
