package com.alamkanak.weekview

import android.graphics.Rect
import java.util.Calendar

internal class SingleEventsUpdater<T : Any>(
    private val chipCache: EventChipCache<T>
) : Updater<T> {

    private val rectCalculator = EventChipRectCalculator<T>()

    override fun isRequired(viewState: WeekViewViewState<T>) = true

    override fun update(viewState: WeekViewViewState<T>) {
        chipCache.clearSingleEventsCache()

        viewState
            .dateRangeWithStartPixels
            .forEach { (date, startPixel) ->
                // If we use a horizontal margin in the day view, we need to offset the start pixel.
                val modifiedStartPixel = when {
                    viewState.isSingleDay -> startPixel + viewState.eventMarginHorizontal
                    else -> startPixel
                }
                calculateRectsForEventsOnDate(viewState, date, modifiedStartPixel)
            }
    }

    private fun calculateRectsForEventsOnDate(
        viewState: WeekViewViewState<T>,
        date: Calendar,
        startPixel: Int
    ) {
        chipCache.normalEventChipsByDate(date)
            .filter { it.event.isNotAllDay && it.event.isWithin(viewState.minHour, viewState.maxHour) }
            .forEach { it.calculateEventChipBounds(viewState, startPixel) }
    }

    private fun EventChip<T>.calculateEventChipBounds(
        viewState: WeekViewViewState<T>,
        startPixel: Int
    ) {
        val candidate = rectCalculator.calculateSingleEvent(viewState, this, startPixel)
        bounds = candidate.takeIf { it.isValidSingleEventRect(viewState) }
    }

    private fun Rect.isValidSingleEventRect(viewState: WeekViewViewState<T>): Boolean {
        val hasCorrectWidth = left < right && left < viewState.bounds.width
        val hasCorrectHeight = top < viewState.bounds.height

        val calendarAreaBounds = viewState.calendarAreaBounds
        val isNotHiddenByChrome = right >= calendarAreaBounds.left && bottom >= calendarAreaBounds.top

        return hasCorrectWidth && hasCorrectHeight && isNotHiddenByChrome
    }
}
