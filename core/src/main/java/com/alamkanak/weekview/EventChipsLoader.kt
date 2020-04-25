package com.alamkanak.weekview

internal class EventChipsLoader<T>(
    viewState: WeekViewViewState<T>,
    private val chipCache: EventChipCache<T>
) {

    private val eventSplitter = WeekViewEventSplitter(viewState)

    fun createAndCacheEventChips(events: List<ResolvedWeekViewEvent<T>>) {
        chipCache += convertEventsToEventChips(events)
    }

    private fun convertEventsToEventChips(
        events: List<ResolvedWeekViewEvent<T>>
    ): List<EventChip<T>> = events
        .sortedWith(compareBy({ it.startTime }, { it.endTime }))
        .map(this::convertEventToEventChips)
        .flatten()

    private fun convertEventToEventChips(
        event: ResolvedWeekViewEvent<T>
    ): List<EventChip<T>> = eventSplitter.split(event).map { EventChip(it, event) }
}
