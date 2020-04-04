package com.alamkanak.weekview

import android.graphics.Canvas
import kotlin.math.max

internal class BackgroundGridDrawer<T> : Drawer<T> {

    private lateinit var hourLines: IntArray

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) {
        viewState.startPixels.forEach { startPixel ->
            val actualStartPixel = max(startPixel, viewState.timeColumnWidth)
            canvas.drawGrid(viewState, actualStartPixel, startPixel)
        }
    }

    private fun createHourLines(viewState: WeekViewViewState<T>): IntArray {
        val gridHeight = viewState.calendarAreaBounds.height
        val linesPerDay = (gridHeight / viewState.hourHeight) + 1
        val overallLines = linesPerDay * (viewState.numberOfVisibleDays + 1)
        return IntArray(overallLines * 4) // 4 lines make a cube in the grid
    }

    private fun Canvas.drawGrid(
        viewState: WeekViewViewState<T>,
        actualStartPixel: Int,
        startPixel: Int
    ) {
        if (viewState.showHourSeparators) {
            hourLines = createHourLines(viewState)
            drawHourLines(viewState, actualStartPixel, startPixel)
        }

        if (viewState.showDaySeparators) {
            drawDaySeparators(viewState, startPixel)
        }
    }

    private fun Canvas.drawDaySeparators(
        viewState: WeekViewViewState<T>,
        startPixel: Int
    ) {
        val days = viewState.numberOfVisibleDays
        val widthPerDay = viewState.widthPerDay
        val top = viewState.headerBounds.height

        for (i in 0 until days) {
            val start = startPixel + widthPerDay * (i + 1)
            drawLine(
                startX = start,
                startY = top,
                endX = start,
                endY = top + viewState.bounds.height,
                paint = viewState.daySeparatorPaint
            )
        }
    }

    private fun Canvas.drawHourLines(
        viewState: WeekViewViewState<T>,
        actualStartPixel: Int,
        startPixel: Int
    ) {
        val hourStep = viewState.timeColumnHoursInterval
        val hoursRange = hourStep until viewState.hoursPerDay
        val hoursSteps = (hoursRange step viewState.timeColumnHoursInterval).toList()

        val headerHeight = viewState.headerBounds.height
        val widthPerDay = viewState.widthPerDay
        val separatorWidth = viewState.hourSeparatorPaint.strokeWidth

        val hourLines = hoursSteps.flatMap { hour ->
            val heightOfHour = (viewState.hourHeight * hour)
            val top = headerHeight + viewState.currentOrigin.y + heightOfHour

            val isNotHiddenByHeader = top > headerHeight - separatorWidth
            val isWithinVisibleRange = top < viewState.bounds.height
            val isVisibleHorizontally = startPixel + widthPerDay - actualStartPixel > 0

            if (isNotHiddenByHeader && isWithinVisibleRange && isVisibleHorizontally) {
                listOf(
                    actualStartPixel.toFloat(),
                    top.toFloat(),
                    (startPixel + widthPerDay).toFloat(),
                    top.toFloat()
                )
            } else {
                emptyList()
            }
        }

        drawLines(hourLines.toFloatArray(), viewState.hourSeparatorPaint)
    }
}

private fun IntProgression.toList() = asIterable().toList()
