package com.alamkanak.weekview

import com.alamkanak.weekview.Constants.UNINITIALIZED

internal class TimeColumnUpdater(
    private val dateTimeInterpreter: DateTimeInterpreter
) : Updater {

    private var previousDateTimeInterpreter: Int? = null

    override fun isRequired(
        viewState: WeekViewViewState
    ): Boolean {
        val currentDateTimeInterpreter = dateTimeInterpreter.hashCode()
        val hasInterpreterChanged = currentDateTimeInterpreter != previousDateTimeInterpreter
        val isNotInitialized =
            viewState.timeTextWidth == UNINITIALIZED || viewState.timeTextHeight == UNINITIALIZED

        previousDateTimeInterpreter = dateTimeInterpreter.hashCode()
        return hasInterpreterChanged || isNotInitialized
    }

    override fun update(viewState: WeekViewViewState) {
        viewState.updateTimeColumnText(dateTimeInterpreter)
    }
}
