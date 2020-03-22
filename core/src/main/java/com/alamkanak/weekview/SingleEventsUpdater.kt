package com.alamkanak.weekview

import android.graphics.RectF
import java.util.Calendar

internal class SingleEventsUpdater<T : Any>(
    private val chipCache: EventChipCache<T>
) : Updater {

    private val rectCalculator = EventChipRectCalculator<T>()

    override fun isRequired(viewState: WeekViewViewState) = true

    override fun update(viewState: WeekViewViewState) {
        chipCache.clearSingleEventsCache()

        viewState
            .dateRangeWithStartPixels
            .forEach { (date, startPixel) ->
                // If we use a horizontal margin in the day view, we need to offset the start pixel.
                val modifiedStartPixel = when {
                    viewState.isSingleDay -> startPixel + viewState.eventMarginHorizontal.toFloat()
                    else -> startPixel
                }
                calculateRectsForEventsOnDate(viewState, date, modifiedStartPixel)
            }
    }

    private fun calculateRectsForEventsOnDate(
        viewState: WeekViewViewState,
        date: Calendar,
        startPixel: Float
    ) {
        chipCache.normalEventChipsByDate(date)
            .filter { it.event.isNotAllDay && it.event.isWithin(viewState.minHour, viewState.maxHour) }
            .forEach { eventChip ->
                eventChip.bounds = rectCalculator
                    .calculateSingleEvent(viewState, eventChip, startPixel)
                    .takeIf { bounds -> bounds.isValidSingleEventRect(viewState) }
            }
    }

    private fun RectF.isValidSingleEventRect(viewState: WeekViewViewState): Boolean {
        val hasCorrectWidth = left < right && left < viewState.width
        val hasCorrectHeight = top < viewState.height
        val isNotHiddenByChrome = right > viewState.timeColumnWidth && bottom > viewState.headerHeight
        return hasCorrectWidth && hasCorrectHeight && isNotHiddenByChrome
    }
}
