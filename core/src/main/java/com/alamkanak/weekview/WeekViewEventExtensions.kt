package com.alamkanak.weekview

import android.content.Context
import android.graphics.Paint
import android.text.TextPaint
import androidx.core.content.ContextCompat

internal fun <T> WeekViewEvent<T>.getTextPaint(
    context: Context,
    viewState: WeekViewViewState
): TextPaint {
    val textPaint = if (isAllDay) {
        viewState.allDayEventTextPaint
    } else {
        viewState.eventTextPaint
    }

    textPaint.color = when (val resource = style.textColorResource) {
        is WeekViewEvent.ColorResource.Id -> ContextCompat.getColor(context, resource.resId)
        is WeekViewEvent.ColorResource.Value -> resource.color
        null -> viewState.eventTextPaint.color
    }

    if (style.isTextStrikeThrough) {
        textPaint.flags = textPaint.flags or Paint.STRIKE_THRU_TEXT_FLAG
    }

    return textPaint
}
