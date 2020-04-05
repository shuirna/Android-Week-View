package com.alamkanak.weekview

import android.text.StaticLayout
import android.text.TextPaint
import java.util.Calendar

internal class MultiLineDayLabelHeightUpdater<T : Any> : Updater<T> {

    private var previousHorizontalOrigin: Int? = null

    override fun isRequired(viewState: WeekViewViewState<T>): Boolean {
        if (viewState.singleLineHeader) {
            return false
        }

        val currentTimeColumnWidth = checkNotNull(viewState.timeTextWidth) + viewState.timeColumnPadding * 2
        val didTimeColumnChange = currentTimeColumnWidth != viewState.timeColumnWidth
        val didScrollHorizontally = previousHorizontalOrigin != viewState.currentOrigin.x
        val isCacheIncomplete = viewState.numberOfVisibleDays != viewState.cache.allDayEventLayouts.size

        return didTimeColumnChange || didScrollHorizontally || isCacheIncomplete
    }

    override fun update(viewState: WeekViewViewState<T>) {
        previousHorizontalOrigin = viewState.currentOrigin.x

        val multiDayLabels = viewState.dateRange.map {
            it to calculateStaticLayoutForDate(viewState, it)
        }

        for ((date, multiDayLabel) in multiDayLabels) {
            val key = date.toEpochDays()
            viewState.cache.multiLineDayLabels.put(key, multiDayLabel)
        }

        val staticLayout = multiDayLabels
            .map { it.second }
            .maxBy { it.height }

        viewState.headerTextHeight = staticLayout?.height ?: 0
    }

    private fun calculateStaticLayoutForDate(
        viewState: WeekViewViewState<T>,
        date: Calendar
    ): StaticLayout {
        val key = date.toEpochDays()
        val dayLabel = viewState.getOrCreateDateLabel(key, date)

        val textPaint = if (date.isToday) {
            viewState.todayHeaderTextPaint
        } else {
            viewState.headerTextPaint
        }

        return dayLabel.buildStaticLayout(viewState, textPaint)
    }

    private fun String.buildStaticLayout(
        viewState: WeekViewViewState<T>,
        textPaint: TextPaint
    ): StaticLayout {
        val width = viewState.widthPerDay
        return toTextLayout(textPaint, width)
    }
}
