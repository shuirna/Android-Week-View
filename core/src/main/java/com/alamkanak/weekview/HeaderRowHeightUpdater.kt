package com.alamkanak.weekview

internal class HeaderRowHeightUpdater<T>(
    private val eventsCacheWrapper: EventsCacheWrapper<T>
) : Updater {

    private var previousHorizontalOrigin: Float? = null
    private val previousAllDayEventIds = mutableSetOf<Long>()

    private val eventsCache: EventsCache<T>
        get() = eventsCacheWrapper.get()

    override fun isRequired(viewState: WeekViewViewState): Boolean {
        val didScrollHorizontally = previousHorizontalOrigin != viewState.currentOrigin.x
        val currentTimeColumnWidth = viewState.timeTextWidth + viewState.timeColumnPadding * 2
        val didTimeColumnChange = currentTimeColumnWidth != viewState.timeColumnWidth
        val allDayEvents = eventsCache[viewState.dateRange]
            .filter { it.isAllDay }
            .map { it.id }
            .toSet()
        val didEventsChange = allDayEvents.hashCode() != previousAllDayEventIds.hashCode()

        return (didScrollHorizontally || didTimeColumnChange || didEventsChange).also {
            previousAllDayEventIds.clear()
            previousAllDayEventIds += allDayEvents
        }
    }

    override fun update(viewState: WeekViewViewState) {
        previousHorizontalOrigin = viewState.currentOrigin.x
        viewState.timeColumnWidth = viewState.timeTextWidth + viewState.timeColumnPadding * 2

        val hasEventsInHeader = eventsCache[viewState.dateRange].any { it.isAllDay }
        viewState.refreshHeaderRowHeight(hasEventsInHeader)
    }
}
