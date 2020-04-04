package com.alamkanak.weekview

import android.graphics.Canvas
import com.alamkanak.weekview.Constants.MINUTES_PER_HOUR
import kotlin.math.max
import kotlin.math.roundToInt

internal class NowLineDrawer<T>() : Drawer<T> {

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) {
        if (viewState.showNowLine.not()) {
            return
        }

        val startPixel = viewState
            .dateRangeWithStartPixels
            .filter { (date, _) -> date.isToday }
            .map { (_, startPixel) -> startPixel }
            .firstOrNull() ?: return

        drawLine(viewState, startPixel, canvas)
    }

    private fun drawLine(
        viewState: WeekViewViewState<T>,
        startPixel: Int,
        canvas: Canvas
    ) {
        val top = viewState.headerBounds.height + viewState.currentOrigin.y
        val now = now()

        val portionOfDay = (now.hour - viewState.minHour) + now.minute / MINUTES_PER_HOUR
        val portionOfDayInPixels = portionOfDay * viewState.hourHeight
        val verticalOffset = top + portionOfDayInPixels.roundToInt()

        val timeColumnWidth = viewState.timeColumnWidth
        val startX = max(startPixel, timeColumnWidth)
        val endX = startPixel + viewState.widthPerDay
        canvas.drawLine(startX, verticalOffset, endX, verticalOffset, viewState.nowLinePaint)

        if (viewState.showNowLineDot) {
            canvas.drawDot(viewState, startPixel, verticalOffset)
        }
    }

    private fun Canvas.drawDot(
        viewState: WeekViewViewState<T>,
        startPixel: Int,
        lineStartY: Int
    ) {
        val dotRadius = viewState.nowDotPaint.strokeWidth.roundToInt()
        val actualStartPixel = max(startPixel, viewState.timeColumnWidth)
        drawCircle(actualStartPixel, lineStartY, dotRadius, viewState.nowDotPaint)
    }
}
