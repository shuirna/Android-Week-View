package com.alamkanak.weekview

import android.graphics.Canvas
import android.graphics.Point
import kotlin.math.roundToInt

typealias HourWithCoordinates = Pair<Int, Point?>
typealias HourWithValidCoordinates = Pair<Int, Point>

internal class TimeColumnDrawer<T>(
    viewState: WeekViewViewState<T>,
    dateTimeInterpreter: DateTimeInterpreter
) : Drawer<T> {

    init {
        // TODO Actually move to ViewState
        viewState.cacheTimeLabels(dateTimeInterpreter)
    }

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) {
        // Draw the time column background
        canvas.drawRect(viewState.timeColumnBounds, viewState.timeColumnBackgroundPaint)

        val hours = viewState.calculateHours()
        canvas.drawTimes(viewState, hours)

        if (viewState.showTimeColumnSeparator) {
            canvas.drawTimeColumnSeparator(viewState)
        }

        if (viewState.showTimeColumnHourSeparator) {
            val hourLines = hours.createHourLines(viewState)
            canvas.drawLines(hourLines, viewState.hourSeparatorPaint)
        }
    }

    private fun Canvas.drawTimeColumnSeparator(
        viewState: WeekViewViewState<T>
    ) = with(viewState) {
        drawLine(
            startX = timeColumnBounds.right,
            startY = timeColumnBounds.top, // TODO
            endX = timeColumnBounds.right,
            endY = timeColumnBounds.bottom,
            paint = timeColumnSeparatorPaint
        )
    }

    private fun IntArray.createHourLines(viewState: WeekViewViewState<T>): FloatArray {
        val result = FloatArray(count() * 4)

        val headerHeight = viewState.headerBounds.height().toFloat()
        val timeColumnWidth = viewState.timeColumnWidth.toFloat()

        filter { hour -> hour > 0 }.forEach { hour ->
            val index = hour - 1
            result[index * 4] = 0f
            result[index * 4 + 1] = headerHeight
            result[index * 4 + 2] = timeColumnWidth
            result[index * 4 + 3] = headerHeight
        }

        return result
    }

    private fun WeekViewViewState<T>.calculateHours(): IntArray {
        return (startHour until hoursPerDay step timeColumnHoursInterval)
            .asIterable()
            .toList()
            .toIntArray()
    }

    private fun Canvas.drawTimes(
        viewState: WeekViewViewState<T>,
        hours: IntArray
    ) {
        val hoursWithCoordinates = hours.map { hour ->
            HourWithCoordinates(
                first = hour,
                second = viewState.calculateCoordinates(hour)
            )
        }

        val validHoursWithCoordinates = hoursWithCoordinates.mapNotNull { it.validate() }

        val timeLabels = viewState.cache.timeLabels
        validHoursWithCoordinates.forEach { (hour, coordinates) ->
            drawText(timeLabels[hour], coordinates.x, coordinates.y, viewState.timeTextPaint)
        }
    }

    private fun WeekViewViewState<T>.calculateCoordinates(hour: Int): Point? {
        val heightOfHour = (hourHeight * hour)
        val topMargin = timeColumnBounds.top + currentOrigin.y + heightOfHour

        val isOutsideVisibleArea = topMargin > timeColumnBounds.bottom
        if (isOutsideVisibleArea) {
            return null
        }

        val timeTextWidth = checkNotNull(timeTextWidth)
        val timeTextHeight = checkNotNull(timeTextHeight)

        val x = timeTextWidth + timeColumnPadding
        var y = topMargin + timeTextHeight / 2

        // If the hour separator is shown in the time column, move the time label below it
        if (showTimeColumnHourSeparator) {
            val hourSeparatorWidth = hourSeparatorPaint.strokeWidth.roundToInt()
            val padding = timeColumnPadding
            y += timeTextHeight.scaleBy(factor = 0.5f) + hourSeparatorWidth + padding
        }

        return Point(x, y)
    }
}

private fun HourWithCoordinates.validate(): HourWithValidCoordinates? {
    return second?.let { HourWithValidCoordinates(first, it) }
}
