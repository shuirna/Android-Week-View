package com.alamkanak.weekview

import android.graphics.Canvas
import java.util.Calendar

internal class SingleEventsDrawer<T>(
    viewState: WeekViewViewState<T>,
    private val chipCache: EventChipCache<T>
) : Drawer<T> {

    private val eventChipDrawer = EventChipDrawer(viewState)

    override fun draw(viewState: WeekViewViewState<T>, canvas: Canvas) {
        for (date in viewState.dateRange) {
            drawEventsForDate(date, canvas)
        }
    }

    private fun drawEventsForDate(
        date: Calendar,
        canvas: Canvas
    ) {
        chipCache
            .normalEventChipsByDate(date)
            .filter { it.bounds != null }
            .forEach { eventChipDrawer.draw(it, canvas) }
    }
}

internal class AllDayEventsDrawer<T>(
    viewState: WeekViewViewState<T>
) : Drawer<T> {

    private val eventChipDrawer = EventChipDrawer(viewState)

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) {
        canvas.drawInRect(viewState.headerBounds) {
            val eventChips = viewState.cache.allDayEventLayouts
            for ((eventChip, textLayout) in eventChips) {
                eventChipDrawer.draw(eventChip, canvas, textLayout)
            }
        }
    }
}
