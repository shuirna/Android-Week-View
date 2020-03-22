package com.alamkanak.weekview

import android.graphics.Canvas
import android.graphics.Paint
import com.alamkanak.weekview.Constants.MINUTES_PER_HOUR
import java.util.Calendar
import kotlin.math.max

internal object DayBackgroundDrawer : Drawer {

    override fun draw(
        viewState: WeekViewViewState,
        canvas: Canvas
    ) {
        viewState.dateRangeWithStartPixels.forEach { (date, startPixel) ->
            drawDayBackground(viewState, date, startPixel, canvas)
        }
    }

    /**
     * Draws a day's background color in the corresponding bounds.
     *
     * @param day The [Calendar] indicating the date
     * @param startPixel The x-coordinate on which to start drawing the background
     * @param canvas The [Canvas] on which to draw the background
     */
    private fun drawDayBackground(
        viewState: WeekViewViewState,
        day: Calendar,
        startPixel: Float,
        canvas: Canvas
    ) {
        val endPixel = startPixel + viewState.widthPerDay
        val isCompletelyHiddenByTimeColumn = endPixel <= viewState.timeColumnWidth
        if (isCompletelyHiddenByTimeColumn) {
            return
        }

        val actualStartPixel = max(startPixel, viewState.timeColumnWidth)
        val height = viewState.height.toFloat()

        if (viewState.showDistinctPastFutureColor) {
            // val useWeekendColor = day.isWeekend && config.showDistinctWeekendColor
            val startY = viewState.headerHeight + viewState.currentOrigin.y
            val endX = startPixel + viewState.widthPerDay

            when {
                day.isToday -> {
                    val pastPaint = viewState.getPastBackgroundPaint(day)
                    val futurePaint = viewState.getFutureBackgroundPaint(day)
                    drawPastAndFutureRect(viewState, actualStartPixel, startY, endX, pastPaint, futurePaint, height, canvas)
                }
                day.isBeforeToday -> {
                    val pastPaint = viewState.getPastBackgroundPaint(day)
                    canvas.drawRect(actualStartPixel, startY, endX, height, pastPaint)
                }
                else -> {
                    val futurePaint = viewState.getFutureBackgroundPaint(day)
                    canvas.drawRect(actualStartPixel, startY, endX, height, futurePaint)
                }
            }
        } else {
            val todayPaint = viewState.getNormalDayBackgroundPaint(day)
            val right = startPixel + viewState.widthPerDay
            canvas.drawRect(actualStartPixel, viewState.headerHeight, right, height, todayPaint)
        }
    }

    private fun drawPastAndFutureRect(
        viewState: WeekViewViewState,
        startX: Float,
        startY: Float,
        endX: Float,
        pastPaint: Paint,
        futurePaint: Paint,
        height: Float,
        canvas: Canvas
    ) {
        val now = now()
        val beforeNow = (now.hour + now.minute / MINUTES_PER_HOUR) * viewState.hourHeight
        canvas.drawRect(startX, startY, endX, startY + beforeNow, pastPaint)
        canvas.drawRect(startX, startY + beforeNow, endX, height, futurePaint)
    }
}

private fun WeekViewViewState.getPastBackgroundPaint(date: Calendar): Paint {
    val useWeekendColor = date.isWeekend && showDistinctWeekendColor
    return if (useWeekendColor) pastWeekendBackgroundPaint else pastBackgroundPaint
}

private fun WeekViewViewState.getFutureBackgroundPaint(date: Calendar): Paint {
    val useWeekendColor = date.isWeekend && showDistinctWeekendColor
    return if (useWeekendColor) futureWeekendBackgroundPaint else futureBackgroundPaint
}

private fun WeekViewViewState.getNormalDayBackgroundPaint(date: Calendar): Paint {
    return if (date.isToday) todayBackgroundPaint else dayBackgroundPaint
}
