package com.alamkanak.weekview

import android.text.StaticLayout
import android.text.TextPaint
import android.util.SparseArray
import java.util.Calendar

internal class MultiLineDayLabelHeightUpdater<T : Any>(
    private val cache: WeekViewCache<T>,
    private val dateTimeInterpreter: DateTimeInterpreter
) : Updater {

    private var previousHorizontalOrigin: Float? = null

    override fun isRequired(viewState: WeekViewViewState): Boolean {
        if (viewState.singleLineHeader) {
            return false
        }

        val currentTimeColumnWidth = viewState.timeTextWidth + viewState.timeColumnPadding * 2
        val didTimeColumnChange = currentTimeColumnWidth != viewState.timeColumnWidth
        val didScrollHorizontally = previousHorizontalOrigin != viewState.currentOrigin.x
        val isCacheIncomplete = viewState.numberOfVisibleDays != cache.allDayEventLayouts.size

        return didTimeColumnChange || didScrollHorizontally || isCacheIncomplete
    }

    override fun update(viewState: WeekViewViewState) {
        previousHorizontalOrigin = viewState.currentOrigin.x

        val multiDayLabels = viewState.dateRange.map {
            it to calculateStaticLayoutForDate(viewState, it)
        }

        for ((date, multiDayLabel) in multiDayLabels) {
            val key = date.toEpochDays()
            cache.multiLineDayLabels.put(key, multiDayLabel)
        }

        val staticLayout = multiDayLabels
            .map { it.second }
            .maxBy { it.height }

        viewState.headerTextHeight = staticLayout?.height?.toFloat() ?: 0f
        viewState.refreshHeaderHeight()
    }

    private fun calculateStaticLayoutForDate(
        viewState: WeekViewViewState,
        date: Calendar
    ): StaticLayout {
        val key = date.toEpochDays()
        val dayLabel = cache.dateLabels.get(key) { provideAndCacheDayLabel(key, date) }

        val textPaint = if (date.isToday) {
            viewState.todayHeaderTextPaint
        } else {
            viewState.headerTextPaint
        }

        return buildStaticLayout(viewState, dayLabel, TextPaint(textPaint))
    }

    private fun buildStaticLayout(
        viewState: WeekViewViewState,
        dayLabel: String,
        textPaint:
        TextPaint
    ): StaticLayout {
        val width = viewState.totalDayWidth.toInt()
        return TextLayoutBuilder.build(dayLabel, textPaint, width)
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
