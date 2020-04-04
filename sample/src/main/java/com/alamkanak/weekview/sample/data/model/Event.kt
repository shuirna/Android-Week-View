package com.alamkanak.weekview.sample.data.model

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

        return WeekViewEvent.Builder<Event>(this)
            .setId(id)
            .setTitle(title)
            .setStartTime(startTime)
            .setEndTime(endTime)
            .setLocation(location)
            .setAllDay(isAllDay)
            .setStyle(style)
            .build()
    }
}
