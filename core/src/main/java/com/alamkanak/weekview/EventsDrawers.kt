package com.alamkanak.weekview

import android.content.Context
import android.graphics.Canvas
import java.util.Calendar

internal class SingleEventsDrawer<T>(
    context: Context,
    viewState: WeekViewViewState<T>,
    private val chipCache: EventChipCache<T>
) : Drawer<T> {

    private val eventChipDrawer = EventChipDrawer(context, viewState)

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
    context: Context,
    viewState: WeekViewViewState<T>,
    private val cache: WeekViewCache<T>
) : Drawer<T> {

    private val eventChipDrawer = EventChipDrawer(context, viewState)

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) {
        canvas.drawInRect(viewState.headerBounds) {
            val eventChips = cache.allDayEventLayouts
            for ((eventChip, textLayout) in eventChips) {
                eventChipDrawer.draw(eventChip, canvas, textLayout)
            }
        }
    }
}
