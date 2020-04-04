package com.alamkanak.weekview

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.ViewConfiguration
import com.alamkanak.weekview.Direction.Left
import com.alamkanak.weekview.Direction.None
import com.alamkanak.weekview.Direction.Right
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

private enum class Direction {
    None, Left, Right, Vertical;

    val isHorizontal: Boolean
        get() = this == Left || this == Right

    val isVertical: Boolean
        get() = this == Vertical
}

internal class WeekViewGestureHandler<T : Any>(
    context: Context,
    private val viewState: WeekViewViewState<T>,
    private val chipCache: EventChipCache<T>,
    private val touchHandler: WeekViewTouchHandler<T>,
    private val onInvalidation: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private val smoothScroller = SmoothScroller()

    private var currentScrollDirection = None
    private var currentFlingDirection = None

    private val gestureDetector = GestureDetector(context, this)
    private val scaleDetector = ScaleGestureDetector(context, viewState, smoothScroller, onInvalidation)

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
        val absDistanceX = abs(distanceX)
        val absDistanceY = abs(distanceY)

        val canScrollHorizontally = viewState.horizontalScrollingEnabled

        when (currentScrollDirection) {
            None -> {
                // Allow scrolling only in one direction.
                currentScrollDirection = if (absDistanceX > absDistanceY && canScrollHorizontally) {
                    if (distanceX > 0) Left else Right
                } else {
                    Direction.Vertical
                }
            }
            Left -> {
                // Change direction if there was enough change.
                if (absDistanceX > absDistanceY && distanceX < -scaledTouchSlop) {
                    currentScrollDirection = Right
                }
            }
            Right -> {
                // Change direction if there was enough change.
                if (absDistanceX > absDistanceY && distanceX > scaledTouchSlop) {
                    currentScrollDirection = Left
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
        if (currentFlingDirection.isHorizontal && !viewState.horizontalScrollingEnabled) {
            return true
        }

        if (currentFlingDirection.isVertical && !viewState.verticalScrollingEnabled) {
            return true
        }

        smoothScroller.stop()

        currentFlingDirection = currentScrollDirection
        when {
            currentFlingDirection.isHorizontal -> onFlingHorizontal()
            currentFlingDirection.isVertical -> onFlingVertical(velocityY)
            else -> Unit
        }

        onInvalidation()
        return true
    }

    private lateinit var preFlingFirstVisibleDate: Calendar

    private fun onFlingHorizontal() {
        val destinationDate = when (currentFlingDirection) {
            Left -> preFlingFirstVisibleDate + Days(viewState.numberOfVisibleDays)
            Right -> preFlingFirstVisibleDate - Days(viewState.numberOfVisibleDays)
            else -> throw IllegalStateException()
        }

        val destinationOffset = viewState.getXOriginForDate(destinationDate)
        val adjustedDestinationOffset = min(max(viewState.minX, destinationOffset), viewState.maxX)

        smoothScroller.scroll(
            fromValue = viewState.currentOrigin.x,
            toValue = adjustedDestinationOffset,
            onUpdate = {
                viewState.currentOrigin.x = it
                onInvalidation()
            }
        )
    }

    private fun onFlingVertical(
        originalVelocityY: Float
    ) {
        val dayHeight = viewState.hourHeight * viewState.hoursPerDay
        val viewHeight = viewState.bounds.height

        val minY = (dayHeight + viewState.headerBounds.height - viewHeight) * -1
        val maxY = 0

        val currentOffset = viewState.currentOrigin.y
        val destinationOffset = currentOffset + (originalVelocityY * 0.18).roundToInt()
        val adjustedDestinationOffset = min(max(destinationOffset, minY), maxY)

        smoothScroller.scroll(
            fromValue = viewState.currentOrigin.y,
            toValue = adjustedDestinationOffset,
            onUpdate = {
                viewState.currentOrigin.y = it
                onInvalidation()
            }
        )
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
            currentFlingDirection != None -> round(daysFromOrigin)
            // snap to last day
            currentScrollDirection == Left -> floor(daysFromOrigin)
            // snap to next day
            currentScrollDirection == None -> ceil(daysFromOrigin)
            // snap to nearest day
            else -> round(daysFromOrigin)
        }

        val nearestOrigin = (viewState.currentOrigin.x - adjustedDaysFromOrigin * dayWidth).roundToInt()
        if (nearestOrigin != 0) {
            // Stop current animation
            // scroller.forceFinished(true)
            smoothScroller.stop()

            // Snap to date
//            val startX = viewState.currentOrigin.x
//            val startY = viewState.currentOrigin.y

//            val distanceX = -nearestOrigin
//            val distanceY = 0

//            val daysScrolled = abs(nearestOrigin).divideBy(viewState.widthPerDay) // / viewState.widthPerDay
//            val duration = (daysScrolled * SCROLL_DURATION_IN_MILLIS)

            // scroller.startScroll(startX, startY, distanceX, distanceY, duration)

            smoothScroller.scroll(
                fromValue = 0,
                toValue = 0,
                onUpdate = {
                    viewState.currentOrigin.x = it
                    onInvalidation()
                }
            )

            onInvalidation()
        }

        // Reset scrolling and fling direction.
        currentFlingDirection = None
        currentScrollDirection = currentFlingDirection
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val handled = gestureDetector.onTouchEvent(event)

        if (event.action == ACTION_UP && currentScrollDirection != None && currentFlingDirection == None) {
            // TODO
            goToNearestOrigin()
            currentScrollDirection = None
        } else if (event.action == ACTION_DOWN) {
            preFlingFirstVisibleDate = checkNotNull(viewState.firstVisibleDate?.copy())
        }

        // Check after call of gestureDetector, so currentFlingDirection and currentScrollDirection
        // are set
//        if (event.action == ACTION_UP && /*!isZooming &&*/ currentFlingDirection == NONE) {
//            if (currentScrollDirection == RIGHT || currentScrollDirection == LEFT) {
//                goToNearestOrigin()
//            }
//            currentScrollDirection = NONE
//        }

        return handled
    }

    fun stopScroll() {
        smoothScroller.stop()
        // scroller.forceFinished(true)
        currentFlingDirection = None
        currentScrollDirection = currentFlingDirection
    }

    fun computeScroll() {
        // val isFinished = scroller.isFinished
        val isFinished = smoothScroller.isFinished
        val isScrolling = currentScrollDirection != None
        val isFlinging = currentFlingDirection != None

//        if (isFinished && isFlinging) {
//            // Snap to day after fling is finished
//            goToNearestOrigin()
//        } else if (isFinished && !isScrolling) {
//            // Snap to day after scrolling is finished
//            goToNearestOrigin()
//        } else {
//            if (isFlinging && shouldForceFinishScroll()) {
//                goToNearestOrigin()
//            } else if (scroller.computeScrollOffset()) {
//                viewState.currentOrigin.x = scroller.currX
//                viewState.currentOrigin.y = scroller.currY
//                onInvalidation()
//            }
//        }
    }

//    private fun shouldForceFinishScroll(): Boolean {
//        return scroller.currVelocity <= minimumFlingVelocity
//    }

    private val Context.scaledMinimumFlingVelocity: Int
        get() = ViewConfiguration.get(this).scaledMinimumFlingVelocity

    private val Context.scaledTouchSlop: Int
        get() = ViewConfiguration.get(this).scaledTouchSlop
}
