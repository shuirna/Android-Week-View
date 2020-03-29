package com.alamkanak.weekview

internal class HeaderRowHeightUpdater<T>(
    private val eventsCacheWrapper: EventsCacheWrapper<T>
) : Updater<T> {

    private var previousHorizontalOrigin: Int? = null
    private val previousAllDayEventIds = mutableSetOf<Long>()

    private val eventsCache: EventsCache<T>
        get() = eventsCacheWrapper.get()

    override fun isRequired(viewState: WeekViewViewState<T>): Boolean {
        val didScrollHorizontally = previousHorizontalOrigin != viewState.currentOrigin.x
        val currentTimeColumnWidth = checkNotNull(viewState.timeTextWidth) + viewState.timeColumnPadding * 2

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

    override fun update(viewState: WeekViewViewState<T>) {
        previousHorizontalOrigin = viewState.currentOrigin.x
        // TODO viewState.timeColumnWidth = checkNotNull(viewState.timeTextWidth) + viewState.timeColumnPadding * 2

        viewState.hasEventsInHeader = eventsCache[viewState.dateRange].any { it.isAllDay }
        viewState.refreshHeaderHeight()
    }
}
