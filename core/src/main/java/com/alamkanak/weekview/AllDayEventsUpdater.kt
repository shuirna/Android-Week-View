package com.alamkanak.weekview

import android.graphics.Rect
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextUtils.TruncateAt.END

internal class AllDayEventsUpdater<T : Any>(
    private val chipCache: EventChipCache<T>
) : Updater<T> {

    private val boundsCalculator = EventChipBoundsCalculator<T>()
    private val spannableStringBuilder = SpannableStringBuilder()

    private var previousHorizontalOrigin: Int? = null
    private var dummyTextLayout: StaticLayout? = null

    override fun isRequired(viewState: WeekViewViewState<T>): Boolean {
        val didScrollHorizontally = previousHorizontalOrigin != viewState.currentOrigin.x
        val dateRange = viewState.dateRange
        val containsNewChips = chipCache.allDayEventChipsInDateRange(dateRange).any { it.bounds == null }
        return didScrollHorizontally || containsNewChips
    }

    override fun update(viewState: WeekViewViewState<T>) {
        viewState.cache.allDayEventLayouts.clear()

        val datesWithStartPixels = viewState.dateRangeWithStartPixels
        for ((date, startPixel) in datesWithStartPixels) {
            // If we use a horizontal margin in the day view, we need to offset the start pixel.
            val modifiedStartPixel = when {
                viewState.isSingleDay -> startPixel + viewState.eventMarginHorizontal
                else -> startPixel
            }

            val eventChips = chipCache.allDayEventChipsByDate(date)
            for (eventChip in eventChips) {
                calculateTextLayout(viewState, eventChip, modifiedStartPixel)
            }
        }

        val maximumChipHeight = viewState.cache.allDayEventLayouts.keys
            .mapNotNull { it.bounds?.height }
            .max() ?: 0

        viewState.currentAllDayEventHeight = maximumChipHeight
    }

    private fun calculateTextLayout(
        viewState: WeekViewViewState<T>,
        eventChip: EventChip<T>,
        startPixel: Int
    ) {
        val chipRect = boundsCalculator.calculateAllDayEvent(viewState, eventChip, startPixel)
        val isValidEventBounds = chipRect.isValidAllDayEventBounds(viewState)
        eventChip.bounds = if (isValidEventBounds) chipRect else null

        if (isValidEventBounds) {
            val textLayout = calculateChipTextLayout(viewState, eventChip)
            if (textLayout != null) {
                viewState.cache.allDayEventLayouts[eventChip] = textLayout
            }
        }
    }

    private fun calculateChipTextLayout(
        viewState: WeekViewViewState<T>,
        eventChip: EventChip<T>
    ): StaticLayout? {
        val event = eventChip.event
        val bounds = checkNotNull(eventChip.bounds)

        val fullHorizontalPadding = viewState.eventPaddingHorizontal * 2
        val fullVerticalPadding = viewState.eventPaddingVertical * 2

        val availableWidth = bounds.width - fullHorizontalPadding
        val availableHeight = bounds.height - fullVerticalPadding

        if (availableHeight < 0) {
            return null
        }

        if (availableWidth < 0) {
            // This happens if there are many all-day events
            val dummyTextLayout = createDummyTextLayout(viewState, event)
            val chipHeight = dummyTextLayout.height + fullVerticalPadding
            bounds.bottom = bounds.top + chipHeight
            return dummyTextLayout
        }

        spannableStringBuilder.clear()
        spannableStringBuilder.append(event.title)

        event.location?.let { location ->
            spannableStringBuilder.append(" ")
            spannableStringBuilder.append(location)
        }

        val text = spannableStringBuilder.build()
        val textPaint = event.getTextPaint(viewState)

        val textLayout = text.toTextLayout(textPaint, availableWidth)
        val lineHeight = textLayout.height / textLayout.lineCount

        // For an all day event, we display just one line
        val chipHeight = lineHeight + fullVerticalPadding
        bounds.bottom = bounds.top + chipHeight

        return eventChip.ellipsizeText(
            viewState = viewState,
            text = text,
            availableWidth = availableWidth,
            existingTextLayout = textLayout
        )
    }

    /**
     * Creates a dummy text layout that is only used to determine the height of all-day events.
     */
    private fun createDummyTextLayout(
        viewState: WeekViewViewState<T>,
        event: ResolvedWeekViewEvent<T>
    ): StaticLayout {
        if (dummyTextLayout == null) {
            val textPaint = event.getTextPaint(viewState)
            dummyTextLayout = "".toTextLayout(textPaint, width = 0)
        }
        return checkNotNull(dummyTextLayout)
    }

    private fun EventChip<T>.ellipsizeText(
        viewState: WeekViewViewState<T>,
        text: CharSequence,
        availableWidth: Int,
        existingTextLayout: StaticLayout
    ): StaticLayout {
        val width = checkNotNull(bounds).width - (viewState.eventPaddingHorizontal * 2)
        val isTooSmallForText = width < 0

        if (isTooSmallForText) {
            // This day contains too many all-day events. We only draw the event chips,
            // but don't attempt to draw the event titles.
            return existingTextLayout
        }

        val textPaint = event.getTextPaint(viewState)
        return text
            .ellipsized(textPaint, availableWidth)
            .toTextLayout(textPaint, width)
    }

    private fun Rect.isValidAllDayEventBounds(
        viewState: WeekViewViewState<T>
    ): Boolean {
        return left < right &&
            left < viewState.headerBounds.right &&
            top < viewState.headerBounds.bottom &&
            right > viewState.timeColumnBounds.right &&
            bottom > 0
    }

    private fun CharSequence.ellipsized(
        textPaint: TextPaint,
        availableArea: Int,
        truncateAt: TextUtils.TruncateAt = END
    ): CharSequence = TextUtils.ellipsize(this, textPaint, availableArea.toFloat(), truncateAt)
}
