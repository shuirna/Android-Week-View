package com.alamkanak.weekview

import android.graphics.Canvas
import kotlin.math.max
import kotlin.math.roundToInt

internal object BackgroundGridDrawer : Drawer {

    private lateinit var hourLines: FloatArray

    override fun draw(
        viewState: WeekViewViewState,
        canvas: Canvas
    ) {
        viewState.startPixels.forEach { startPixel ->
            val startX = max(startPixel, viewState.timeColumnWidth)
            drawGrid(viewState, startX, startPixel, canvas)
        }
    }

    private fun createHourLines(viewState: WeekViewViewState): FloatArray {
        val headerHeight = viewState.getTotalHeaderHeight()
        val gridHeight = viewState.height - headerHeight.toInt()
        val linesPerDay = (gridHeight / viewState.hourHeight) + 1
        val overallLines = linesPerDay.roundToInt() * (viewState.numberOfVisibleDays + 1)
        return FloatArray(overallLines * 4) // 4 lines make a cube in the grid
    }

    private fun drawGrid(
        viewState: WeekViewViewState,
        startX: Float,
        startPixel: Float,
        canvas: Canvas
    ) {
        if (viewState.showHourSeparators) {
            hourLines = createHourLines(viewState)
            drawHourLines(viewState, startX, startPixel, canvas)
        }

        if (viewState.showDaySeparators) {
            drawDaySeparators(viewState, startPixel, canvas)
        }
    }

    private fun drawDaySeparators(
        viewState: WeekViewViewState,
        startPixel: Float,
        canvas: Canvas
    ) {
        val days = viewState.numberOfVisibleDays
        val widthPerDay = viewState.totalDayWidth
        val top = viewState.headerHeight

        for (i in 0 until days) {
            val start = startPixel + widthPerDay * (i + 1)
            canvas.drawLine(start, top, start, top + viewState.height, viewState.daySeparatorPaint)
        }
    }

    private fun drawHourLines(
        viewState: WeekViewViewState,
        startX: Float,
        startPixel: Float,
        canvas: Canvas
    ) {
        val hourStep = viewState.timeColumnHoursInterval
        var lineIndex = 0

        for (hour in hourStep until viewState.hoursPerDay step hourStep) {
            val heightOfHour = (viewState.hourHeight * hour)
            val top = viewState.headerHeight + viewState.currentOrigin.y + heightOfHour

            val widthPerDay = viewState.totalDayWidth
            val separatorWidth = viewState.hourSeparatorPaint.strokeWidth

            val isNotHiddenByHeader = top > viewState.headerHeight - separatorWidth
            val isWithinVisibleRange = top < viewState.height
            val isVisibleHorizontally = startPixel + widthPerDay - startX > 0

            if (isNotHiddenByHeader && isWithinVisibleRange && isVisibleHorizontally) {
                hourLines[lineIndex * 4] = startX
                hourLines[lineIndex * 4 + 1] = top
                hourLines[lineIndex * 4 + 2] = startPixel + widthPerDay
                hourLines[lineIndex * 4 + 3] = top
                lineIndex++
            }
        }

        canvas.drawLines(hourLines, viewState.hourSeparatorPaint)
    }
}
