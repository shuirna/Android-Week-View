package com.alamkanak.weekview

@Deprecated("")
internal class TimeColumnUpdater<T>(
    private val dateTimeInterpreter: DateTimeInterpreter
) : Updater<T> {

    private var previousDateTimeInterpreter: Int? = null

    override fun isRequired(
        viewState: WeekViewViewState<T>
    ): Boolean {
        /*
        val currentDateTimeInterpreter = dateTimeInterpreter.hashCode()
        val hasInterpreterChanged = currentDateTimeInterpreter != previousDateTimeInterpreter
        val isNotInitialized = viewState.timeTextWidth == null || viewState.timeTextHeight == null

        previousDateTimeInterpreter = dateTimeInterpreter.hashCode()
        return hasInterpreterChanged || isNotInitialized
        */
        return true
    }

    override fun update(viewState: WeekViewViewState<T>) {
        // viewState.updateTimeColumnText(dateTimeInterpreter)
    }
}
