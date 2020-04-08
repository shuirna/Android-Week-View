package com.alamkanak.weekview

import android.graphics.Canvas
import android.text.StaticLayout
import java.util.Calendar

internal class DateLabelsDrawer<T> : Drawer<T> {

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
        val textLayout = viewState.cache.dateLayouts[date.toEpochDays()]
        val x = startPixel + viewState.drawableWidthPerDay.scaleBy(0.5f)
        val y = viewState.headerRowPadding

        withTranslation(x, y) {
            draw(textLayout)
        }
    }

    private fun Canvas.draw(staticLayout: StaticLayout) {
        staticLayout.draw(this)
    }
}
