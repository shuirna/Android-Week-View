package com.alamkanak.weekview

import android.graphics.Canvas
import com.alamkanak.weekview.Constants.MINUTES_PER_HOUR
import kotlin.math.max

internal object NowLineDrawer : Drawer {

    override fun draw(
        viewState: WeekViewViewState,
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
        viewState: WeekViewViewState,
        startPixel: Float,
        canvas: Canvas
    ) {
        val top = viewState.headerHeight + viewState.currentOrigin.y
        val now = now()

        val portionOfDay = (now.hour - viewState.minHour) + now.minute / MINUTES_PER_HOUR
        val portionOfDayInPixels = portionOfDay * viewState.hourHeight
        val verticalOffset = top + portionOfDayInPixels

        val startX = max(startPixel, viewState.timeColumnWidth)
        val endX = startPixel + viewState.totalDayWidth
        canvas.drawLine(startX, verticalOffset, endX, verticalOffset, viewState.nowLinePaint)

        if (viewState.showNowLineDot) {
            drawDot(viewState, startPixel, verticalOffset, canvas)
        }
    }

    private fun drawDot(
        viewState: WeekViewViewState,
        startPixel: Float,
        lineStartY: Float,
        canvas: Canvas
    ) {
        // We use a margin to prevent the dot from sticking on the left side of the screen
        val dotRadius = viewState.nowDotPaint.strokeWidth
        val dotMargin = 32f
        canvas.drawCircle(startPixel + dotMargin, lineStartY, dotRadius, viewState.nowDotPaint)
    }
}
