package com.alamkanak.weekview

import android.graphics.Rect
import kotlin.math.roundToInt

internal class EventChipBoundsCalculator<T> {

    fun calculateSingleEvent(
        viewState: WeekViewViewState<T>,
        eventChip: EventChip<T>,
        startPixel: Int
    ): Rect {
        val widthPerDay = viewState.drawableWidthPerDay
        val singleVerticalMargin = viewState.eventMarginVertical / 2

        val minutesFromStart = eventChip.minutesFromStartHour
        val top = viewState.calculateDistanceFromTop(minutesFromStart) + singleVerticalMargin

        val bottomMinutesFromStart = minutesFromStart + eventChip.event.durationInMinutes
        val bottom = viewState.calculateDistanceFromTop(bottomMinutesFromStart) - singleVerticalMargin

        var left = startPixel + (eventChip.relativeStart * widthPerDay).roundToInt()
        var right = left + (eventChip.relativeWidth * widthPerDay).roundToInt()

        if (left > startPixel) {
            left += (viewState.overlappingEventGap / 2f).roundToInt()
        }

        if (right < startPixel + widthPerDay) {
            right -= (viewState.overlappingEventGap / 2f).roundToInt()
        }

        val hasNoOverlaps = (right == startPixel + widthPerDay)
        if (viewState.isSingleDay && hasNoOverlaps) {
            right -= viewState.eventMarginHorizontal * 2
        }

        return Rect(left, top, right, bottom)
    }

    private fun WeekViewViewState<T>.calculateDistanceFromTop(
        minutesFromStart: Int
    ): Int {
        val portionOfDay = minutesFromStart / minutesPerDay.toFloat()
        val pixelsFromTop = hourHeight * hoursPerDay * portionOfDay
        return currentOrigin.y + headerBounds.height + pixelsFromTop.roundToInt()
    }

    fun calculateAllDayEvent(
        viewState: WeekViewViewState<T>,
        eventChip: EventChip<T>,
        startPixel: Int
    ): Rect {
        val top = checkNotNull(viewState.headerTextHeight) + (viewState.headerRowPadding * 1.5f).roundToInt()
        val height = viewState.allDayEventTextPaint.textSize.roundToInt() + viewState.eventPaddingVertical * 2
        val bottom = top + height

        val widthPerDay = viewState.drawableWidthPerDay

        var left = startPixel + (eventChip.relativeStart * widthPerDay).roundToInt()
        var right = left + (eventChip.relativeWidth * widthPerDay).roundToInt()

        if (left > startPixel) {
            left += (viewState.overlappingEventGap / 2f).roundToInt()
        }

        if (right < startPixel + widthPerDay) {
            right -= (viewState.overlappingEventGap / 2f).roundToInt()
        }

        val hasNoOverlaps = (right == startPixel + widthPerDay)
        if (viewState.isSingleDay && hasNoOverlaps) {
            right -= viewState.eventMarginHorizontal * 2
        }

        return Rect(left, top, right, bottom)
    }
}
