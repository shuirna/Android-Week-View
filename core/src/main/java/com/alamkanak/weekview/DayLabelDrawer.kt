package com.alamkanak.weekview

import android.graphics.Canvas
import android.util.SparseArray
import java.util.Calendar
import kotlin.math.roundToInt

internal class DayLabelDrawer<T>(
    private val cache: WeekViewCache<T>,
    private val dateTimeInterpreter: DateTimeInterpreter
) : Drawer<T> {

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
        day: Calendar,
        startPixel: Int
    ) {
        val key = day.toEpochDays()
        val dayLabel = cache.dateLabels.get(key) { provideAndCacheDayLabel(key, day) }

        val x = startPixel + viewState.drawableWidthPerDay.scaleBy(0.5f)

        val textPaint = if (day.isToday) {
            viewState.todayHeaderTextPaint
        } else {
            viewState.headerTextPaint
        }

        if (viewState.singleLineHeader) {
            val y = viewState.headerRowPadding - textPaint.ascent().roundToInt()
            drawText(dayLabel, x, y, textPaint)
        } else {
            // Draw the multi-line header
            val staticLayout = cache.multiLineDayLabels.get(key)
            val y = viewState.headerRowPadding

            withTranslation(x, y) {
                staticLayout.draw(this)
            }
        }
    }

    private fun provideAndCacheDayLabel(key: Int, day: Calendar): String {
        return dateTimeInterpreter.interpretDate(day).also {
            cache.dateLabels.put(key, it)
        }
    }

    private fun <E> SparseArray<E>.get(key: Int, providerIfEmpty: () -> E): E {
        return get(key) ?: providerIfEmpty.invoke()
    }
}
