package com.alamkanak.weekview

import android.content.Context
import android.graphics.Canvas
import java.util.Calendar

internal class SingleEventsDrawer<T>(
    context: Context,
    viewState: WeekViewViewState,
    private val chipCache: EventChipCache<T>
) : Drawer {

    private val eventChipDrawer = EventChipDrawer<T>(context, viewState)

    override fun draw(viewState: WeekViewViewState, canvas: Canvas) {
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
    viewState: WeekViewViewState,
    private val cache: WeekViewCache<T>
) : CachingDrawer {

    private val eventChipDrawer = EventChipDrawer<T>(context, viewState)

    override fun draw(
        viewState: WeekViewViewState,
        canvas: Canvas
    ) {
        val left = viewState.timeColumnWidth
        val top = 0f
        val right = canvas.width.toFloat()
        val bottom = viewState.getTotalHeaderHeight()

        canvas.drawInRect(left, top, right, bottom) {
            val eventChips = cache.allDayEventLayouts
            for ((eventChip, textLayout) in eventChips) {
                eventChipDrawer.draw(eventChip, canvas, textLayout)
            }
        }
    }

    override fun clear(viewState: WeekViewViewState) {
        cache.allDayEventLayouts.clear()
    }
}
