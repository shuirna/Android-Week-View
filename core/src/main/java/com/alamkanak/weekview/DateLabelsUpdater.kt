package com.alamkanak.weekview

import android.graphics.Color
import android.text.StaticLayout
import java.util.Calendar

internal class DateLabelsUpdater<T> : Updater<T> {

    private var previousHorizontalOrigin: Int? = null

    override fun isRequired(viewState: WeekViewViewState<T>): Boolean = with(viewState) {
        val currentTimeColumnWidth = timeTextWidth + timeColumnPadding * 2
        val didTimeColumnChange = currentTimeColumnWidth != timeColumnWidth
        val didScrollHorizontally = previousHorizontalOrigin != currentOrigin.x
        val isCacheIncomplete = numberOfVisibleDays != cache.allDayEventLayouts.size
        return didTimeColumnChange || didScrollHorizontally || isCacheIncomplete
    }

    override fun update(viewState: WeekViewViewState<T>) {
        previousHorizontalOrigin = viewState.currentOrigin.x

        val textLayouts = viewState.dateRange.map { viewState.getOrCreateDateTextLayout(it) }
        val datesWithTextLayouts = viewState.dateRange.zip(textLayouts)

        datesWithTextLayouts.forEach { (key, textLayout) ->
            viewState.cache.dateLayouts[key.toEpochDays()] = textLayout
        }

        viewState.headerTextHeight = textLayouts.map { it.height }.max() ?: 0
    }

    private fun WeekViewViewState<T>.getOrCreateDateTextLayout(
        date: Calendar
    ): StaticLayout {
        val key = date.toEpochDays()
        return cache.dateLayouts[key] ?: createDateTextLayout(date)
    }

    private fun WeekViewViewState<T>.createDateTextLayout(date: Calendar): StaticLayout {
        val dateLabel = dateFormatter(date, numberOfVisibleDays)
        val textPaint = if (date.isToday) todayHeaderTextPaint else headerTextPaint
        return dateLabel.toTextLayout(textPaint = textPaint.apply {
            bgColor = Color.RED
        }, width = drawableWidthPerDay)
    }
}
