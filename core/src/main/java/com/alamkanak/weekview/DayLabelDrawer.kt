package com.alamkanak.weekview

import android.graphics.Canvas
import android.text.StaticLayout
import java.util.Calendar

internal class DayLabelDrawer<T> : Drawer<T> {

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) {
        canvas.drawInRect(viewState.headerBounds) {
            viewState.dateRangeWithStartPixels.forEach { (date, startPixel) ->
                canvas.drawLabel(viewState, date, startPixel)
            }
        }
    }

    private fun Canvas.drawLabel(
        viewState: WeekViewViewState<T>,
        date: Calendar,
        startPixel: Int
    ) {
        val key = date.toEpochDays()
        val dayLabel = viewState.getOrCreateDateLabel2(key, date)

        viewState.headerTextHeight = dayLabel.height

        // val x = startPixel + viewState.widthPerDay.scaleBy(0.5f) // - dayLabel.width.scaleBy(0.5f)

//        val textPaint = if (date.isToday) {
//            viewState.todayHeaderTextPaint
//        } else {
//            viewState.headerTextPaint
//        }

        if (viewState.singleLineHeader) {
            // val y = viewState.headerRowPadding - textPaint.ascent().roundToInt()
            val y = viewState.headerRowPadding // - dayLabel.height.scaleBy(0.5f)

            withTranslation(x = startPixel, y = y) {
                // dayLabel.draw(this)
                val r = android.graphics.Rect()
                dayLabel.getLineBounds(0, r)
                drawTextLayout(dayLabel)
            }
            // drawText(dayLabel, x, y, textPaint)
        } else {
            // Draw the multi-line header
            val staticLayout = viewState.cache.multiLineDayLabels.get(key)

            val x = startPixel + viewState.widthPerDay.scaleBy(0.5f)
            val y = viewState.headerRowPadding

            withTranslation(x, y) {
                drawTextLayout(staticLayout)
                // staticLayout.draw(this)
            }
        }
    }

    private fun Canvas.drawTextLayout(textLayout: StaticLayout) = textLayout.draw(this)
}
