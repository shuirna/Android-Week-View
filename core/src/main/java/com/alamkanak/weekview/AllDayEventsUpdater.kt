package com.alamkanak.weekview

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextUtils.TruncateAt.END
import android.text.style.StyleSpan
import com.alamkanak.weekview.WeekViewEvent.TextResource
import kotlin.math.roundToInt

internal class AllDayEventsUpdater<T : Any>(
    private val context: Context,
    private val cache: WeekViewCache<T>,
    private val chipCache: EventChipCache<T>,
    private val emojiTextProcessor: EmojiTextProcessor = EmojiTextProcessor()
) : Updater {

    private val rectCalculator = EventChipRectCalculator<T>()

    private var previousHorizontalOrigin: Float? = null
    private var dummyTextLayout: StaticLayout? = null

    override fun isRequired(viewState: WeekViewViewState): Boolean {
        val didScrollHorizontally = previousHorizontalOrigin != viewState.currentOrigin.x
        val dateRange = viewState.dateRange
        val containsNewChips = chipCache.allDayEventChipsInDateRange(dateRange).any { it.bounds == null }
        return didScrollHorizontally || containsNewChips
    }

    override fun update(viewState: WeekViewViewState) {
        cache.clearAllDayEventLayouts()

        val datesWithStartPixels = viewState.dateRangeWithStartPixels
        for ((date, startPixel) in datesWithStartPixels) {
            // If we use a horizontal margin in the day view, we need to offset the start pixel.
            val modifiedStartPixel = when {
                viewState.isSingleDay -> startPixel + viewState.eventMarginHorizontal.toFloat()
                else -> startPixel
            }

            val eventChips = chipCache.allDayEventChipsByDate(date)
            for (eventChip in eventChips) {
                calculateTextLayout(viewState, eventChip, modifiedStartPixel)
            }
        }

        val maximumChipHeight = cache.allDayEventLayouts.keys
            .mapNotNull { it.bounds }
            .map { it.height().roundToInt() }
            .max() ?: 0

        viewState.updateAllDayEventHeight(maximumChipHeight)
    }

    private fun calculateTextLayout(
        viewState: WeekViewViewState,
        eventChip: EventChip<T>,
        startPixel: Float
    ) {
        val chipRect = rectCalculator.calculateAllDayEvent(viewState, eventChip, startPixel)
        val isValidEventBounds = chipRect.isValidEventBounds(viewState)
        eventChip.bounds = if (isValidEventBounds) chipRect else null

        if (isValidEventBounds) {
            val textLayout = calculateChipTextLayout(viewState, eventChip)
            textLayout?.let { layout ->
                cache.allDayEventLayouts[eventChip] = layout
            }
        }
    }

    private fun calculateChipTextLayout(
        viewState: WeekViewViewState,
        eventChip: EventChip<T>
    ): StaticLayout? {
        val event = eventChip.event
        val bounds = checkNotNull(eventChip.bounds)

        val fullHorizontalPadding = viewState.eventPaddingHorizontal * 2
        val fullVerticalPadding = viewState.eventPaddingVertical * 2

        val width = bounds.width() - fullHorizontalPadding
        val height = bounds.height() - fullVerticalPadding

        if (height < 0) {
            return null
        }

        if (width < 0) {
            // This happens if there are many all-day events
            val dummyTextLayout = createDummyTextLayout(viewState, event)
            val chipHeight = dummyTextLayout.height + fullVerticalPadding
            bounds.bottom = bounds.top + chipHeight
            return dummyTextLayout
        }

        val title = when (val resource = event.titleResource) {
            is TextResource.Id -> context.getString(resource.resId)
            is TextResource.Value -> resource.text
            null -> ""
        }

        val modifiedTitle = emojiTextProcessor.process(title)
        val text = SpannableStringBuilder(modifiedTitle)
        text.setSpan(StyleSpan(Typeface.BOLD))

        val location = when (val resource = event.locationResource) {
            is TextResource.Id -> context.getString(resource.resId)
            is TextResource.Value -> resource.text
            null -> null
        }

        if (location != null) {
            val modifiedLocation = emojiTextProcessor.process(location)
            text.append(' ').append(modifiedLocation)
        }

        val availableWidth = width.toInt()

        val textPaint = event.getTextPaint(context, viewState)
        val textLayout = TextLayoutBuilder.build(text, textPaint, availableWidth)
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
        viewState: WeekViewViewState,
        event: WeekViewEvent<T>
    ): StaticLayout {
        if (dummyTextLayout == null) {
            val textPaint = event.getTextPaint(context, viewState)
            dummyTextLayout = TextLayoutBuilder.build("", textPaint, width = 0)
        }
        return checkNotNull(dummyTextLayout)
    }

    private fun EventChip<T>.ellipsizeText(
        viewState: WeekViewViewState,
        text: CharSequence,
        availableWidth: Int,
        existingTextLayout: StaticLayout
    ): StaticLayout {
        val textPaint = event.getTextPaint(context, viewState)
        val bounds = checkNotNull(bounds)
        val width = bounds.width().roundToInt() - (viewState.eventPaddingHorizontal * 2)

        val ellipsized = text.ellipsized(textPaint, availableWidth)
        val isTooSmallForText = width < 0
        if (isTooSmallForText) {
            // This day contains too many all-day events. We only draw the event chips,
            // but don't attempt to draw the event titles.
            return existingTextLayout
        }

        return TextLayoutBuilder.build(ellipsized, textPaint, width)
    }

    private fun RectF.isValidEventBounds(
        viewState: WeekViewViewState
    ): Boolean {
        return left < right &&
            left < viewState.width &&
            top < viewState.height &&
            right > viewState.timeColumnWidth &&
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
