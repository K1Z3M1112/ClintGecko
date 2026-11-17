package com.simplefox.browser.browser.webview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Self-contained pull-to-refresh container (no androidx.swiperefreshlayout).
 * Hosts a single scrollable target and draws a circular progress indicator above it.
 */
open class SimplePullRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    fun interface OnRefreshListener {
        fun onRefresh()
    }

    private val density = resources.displayMetrics.density
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop
    private val circleSize = (CIRCLE_DIAMETER_DP * density).toInt()
    private val circle = ProgressCircle(context, density)

    private var target: View? = null
    private var listener: OnRefreshListener? = null
    private var refreshing = false
    private var notifyOnRefresh = false
    private var returningToStart = false

    private var totalDragDistance = (DEFAULT_TRIGGER_DP * density)
    private var originalOffsetTop = -circleSize
    private var currentTargetOffsetTop = -circleSize
    private var spinnerOffsetEnd = (DEFAULT_END_DP * density).toInt()

    private var initialDownY = 0f
    private var initialMotionY = 0f
    private var isBeingDragged = false
    private var activePointerId = INVALID_POINTER

    private var nestedScrollInProgress = false
    private var totalUnconsumed = 0f

    private var offsetAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null

    init {
        setChildrenDrawingOrderEnabled(true)
        circle.visibility = GONE
        addView(circle, LayoutParams(circleSize, circleSize))
    }

    // ---- public API ------------------------------------------------------------------------

    fun setOnRefreshListener(l: OnRefreshListener?) {
        listener = l
    }

    var isRefreshing: Boolean
        get() = refreshing
        set(value) {
            if (refreshing == value) return
            refreshing = value
            notifyOnRefresh = false
            if (value) {
                offsetAnimator?.cancel()
                scaleAnimator?.cancel()
                circle.visibility = VISIBLE
                circle.alpha = 1f
                setTargetOffsetTopAndBottom(spinnerOffsetEnd - currentTargetOffsetTop)
                circle.startSpinning()
                animateScale(0f, 1f, 150L, null)
            } else {
                offsetAnimator?.cancel()
                animateScale(1f, 0f, 150L) { reset() }
            }
        }

    fun setProgressBackgroundColorSchemeColor(color: Int) {
        circle.setBackgroundFill(color)
    }

    fun setColorSchemeColors(vararg colors: Int) {
        if (colors.isNotEmpty()) circle.setArcColors(colors)
    }

    fun setDistanceToTriggerSync(distance: Int) {
        totalDragDistance = distance.toFloat()
    }

    @Suppress("UNUSED_PARAMETER")
    fun setProgressViewOffset(scale: Boolean, start: Int, end: Int) {
        originalOffsetTop = start
        currentTargetOffsetTop = start
        spinnerOffsetEnd = end
        refreshing = false
        notifyOnRefresh = false
        reset()
        requestLayout()
    }

    open fun canChildScrollUp(): Boolean {
        val t = target ?: return false
        return t.canScrollVertically(-1)
    }

    // ---- layout ----------------------------------------------------------------------------

    private fun ensureTarget() {
        if (target != null) return
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child !== circle) {
                target = child
                break
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        ensureTarget()
        val t = target ?: return
        t.measure(
            MeasureSpec.makeMeasureSpec(measuredWidth - paddingLeft - paddingRight, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight - paddingTop - paddingBottom, MeasureSpec.EXACTLY)
        )
        circle.measure(
            MeasureSpec.makeMeasureSpec(circleSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(circleSize, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = measuredWidth
        val height = measuredHeight
        if (childCount == 0) return
        ensureTarget()
        val child = target ?: return
        val childLeft = paddingLeft
        val childTop = paddingTop
        child.layout(
            childLeft,
            childTop,
            childLeft + width - paddingLeft - paddingRight,
            childTop + height - paddingTop - paddingBottom
        )
        circle.layout(
            width / 2 - circleSize / 2,
            currentTargetOffsetTop,
            width / 2 + circleSize / 2,
            currentTargetOffsetTop + circleSize
        )
    }

    override fun getChildDrawingOrder(childCount: Int, i: Int): Int {
        val circleIndex = indexOfChild(circle)
        if (circleIndex < 0) return i
        return when {
            i == childCount - 1 -> circleIndex
            i >= circleIndex -> i + 1
            else -> i
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        refreshing = false
        notifyOnRefresh = false
        isBeingDragged = false
        nestedScrollInProgress = false
        totalUnconsumed = 0f
        reset()
    }

    // ---- touch -----------------------------------------------------------------------------

    private fun gestureBlocked(): Boolean =
        !isEnabled || returningToStart || canChildScrollUp() || refreshing || nestedScrollInProgress

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        ensureTarget()
        val action = ev.actionMasked
        if (returningToStart && action == MotionEvent.ACTION_DOWN) returningToStart = false
        if (gestureBlocked()) return false

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                isBeingDragged = false
                val y = pointerY(ev, activePointerId)
                if (y == INVALID_Y) return false
                initialDownY = y
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == INVALID_POINTER) return false
                val y = pointerY(ev, activePointerId)
                if (y == INVALID_Y) return false
                startDragging(y)
            }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
                activePointerId = INVALID_POINTER
            }
        }
        return isBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        if (returningToStart && action == MotionEvent.ACTION_DOWN) returningToStart = false
        if (gestureBlocked()) return false

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                isBeingDragged = false
            }
            MotionEvent.ACTION_MOVE -> {
                val y = pointerY(ev, activePointerId)
                if (y == INVALID_Y) return false
                if (isBeingDragged) {
                    val overscroll = (y - initialMotionY) * DRAG_RATE
                    if (overscroll > 0) moveSpinner(overscroll) else return false
                } else {
                    startDragging(y)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                if (index < 0) return false
                activePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP -> {
                if (activePointerId == INVALID_POINTER) return false
                val y = pointerY(ev, activePointerId)
                if (isBeingDragged && y != INVALID_Y) {
                    val overscroll = (y - initialMotionY) * DRAG_RATE
                    isBeingDragged = false
                    finishSpinner(overscroll)
                }
                activePointerId = INVALID_POINTER
                return false
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isBeingDragged) {
                    isBeingDragged = false
                    finishSpinner(0f)
                }
                activePointerId = INVALID_POINTER
                return false
            }
        }
        return true
    }

    private fun pointerY(ev: MotionEvent, pointerId: Int): Float {
        val index = ev.findPointerIndex(pointerId)
        return if (index < 0) INVALID_Y else ev.getY(index)
    }

    private fun startDragging(y: Float) {
        val dy = y - initialDownY
        if (dy > touchSlopPx && !isBeingDragged) {
            initialMotionY = initialDownY + touchSlopPx
            isBeingDragged = true
        }
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        if (ev.getPointerId(pointerIndex) == activePointerId) {
            val newIndex = if (pointerIndex == 0) 1 else 0
            if (newIndex < ev.pointerCount) activePointerId = ev.getPointerId(newIndex)
        }
    }

    // ---- nested scrolling (platform protocol) ----------------------------------------------

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean =
        isEnabled && !returningToStart && !refreshing &&
            (nestedScrollAxes and SCROLL_AXIS_VERTICAL) != 0

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        super.onNestedScrollAccepted(child, target, axes)
        totalUnconsumed = 0f
        nestedScrollInProgress = true
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (dy > 0 && totalUnconsumed > 0) {
            if (dy > totalUnconsumed) {
                consumed[1] = dy - totalUnconsumed.toInt()
                totalUnconsumed = 0f
            } else {
                totalUnconsumed -= dy
                consumed[1] = dy
            }
            moveSpinner(totalUnconsumed)
        }
        super.onNestedPreScroll(target, dx, dy, consumed)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int
    ) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)
        if (refreshing) return
        if (dyUnconsumed < 0 && !canChildScrollUp()) {
            totalUnconsumed += abs(dyUnconsumed)
            moveSpinner(totalUnconsumed)
        }
    }

    override fun onStopNestedScroll(child: View) {
        super.onStopNestedScroll(child)
        nestedScrollInProgress = false
        if (totalUnconsumed > 0) {
            finishSpinner(totalUnconsumed)
            totalUnconsumed = 0f
        }
    }

    // ---- spinner ---------------------------------------------------------------------------

    private fun moveSpinner(overscrollTop: Float) {
        if (totalDragDistance <= 0f) return
        val dragPercent = min(1f, abs(overscrollTop / totalDragDistance))
        val adjustedPercent = max(dragPercent - 0.4f, 0f) * 5f / 3f
        val extraOS = abs(overscrollTop) - totalDragDistance
        val slingshotDist = max(1f, (spinnerOffsetEnd - originalOffsetTop).toFloat())
        val tensionSlingshot = max(0f, min(extraOS, slingshotDist * 2) / slingshotDist)
        val tensionPercent = (tensionSlingshot / 4 - (tensionSlingshot / 4).pow(2)) * 2f
        val extraMove = slingshotDist * tensionPercent * 2
        val targetY = originalOffsetTop + (slingshotDist * dragPercent + extraMove).toInt()

        if (circle.visibility != VISIBLE) circle.visibility = VISIBLE
        if (circle.scaleX != 1f) {
            circle.scaleX = 1f
            circle.scaleY = 1f
        }
        if (circle.alpha != 1f) circle.alpha = 1f
        circle.setPull(
            sweep = 288f * min(1f, adjustedPercent),
            rotation = (-0.25f + 0.4f * adjustedPercent + tensionPercent * 2f) * 0.5f,
            alpha = (76 + (255 - 76) * dragPercent).toInt()
        )
        setTargetOffsetTopAndBottom(targetY - currentTargetOffsetTop)
    }

    private fun finishSpinner(overscrollTop: Float) {
        if (overscrollTop > totalDragDistance) {
            refreshing = true
            notifyOnRefresh = true
            circle.startSpinning()
            animateOffset(currentTargetOffsetTop, spinnerOffsetEnd) {
                if (notifyOnRefresh) {
                    notifyOnRefresh = false
                    listener?.onRefresh()
                }
            }
        } else {
            refreshing = false
            if (circle.visibility != VISIBLE) return
            returningToStart = true
            val fromAlpha = circle.alpha
            animateOffset(currentTargetOffsetTop, originalOffsetTop, onUpdate = { f -> circle.alpha = fromAlpha * (1f - f) }) {
                returningToStart = false
                reset()
            }
        }
    }

    private fun setTargetOffsetTopAndBottom(offset: Int) {
        if (offset == 0) return
        circle.offsetTopAndBottom(offset)
        currentTargetOffsetTop = circle.top
    }

    private fun reset() {
        offsetAnimator?.cancel()
        scaleAnimator?.cancel()
        circle.stopSpinning()
        circle.visibility = GONE
        circle.scaleX = 1f
        circle.scaleY = 1f
        circle.alpha = 1f
        returningToStart = false
        setTargetOffsetTopAndBottom(originalOffsetTop - currentTargetOffsetTop)
        currentTargetOffsetTop = circle.top
    }

    private fun animateOffset(
        from: Int,
        to: Int,
        onUpdate: ((Float) -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        offsetAnimator?.cancel()
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 200L
        animator.interpolator = DecelerateInterpolator(2f)
        animator.addUpdateListener {
            val f = it.animatedValue as Float
            setTargetOffsetTopAndBottom(from + ((to - from) * f).toInt() - currentTargetOffsetTop)
            onUpdate?.invoke(f)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false
            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!cancelled) onEnd?.invoke()
            }
        })
        offsetAnimator = animator
        animator.start()
    }

    private fun animateScale(from: Float, to: Float, duration: Long, onEnd: (() -> Unit)?) {
        scaleAnimator?.cancel()
        circle.scaleX = from
        circle.scaleY = from
        val animator = ValueAnimator.ofFloat(from, to)
        animator.duration = duration
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            val v = it.animatedValue as Float
            circle.scaleX = v
            circle.scaleY = v
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false
            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!cancelled) onEnd?.invoke()
            }
        })
        scaleAnimator = animator
        animator.start()
    }

    // ---- progress circle -------------------------------------------------------------------

    private class ProgressCircle(context: Context, private val density: Float) : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 2.5f * density
        }
        private val bounds = RectF()
        private var colors = intArrayOf(Color.BLACK)
        private var spinner: ValueAnimator? = null
        private var spinning = false
        private var spinFraction = 0f
        private var loop = 0
        private var pullSweep = 0f
        private var pullRotation = 0f
        private var pullAlpha = 255

        init {
            elevation = 4f * density
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }

        fun setBackgroundFill(color: Int) {
            fill.color = color
            invalidate()
        }

        fun setArcColors(newColors: IntArray) {
            colors = newColors.copyOf()
            invalidate()
        }

        fun setPull(sweep: Float, rotation: Float, alpha: Int) {
            pullSweep = sweep
            pullRotation = rotation
            pullAlpha = alpha.coerceIn(0, 255)
            invalidate()
        }

        fun startSpinning() {
            if (spinning) return
            spinning = true
            loop = 0
            spinner?.cancel()
            spinner = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1332L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    spinFraction = it.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationRepeat(animation: Animator) {
                        loop++
                    }
                })
                start()
            }
        }

        fun stopSpinning() {
            spinning = false
            spinner?.cancel()
            spinner = null
            pullSweep = 0f
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            canvas.drawCircle(cx, cy, min(cx, cy), fill)
            val radius = 8.75f * density
            bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
            val start: Float
            val sweep: Float
            if (spinning) {
                arc.color = colors[loop % colors.size]
                arc.alpha = 255
                start = -90f + spinFraction * 720f
                sweep = 40f + 250f * (0.5f - 0.5f * cos(2.0 * Math.PI * spinFraction).toFloat())
            } else {
                arc.color = colors[0]
                arc.alpha = pullAlpha
                start = -90f + pullRotation * 360f
                sweep = pullSweep
            }
            if (sweep > 0f) canvas.drawArc(bounds, start, sweep, false, arc)
        }
    }

    private companion object {
        const val CIRCLE_DIAMETER_DP = 40
        const val DEFAULT_TRIGGER_DP = 64
        const val DEFAULT_END_DP = 64
        const val DRAG_RATE = 0.5f
        const val INVALID_POINTER = -1
        const val INVALID_Y = -1f
    }
}
