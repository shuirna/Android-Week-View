package com.alamkanak.weekview

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextUtils.TruncateAt.END

internal class AllDayEventsUpdater<T : Any>(
    private val context: Context,
    private val cache: WeekViewCache<T>,
    private val chipCache: EventChipCache<T>
) : Updater<T> {

    private val rectCalculator = EventChipRectCalculator<T>()
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
        cache.allDayEventLayouts.clear()

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

        val maximumChipHeight = cache.allDayEventLayouts.keys
            .mapNotNull { it.bounds?.height }
            .max() ?: 0

        viewState.updateAllDayEventHeight(maximumChipHeight)
    }

    private fun calculateTextLayout(
        viewState: WeekViewViewState<T>,
        eventChip: EventChip<T>,
        startPixel: Int
    ) {
        val chipRect = rectCalculator.calculateAllDayEvent(viewState, eventChip, startPixel)
        val isValidEventBounds = chipRect.isValidAllDayEventBounds(viewState)
        eventChip.bounds = if (isValidEventBounds) chipRect else null

        if (isValidEventBounds) {
            val textLayout = calculateChipTextLayout(viewState, eventChip)
            if (textLayout != null) {
                cache.allDayEventLayouts[eventChip] = textLayout
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
        val title = event.titleResource.toSpannableString(context)
        spannableStringBuilder.append(title)

        val location = event.locationResource?.toSpannableString(context)
        if (location != null) {
            spannableStringBuilder.append(" ")
            spannableStringBuilder.append(location)
        }

        val text = spannableStringBuilder.build()

//        val modifiedTitle = title.emojify()
//        val text = SpannableStringBuilder(modifiedTitle)
//        text.setSpan(StyleSpan(Typeface.BOLD))

//        val location = when (val resource = event.locationResource) {
//            is TextResource.Id -> context.getString(resource.resId)
//            is TextResource.Value -> resource.text
//            null -> null
//        }

//        if (location != null) {
//            val modifiedLocation = location.emojify()
//            text.append(' ').append(modifiedLocation)
//        }

        val textPaint = event.getTextPaint(context, viewState)
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
        event: WeekViewEvent<T>
    ): StaticLayout {
        if (dummyTextLayout == null) {
            val textPaint = event.getTextPaint(context, viewState)
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
        val textPaint = event.getTextPaint(context, viewState)
        val width = checkNotNull(bounds).width - (viewState.eventPaddingHorizontal * 2)

        val ellipsized = text.ellipsized(textPaint, availableWidth)
        val isTooSmallForText = width < 0
        if (isTooSmallForText) {
            // This day contains too many all-day events. We only draw the event chips,
            // but don't attempt to draw the event titles.
            return existingTextLayout
        }

        return ellipsized.toTextLayout(textPaint, width)
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

    private operator fun RectF.component1() = left

    private operator fun RectF.component2() = top

    private operator fun RectF.component3() = right

    private operator fun RectF.component4() = bottom

    private fun CharSequence.ellipsized(
        textPaint: TextPaint,
        availableArea: Int,
        truncateAt: TextUtils.TruncateAt = END
    ): CharSequence = TextUtils.ellipsize(this, textPaint, availableArea.toFloat(), truncateAt)
}
