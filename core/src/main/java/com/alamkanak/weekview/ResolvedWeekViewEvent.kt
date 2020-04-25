package com.alamkanak.weekview

import android.content.Context
import android.text.SpannableString
import androidx.core.content.ContextCompat
import java.util.Calendar
import kotlin.math.roundToInt

internal interface ResourceResolver {
    fun resolve(colorResource: WeekViewEvent.ColorResource?): Int?
    fun resolve(dimenResource: WeekViewEvent.DimenResource?): Int?
    fun resolve(style: WeekViewEvent.Style): ResolvedWeekViewEvent.Style
    fun resolve(textResource: WeekViewEvent.TextResource?): SpannableString?
}

internal class RealResourceResolver(
    private val context: Context
) : ResourceResolver {

    override fun resolve(
        colorResource: WeekViewEvent.ColorResource?
    ): Int? = when (colorResource) {
        is WeekViewEvent.ColorResource.Id -> ContextCompat.getColor(context, colorResource.resId)
        is WeekViewEvent.ColorResource.Value -> colorResource.color
        else -> null
    }

    override fun resolve(
        dimenResource: WeekViewEvent.DimenResource?
    ): Int? = when (dimenResource) {
        is WeekViewEvent.DimenResource.Id -> context.resources.getDimensionPixelSize(dimenResource.resId)
        is WeekViewEvent.DimenResource.Value -> dimenResource.value
        else -> null
    }

    override fun resolve(style: WeekViewEvent.Style): ResolvedWeekViewEvent.Style {
        return ResolvedWeekViewEvent.Style(
            backgroundColor = resolve(style.backgroundColorResource),
            textColor = resolve(style.textColorResource),
            isTextStrikeThrough = style.isTextStrikeThrough,
            borderWidth = resolve(style.borderWidthResource),
            borderColor = resolve(style.borderColorResource)
        )
    }

    override fun resolve(
        textResource: WeekViewEvent.TextResource?
    ): SpannableString? = when (textResource) {
        is WeekViewEvent.TextResource.Id -> SpannableString(context.getString(textResource.resId)).emojify()
        is WeekViewEvent.TextResource.Value -> textResource.text.emojify()
        else -> null
    }
}

data class ResolvedWeekViewEvent<T> internal constructor(
    val id: Long = 0L,
    internal val title: SpannableString,
    val startTime: Calendar = now(),
    val endTime: Calendar = now(),
    internal val location: SpannableString? = null,
    val isAllDay: Boolean = false,
    val style: Style = Style(),
    val data: T
) {

    internal val isNotAllDay: Boolean
        get() = isAllDay.not()

    internal val durationInMinutes: Int =
        ((endTime.timeInMillis - startTime.timeInMillis).toFloat() / 60_000).roundToInt()

    internal val isMultiDay: Boolean = startTime.isSameDate(endTime).not()

    internal fun isWithin(
        minHour: Int,
        maxHour: Int
    ): Boolean = startTime.hour >= minHour && endTime.hour <= maxHour

    internal fun collidesWith(other: ResolvedWeekViewEvent<T>): Boolean {
        if (isAllDay != other.isAllDay) {
            return false
        }

        if (startTime == other.startTime && endTime == other.endTime) {
            // Complete overlap
            return true
        }

        // Resolve collisions by shortening the preceding event by 1 ms
        if (endTime == other.startTime) {
            endTime -= Millis(1)
            return false
        } else if (startTime == other.endTime) {
            other.endTime -= Millis(1)
        }

        return startTime <= other.endTime && endTime >= other.startTime
    }

    data class Style(
        val backgroundColor: Int? = null,
        val textColor: Int? = null,
        val isTextStrikeThrough: Boolean = false,
        val borderWidth: Int? = null,
        val borderColor: Int? = null
    )
}
