package com.alamkanak.weekview.sample.data.model

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import com.alamkanak.weekview.WeekViewDisplayable
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.sample.R
import java.util.Calendar

class Event(
    val id: Long,
    val title: String,
    private val startTime: Calendar,
    private val endTime: Calendar,
    private val location: String,
    private val textColor: Int,
    private val backgroundColor: Int,
    private val isAllDay: Boolean,
    private val isCanceled: Boolean
) : WeekViewDisplayable<Event> {

    override fun toWeekViewEvent(): WeekViewEvent<Event> {
        val borderWidthResId = if (!isCanceled) R.dimen.no_border_width else R.dimen.border_width

        val style = WeekViewEvent.Style.Builder()
            .setTextColor(textColor)
            .setBackgroundColor(backgroundColor)
            .setTextStrikeThrough(isCanceled)
            .setBorderWidthResource(borderWidthResId)
            .setBorderColor(if (isCanceled) textColor else backgroundColor)
            .build()

        val spannable = SpannableString(title).apply {
            setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, title.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return WeekViewEvent.Builder(this)
            .setId(id)
            .setTitle(spannable)
            .setStartTime(startTime)
            .setEndTime(endTime)
            .setLocation(location)
            .setAllDay(isAllDay)
            .setStyle(style)
            .build()
    }
}
