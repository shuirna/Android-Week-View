package com.alamkanak.weekview

import android.graphics.Canvas
import android.util.SparseArray
import java.util.Calendar

internal class DayLabelDrawer<T>(
    private val cache: WeekViewCache<T>,
    private val dateTimeInterpreter: DateTimeInterpreter
) : CachingDrawer {

    override fun draw(
        viewState: WeekViewViewState,
        canvas: Canvas
    ) {
        val left = viewState.timeColumnWidth
        val top = 0f
        val right = canvas.width.toFloat()
        val bottom = viewState.getTotalHeaderHeight()

        canvas.drawInRect(left, top, right, bottom) {
            viewState.dateRangeWithStartPixels.forEach { (date, startPixel) ->
                drawLabel(viewState, date, startPixel)
            }
        }
    }

    private fun Canvas.drawLabel(
        viewState: WeekViewViewState,
        day: Calendar,
        startPixel: Float
    ) {
        val key = day.toEpochDays()
        val dayLabel = cache.dayLabelCache.get(key) { provideAndCacheDayLabel(key, day) }

        val x = startPixel + viewState.widthPerDay / 2

        val textPaint = if (day.isToday) {
            viewState.todayHeaderTextPaint
        } else {
            viewState.headerTextPaint
        }

        if (viewState.singleLineHeader) {
            val y = viewState.headerRowPadding.toFloat() - textPaint.ascent()
            drawText(dayLabel, x, y, textPaint)
        } else {
            // Draw the multi-line header
            val staticLayout = cache.multiLineDayLabelCache.get(key)
            val y = viewState.headerRowPadding.toFloat()
            withTranslation(x, y) {
                staticLayout.draw(this)
            }
        }
    }

    private fun provideAndCacheDayLabel(key: Int, day: Calendar): String {
        return dateTimeInterpreter.interpretDate(day).also {
            cache.dayLabelCache.put(key, it)
        }
    }

    override fun clear(viewState: WeekViewViewState) {
        cache.dayLabelCache.clear()
        cache.multiLineDayLabelCache.clear()
    }

    private fun <E> SparseArray<E>.get(key: Int, providerIfEmpty: () -> E): E {
        return get(key) ?: providerIfEmpty.invoke()
    }
}
