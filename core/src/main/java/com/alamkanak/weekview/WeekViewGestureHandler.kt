package com.alamkanak.weekview

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.alamkanak.weekview.Direction.LEFT
import com.alamkanak.weekview.Direction.NONE
import com.alamkanak.weekview.Direction.RIGHT
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

private enum class Direction {
    NONE, LEFT, RIGHT, VERTICAL;

    val isHorizontal: Boolean
        get() = this == LEFT || this == RIGHT

    val isVertical: Boolean
        get() = this == VERTICAL
}

internal const val SCROLL_DURATION_IN_MILLIS = 250

internal class WeekViewGestureHandler<T : Any>(
    context: Context,
    private val viewState: WeekViewViewState<T>,
    private val chipCache: EventChipCache<T>,
    private val touchHandler: WeekViewTouchHandler<T>,
    private val onInvalidation: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private val scroller = OverScroller(context, FastOutLinearInInterpolator())
    private var currentScrollDirection = NONE
    private var currentFlingDirection = NONE

    private val gestureDetector = GestureDetector(context, this)

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isZooming = false
                onInvalidation()
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isZooming = true
                goToNearestOrigin()
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val hourHeight = viewState.hourHeight
                viewState.newHourHeight = (hourHeight * detector.scaleFactor).roundToInt()
                onInvalidation()
                return true
            }
        })

    private var isZooming: Boolean = false

    private val minimumFlingVelocity = context.scaledMinimumFlingVelocity
    private val scaledTouchSlop = context.scaledTouchSlop

    var scrollListener: ScrollListener? = null

    override fun onDown(
        e: MotionEvent
    ): Boolean {
        goToNearestOrigin()
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (isZooming) {
            return true
        }

        val absDistanceX = abs(distanceX)
        val absDistanceY = abs(distanceY)

        val canScrollHorizontally = viewState.horizontalScrollingEnabled

        when (currentScrollDirection) {
            NONE -> {
                // Allow scrolling only in one direction.
                currentScrollDirection = if (absDistanceX > absDistanceY && canScrollHorizontally) {
                    if (distanceX > 0) LEFT else RIGHT
                } else {
                    Direction.VERTICAL
                }
            }
            LEFT -> {
                // Change direction if there was enough change.
                if (absDistanceX > absDistanceY && distanceX < -scaledTouchSlop) {
                    currentScrollDirection = RIGHT
                }
            }
            RIGHT -> {
                // Change direction if there was enough change.
                if (absDistanceX > absDistanceY && distanceX > scaledTouchSlop) {
                    currentScrollDirection = LEFT
                }
            }
            else -> Unit
        }

        // Calculate the new origin after scroll.
        when {
            currentScrollDirection.isHorizontal -> {
                viewState.currentOrigin.x -= distanceX.roundToInt()
                viewState.currentOrigin.x = min(viewState.currentOrigin.x, viewState.maxX)
                viewState.currentOrigin.x = max(viewState.currentOrigin.x, viewState.minX)
                onInvalidation()
            }
            currentScrollDirection.isVertical -> {
                viewState.currentOrigin.y -= distanceY.roundToInt()
                onInvalidation()
            }
            else -> Unit
        }

        return true
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (isZooming) {
            return true
        }

        val isHorizontalAndDisabled =
            currentFlingDirection.isHorizontal && !viewState.horizontalScrollingEnabled

        val isVerticalAndDisabled =
            currentFlingDirection.isVertical && !viewState.verticalScrollingEnabled

        if (isHorizontalAndDisabled || isVerticalAndDisabled) {
            return true
        }

        scroller.forceFinished(true)

        currentFlingDirection = currentScrollDirection
        when {
            currentFlingDirection.isHorizontal -> onFlingHorizontal(velocityX)
            currentFlingDirection.isVertical -> onFlingVertical(velocityY)
            else -> Unit
        }

        onInvalidation()
        return true
    }

    private fun onFlingHorizontal(
        originalVelocityX: Float
    ) {
        val startX = viewState.currentOrigin.x
        val startY = viewState.currentOrigin.y

        val velocityX = originalVelocityX.toInt()
        val velocityY = 0

        val minX = viewState.minX
        val maxX = viewState.maxX

        val dayHeight = viewState.hourHeight * viewState.hoursPerDay
        val viewHeight = viewState.bounds.height

        val minY = (dayHeight + viewState.headerBounds.height - viewHeight) * -1
        val maxY = 0

        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
    }

    private fun onFlingVertical(
        originalVelocityY: Float
    ) {
        val startX = viewState.currentOrigin.x
        val startY = viewState.currentOrigin.y

        val velocityX = 0
        val velocityY = originalVelocityY.toInt()

        val minX = Int.MIN_VALUE
        val maxX = Int.MAX_VALUE

        val dayHeight = viewState.hourHeight * viewState.hoursPerDay
        val viewHeight = viewState.bounds.height

        val minY = (dayHeight + viewState.headerBounds.height - viewHeight) * -1
        val maxY = 0

        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
    }

    override fun onSingleTapConfirmed(
        e: MotionEvent
    ): Boolean {
        touchHandler.handleClick(e.x, e.y)
        return super.onSingleTapConfirmed(e)
    }

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        touchHandler.handleLongClick(e.x, e.y)
    }

    internal fun findHitEvent(x: Float, y: Float): EventChip<T>? {
        val candidates = chipCache.allEventChips.filter { it.isHit(x, y) }
        return when {
            candidates.isEmpty() -> null
            // Two events hit. This is most likely because an all-day event was clicked, but a
            // single event is rendered underneath it. We return the all-day event.
            candidates.size == 2 -> candidates.first { it.event.isAllDay }
            else -> candidates.first()
        }
    }

    private fun goToNearestOrigin() {
        val dayWidth = viewState.widthPerDay
        val daysFromOrigin = viewState.currentOrigin.x / dayWidth.toDouble()

        val adjustedDaysFromOrigin = when {
            // snap to nearest day
            currentFlingDirection != NONE -> round(daysFromOrigin)
            // snap to last day
            currentScrollDirection == LEFT -> floor(daysFromOrigin)
            // snap to next day
            currentScrollDirection == NONE -> ceil(daysFromOrigin)
            // snap to nearest day
            else -> round(daysFromOrigin)
        }

        val nearestOrigin = (viewState.currentOrigin.x - adjustedDaysFromOrigin * dayWidth).roundToInt()
        if (nearestOrigin != 0) {
            // Stop current animation
            scroller.forceFinished(true)

            // Snap to date
            val startX = viewState.currentOrigin.x
            val startY = viewState.currentOrigin.y

            val distanceX = -nearestOrigin
            val distanceY = 0

            val daysScrolled = abs(nearestOrigin).divideBy(viewState.widthPerDay) // / viewState.widthPerDay
            val duration = (daysScrolled * SCROLL_DURATION_IN_MILLIS)

            scroller.startScroll(startX, startY, distanceX, distanceY, duration)
            onInvalidation()
        }

        // Reset scrolling and fling direction.
        currentFlingDirection = NONE
        currentScrollDirection = currentFlingDirection
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val value = gestureDetector.onTouchEvent(event)

        // Check after call of gestureDetector, so currentFlingDirection and currentScrollDirection
        // are set
        if (event.action == ACTION_UP && !isZooming && currentFlingDirection == NONE) {
            if (currentScrollDirection == RIGHT || currentScrollDirection == LEFT) {
                goToNearestOrigin()
            }
            currentScrollDirection = NONE
        }

        return value
    }

    fun forceScrollFinished() {
        scroller.forceFinished(true)
        currentFlingDirection = NONE
        currentScrollDirection = currentFlingDirection
    }

    fun computeScroll() {
        val isFinished = scroller.isFinished
        val isFlinging = currentFlingDirection != NONE
        val isScrolling = currentScrollDirection != NONE

        if (isFinished && isFlinging) {
            // Snap to day after fling is finished
            goToNearestOrigin()
        } else if (isFinished && !isScrolling) {
            // Snap to day after scrolling is finished
            goToNearestOrigin()
        } else {
            if (isFlinging && shouldForceFinishScroll()) {
                goToNearestOrigin()
            } else if (scroller.computeScrollOffset()) {
                viewState.currentOrigin.x = scroller.currX
                viewState.currentOrigin.y = scroller.currY
                onInvalidation()
            }
        }
    }

    private fun shouldForceFinishScroll(): Boolean {
        return scroller.currVelocity <= minimumFlingVelocity
    }

    private val Context.scaledMinimumFlingVelocity: Int
        get() = ViewConfiguration.get(this).scaledMinimumFlingVelocity

    private val Context.scaledTouchSlop: Int
        get() = ViewConfiguration.get(this).scaledTouchSlop
}
