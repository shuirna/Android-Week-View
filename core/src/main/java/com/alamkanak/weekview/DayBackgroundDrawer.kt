package com.alamkanak.weekview

import android.graphics.Canvas
import android.graphics.Paint
import com.alamkanak.weekview.Constants.MINUTES_PER_HOUR
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

internal class DayBackgroundDrawer<T> : Drawer<T> {

    override fun draw(
        viewState: WeekViewViewState<T>,
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
        viewState: WeekViewViewState<T>,
        day: Calendar,
        startPixel: Int,
        canvas: Canvas
    ) {
        val endPixel = startPixel + viewState.widthPerDay
        val timeColumnWidth = viewState.timeColumnWidth

        val isCompletelyHiddenByTimeColumn = endPixel <= timeColumnWidth
        if (isCompletelyHiddenByTimeColumn) {
            return
        }

        val actualStartPixel = max(startPixel, timeColumnWidth)
        val height = viewState.bounds.height
        val headerHeight = viewState.headerBounds.height

        if (viewState.showDistinctPastFutureColor) {
            // val useWeekendColor = day.isWeekend && config.showDistinctWeekendColor
            val startY = headerHeight + viewState.currentOrigin.y
            val endX = startPixel + viewState.drawableWidthPerDay

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
            val right = startPixel + viewState.drawableWidthPerDay
            canvas.drawRect(actualStartPixel, headerHeight, right, height, todayPaint)
        }
    }

    private fun drawPastAndFutureRect(
        viewState: WeekViewViewState<T>,
        startX: Int,
        startY: Int,
        endX: Int,
        pastPaint: Paint,
        futurePaint: Paint,
        height: Int,
        canvas: Canvas
    ) {
        val now = now()
        val beforeNow = (now.hour + now.minute / MINUTES_PER_HOUR).roundToInt() * viewState.hourHeight
        canvas.drawRect(startX, startY, endX, startY + beforeNow, pastPaint)
        canvas.drawRect(startX, startY + beforeNow, endX, height, futurePaint)
    }
}

private fun <T> WeekViewViewState<T>.getPastBackgroundPaint(date: Calendar): Paint {
    val useWeekendColor = date.isWeekend && showDistinctWeekendColor
    return if (useWeekendColor) pastWeekendBackgroundPaint else pastBackgroundPaint
}

private fun <T> WeekViewViewState<T>.getFutureBackgroundPaint(date: Calendar): Paint {
    val useWeekendColor = date.isWeekend && showDistinctWeekendColor
    return if (useWeekendColor) futureWeekendBackgroundPaint else futureBackgroundPaint
}

private fun <T> WeekViewViewState<T>.getNormalDayBackgroundPaint(date: Calendar): Paint {
    return if (date.isToday) todayBackgroundPaint else dayBackgroundPaint
}
