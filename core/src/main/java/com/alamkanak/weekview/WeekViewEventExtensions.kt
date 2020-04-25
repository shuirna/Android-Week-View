package com.alamkanak.weekview

import android.graphics.Paint
import android.text.TextPaint

internal fun <T> ResolvedWeekViewEvent<T>.getTextPaint(
    viewState: WeekViewViewState<T>
): TextPaint {
    val textPaint = if (isAllDay) {
        viewState.allDayEventTextPaint
    } else {
        viewState.eventTextPaint
    }

    textPaint.color = style.textColor ?: viewState.eventTextPaint.color

    if (style.isTextStrikeThrough) {
        textPaint.flags = textPaint.flags or Paint.STRIKE_THRU_TEXT_FLAG
    }

    return textPaint
}
