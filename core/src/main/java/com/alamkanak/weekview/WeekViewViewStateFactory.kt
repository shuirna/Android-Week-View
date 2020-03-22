package com.alamkanak.weekview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcelable
import android.util.AttributeSet
import kotlinx.android.parcel.Parcelize

private const val SANS = 1
private const val SERIF = 2
private const val MONOSPACE = 3

internal fun WeekViewViewState(
    context: Context,
    attrs: AttributeSet?
): WeekViewViewState {
    val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, 0, 0)
    return WeekViewViewState(
        // Calendar configuration
        firstDayOfWeek = a.getInt(R.styleable.WeekView_firstDayOfWeek, defaultValue = { now().firstDayOfWeek }),
        numberOfVisibleDays = a.getInteger(R.styleable.WeekView_numberOfVisibleDays, 3),
        restoreNumberOfVisibleDays = a.getBoolean(R.styleable.WeekView_restoreNumberOfVisibleDays, true),
        showFirstDayOfWeekFirst = a.getBoolean(R.styleable.WeekView_showFirstDayOfWeekFirst, false),
        showCurrentTimeFirst = a.getBoolean(R.styleable.WeekView_showCurrentTimeFirst, false),
        showHeaderRowBottomLine = a.getBoolean(R.styleable.WeekView_showHeaderRowBottomLine, false),
        headerRowBottomLineColor = a.getColor(R.styleable.WeekView_headerRowBottomLineColor, Defaults.GRID_COLOR),
        headerRowBottomLineWidth = a.getDimensionPixelSize(R.styleable.WeekView_headerRowBottomLineWidth, 1),

        // Time column
        timeColumnTextColor = a.getColor(R.styleable.WeekView_timeColumnTextColor, Color.BLACK),
        timeColumnBackgroundColor = a.getColor(R.styleable.WeekView_timeColumnBackgroundColor, Color.WHITE),
        timeColumnPadding = a.getDimensionPixelSize(R.styleable.WeekView_timeColumnPadding, 10),
        timeColumnTextSize = a.getDimensionPixelSize(R.styleable.WeekView_timeColumnTextSize, Defaults.textSize(context)),
        showMidnightHour = a.getBoolean(R.styleable.WeekView_showMidnightHour, false),
        showTimeColumnHourSeparator = a.getBoolean(R.styleable.WeekView_showTimeColumnHourSeparator, false),
        timeColumnHoursInterval = a.getInteger(R.styleable.WeekView_timeColumnHoursInterval, 1),

        // Time column separator
        showTimeColumnSeparator = a.getBoolean(R.styleable.WeekView_showTimeColumnSeparator, false),
        timeColumnSeparatorColor = a.getColor(R.styleable.WeekView_timeColumnSeparatorColor, Defaults.GRID_COLOR),
        timeColumnSeparatorStrokeWidth = a.getDimensionPixelSize(R.styleable.WeekView_timeColumnSeparatorStrokeWidth, 1),

        // Time range
        minHour = a.getInt(R.styleable.WeekView_minHour, 0),
        maxHour = a.getInt(R.styleable.WeekView_maxHour, 24),

        // Header row
        headerRowTextColor = a.getColor(R.styleable.WeekView_headerRowTextColor, Color.BLACK),
        headerRowBackgroundColor = a.getColor(R.styleable.WeekView_headerRowBackgroundColor, Color.WHITE),
        headerRowTextSize = a.getDimensionPixelSize(R.styleable.WeekView_headerRowTextSize, Defaults.textSize(context)),
        headerRowPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerRowPadding, 10),
        todayHeaderTextColor = a.getColor(R.styleable.WeekView_todayHeaderTextColor, Defaults.HIGHLIGHT_COLOR),
        singleLineHeader = a.getBoolean(R.styleable.WeekView_singleLineHeader, true),

        // Event chips
        eventCornerRadius = a.getDimensionPixelSize(R.styleable.WeekView_eventCornerRadius, 0),
        eventTextSize = a.getDimensionPixelSize(R.styleable.WeekView_eventTextSize, Defaults.textSize(context)),
        adaptiveEventTextSize = a.getBoolean(R.styleable.WeekView_adaptiveEventTextSize, false),
        eventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, Color.BLACK),
        defaultEventColor = a.getColor(R.styleable.WeekView_defaultEventColor, Defaults.EVENT_COLOR),

        // Event padding
        eventPaddingHorizontal = a.getDimensionPixelSize(R.styleable.WeekView_eventPaddingHorizontal, 8),
        eventPaddingVertical = a.getDimensionPixelSize(R.styleable.WeekView_eventPaddingVertical, 8),

        // Event margins
        columnGap = a.getDimensionPixelSize(R.styleable.WeekView_columnGap, 10),
        overlappingEventGap = a.getDimensionPixelSize(R.styleable.WeekView_overlappingEventGap, 0),
        eventMarginVertical = a.getDimensionPixelSize(R.styleable.WeekView_eventMarginVertical, 2),
        eventMarginHorizontal = a.getDimensionPixelSize(R.styleable.WeekView_singleDayHorizontalMargin, 0),

        // Colors
        dayBackgroundColor = a.getColor(R.styleable.WeekView_dayBackgroundColor, Defaults.BACKGROUND_COLOR),
        todayBackgroundColor = a.getColor(R.styleable.WeekView_todayBackgroundColor, Defaults.BACKGROUND_COLOR),
        showDistinctPastFutureColor = a.getBoolean(R.styleable.WeekView_showDistinctPastFutureColor, false),
        showDistinctWeekendColor = a.getBoolean(R.styleable.WeekView_showDistinctWeekendColor, false),
        pastBackgroundColor = a.getColor(R.styleable.WeekView_pastBackgroundColor, Defaults.PAST_BACKGROUND_COLOR),
        futureBackgroundColor = a.getColor(R.styleable.WeekView_futureBackgroundColor, Defaults.FUTURE_BACKGROUND_COLOR),

        // Hour height
        hourHeight = a.getDimension(R.styleable.WeekView_hourHeight, 50f),
        minHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_minHourHeight, 0),
        maxHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_maxHourHeight, 400),
        showCompleteDay = a.getBoolean(R.styleable.WeekView_showCompleteDay, false),

        // Now line
        showNowLine = a.getBoolean(R.styleable.WeekView_showNowLine, false),
        nowLineColor = a.getColor(R.styleable.WeekView_nowLineColor, Defaults.NOW_COLOR),
        nowLineStrokeWidth = a.getDimensionPixelSize(R.styleable.WeekView_nowLineStrokeWidth, 5),

        // Now line dot
        showNowLineDot = a.getBoolean(R.styleable.WeekView_showNowLineDot, false),
        nowLineDotColor = a.getColor(R.styleable.WeekView_nowLineDotColor, Defaults.NOW_COLOR),
        nowLineDotRadius = a.getDimensionPixelSize(R.styleable.WeekView_nowLineDotRadius, 16),

        // Hour separators
        showHourSeparators = a.getBoolean(R.styleable.WeekView_showHourSeparator, true),
        hourSeparatorColor = a.getColor(R.styleable.WeekView_hourSeparatorColor, Defaults.SEPARATOR_COLOR),
        hourSeparatorStrokeWidth = a.getDimensionPixelSize(R.styleable.WeekView_hourSeparatorStrokeWidth, 2),

        // Day separators
        showDaySeparators = a.getBoolean(R.styleable.WeekView_showDaySeparator, true),
        daySeparatorColor = a.getColor(R.styleable.WeekView_daySeparatorColor, Defaults.SEPARATOR_COLOR),
        daySeparatorStrokeWidth = a.getDimensionPixelSize(R.styleable.WeekView_daySeparatorStrokeWidth, 2),

        // Scrolling
        horizontalScrollingEnabled = a.getBoolean(R.styleable.WeekView_horizontalScrollingEnabled, true),
        verticalScrollingEnabled = a.getBoolean(R.styleable.WeekView_verticalScrollingEnabled, true),

        // Typeface
        typefaceInfo = TypefaceInfo(
            fontFamily = a.getString(R.styleable.WeekView_fontFamily),
            typefaceIndex = a.getInteger(R.styleable.WeekView_typeface, 0),
            textStyle = a.getInteger(R.styleable.WeekView_textStyle, 0)
        )
    ).apply {
        effectiveMinHourHeight = minHourHeight
        allDayEventTextSize = a.getDimensionPixelSize(R.styleable.WeekView_allDayEventTextSize, eventTextSize)
        pastWeekendBackgroundColor = a.getColor(R.styleable.WeekView_pastWeekendBackgroundColor, pastBackgroundColor)
        futureWeekendBackgroundColor = a.getColor(R.styleable.WeekView_futureWeekendBackgroundColor, futureBackgroundColor)
    }
}

@Parcelize
internal data class TypefaceInfo(
    val fontFamily: String? = null,
    val typefaceIndex: Int = 0,
    val textStyle: Int = 0
) : Parcelable {
    fun toTypeface(): Typeface {
        return fontFamily?.let {
            Typeface.create(it, textStyle)
        } ?: when (typefaceIndex) {
            SANS -> Typeface.SANS_SERIF
            SERIF -> Typeface.SERIF
            MONOSPACE -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }
    }
}

private fun TypedArray.getInt(index: Int, defaultValue: () -> Int): Int {
    return if (hasValue(index)) {
        getInt(index, 0)
    } else {
        defaultValue()
    }
}
