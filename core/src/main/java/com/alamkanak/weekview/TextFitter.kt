package com.alamkanak.weekview

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.StaticLayout

internal class TextFitter<T>(
    private val viewState: WeekViewViewState<T>
) {

    private val spannableStringBuilder = SpannableStringBuilder()

    fun fit(
        eventChip: EventChip<T>,
        title: SpannableString,
        location: SpannableString?,
        chipHeight: Int,
        chipWidth: Int
    ): StaticLayout {
        val text = combineTitleAndLocation(title, location, isMultiLine = true)
        val textPaint = eventChip.event.getTextPaint(viewState)
        val textLayout = text.toTextLayout(textPaint, width = chipWidth)

        val fitsIntoChip = chipHeight >= textLayout.height
        if (fitsIntoChip) {
            return ellipsize(eventChip, textLayout, text, chipHeight, chipWidth)
        }

        val modifiedText = combineTitleAndLocation(title, location, isMultiLine = false)
        val modifiedTextLayout = text.toTextLayout(textPaint, chipWidth)

        val fitsIntoChipNow = chipHeight >= modifiedTextLayout.height
        val isAdaptive = viewState.adaptiveEventTextSize

        return when {
            fitsIntoChipNow || !isAdaptive -> {
                ellipsize(eventChip, modifiedTextLayout, modifiedText, chipHeight, chipWidth)
            }
            isAdaptive -> scaleToFit(eventChip, modifiedText, chipHeight)
            else -> modifiedTextLayout
        }
    }

    private fun combineTitleAndLocation(
        title: SpannableString,
        location: SpannableString?,
        isMultiLine: Boolean
    ): SpannableString = when (location) {
        null -> title
        else -> {
            val separator = if (isMultiLine) "\n" else " "
            spannableStringBuilder.clear()
            spannableStringBuilder
                .append(title)
                .append(separator)
                .append(location)
                .build()
        }
    }

    private fun ellipsize(
        eventChip: EventChip<T>,
        textLayout: StaticLayout,
        text: SpannableString,
        availableHeight: Int,
        availableWidth: Int
    ): StaticLayout {
        val event = eventChip.event
        val rect = checkNotNull(eventChip.bounds)

        // The text fits into the chip, so we just need to ellipsize it
        var newTextLayout = textLayout
        val textPaint = event.getTextPaint(viewState)

        var availableLineCount = availableHeight / newTextLayout.lineHeight
        val fullHorizontalPadding = viewState.eventPaddingHorizontal * 2f
        val width = (rect.right - rect.left - fullHorizontalPadding).toInt()

        do {
            // Ellipsize text to fit into event rect
            val availableArea = availableLineCount * availableWidth
            val ellipsized = text.ellipsized(textPaint, availableArea)
            newTextLayout = ellipsized.toTextLayout(textPaint, width)
            availableLineCount -= 1
        } while (newTextLayout.height > availableHeight && availableLineCount > 0)

        return newTextLayout
    }

    private fun scaleToFit(
        eventChip: EventChip<T>,
        text: SpannableString,
        availableHeight: Int
    ): StaticLayout {
        val event = eventChip.event
        val rect = checkNotNull(eventChip.bounds)

        val textPaint = event.getTextPaint(viewState)
        val fullHorizontalPadding = viewState.eventPaddingHorizontal * 2f
        val width = (rect.right - rect.left - fullHorizontalPadding).toInt()

        var textLayout: StaticLayout

        do {
            // The text doesn't fit into the chip, so we need to gradually
            // reduce its size until it does
            textPaint.textSize -= 1
            textLayout = text.toTextLayout(textPaint, width)
        } while (availableHeight < textLayout.height)

        return textLayout
    }
}
