package com.alamkanak.weekview

import android.graphics.Canvas
import java.util.Calendar
import kotlin.math.roundToInt

internal class DayLabelDrawer<T> : Drawer<T> {

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) {
        canvas.drawInRect(viewState.headerBounds) {
            viewState.dateRangeWithStartPixels.forEach { (date, startPixel) ->
                drawLabel(viewState, date, startPixel)
            }
        }
    }

    private fun Canvas.drawLabel(
        viewState: WeekViewViewState<T>,
        date: Calendar,
        startPixel: Int
    ) {
        val key = date.toEpochDays()
        val dayLabel = viewState.getOrCreateDateLabel(key, date)

        val x = startPixel + viewState.drawableWidthPerDay.scaleBy(0.5f)

        val textPaint = if (date.isToday) {
            viewState.todayHeaderTextPaint
        } else {
            viewState.headerTextPaint
        }

        if (viewState.singleLineHeader) {
            val y = viewState.headerRowPadding - textPaint.ascent().roundToInt()
            drawText(dayLabel, x, y, textPaint)
        } else {
            // Draw the multi-line header
            val staticLayout = viewState.cache.multiLineDayLabels.get(key)
            val y = viewState.headerRowPadding

            withTranslation(x, y) {
                staticLayout.draw(this)
            }
        }
    }
}
