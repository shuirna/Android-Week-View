package com.alamkanak.weekview

import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import com.alamkanak.weekview.Constants.UNINITIALIZED
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.android.parcel.IgnoredOnParcel

internal data class WeekViewViewState(
    // Calendar configuration
    var firstDayOfWeek: Int = Calendar.MONDAY,
    var numberOfVisibleDays: Int = 0,
    var restoreNumberOfVisibleDays: Boolean = true,
    var showFirstDayOfWeekFirst: Boolean = false,
    var showCurrentTimeFirst: Boolean = false,

    // Header bottom line
    var showHeaderRowBottomLine: Boolean = false,
    var headerRowBottomLineColor: Int = 0,
    var headerRowBottomLineWidth: Int = 0,

    // Time column
    var timeColumnTextColor: Int = 0,
    var timeColumnBackgroundColor: Int = 0,
    var timeColumnPadding: Int = 0,
    var timeColumnTextSize: Int = 0,
    var showMidnightHour: Boolean = false,
    var showTimeColumnHourSeparator: Boolean = false,
    var timeColumnHoursInterval: Int = 0,

    // Time column separator
    var showTimeColumnSeparator: Boolean = false,
    var timeColumnSeparatorColor: Int = 0,
    var timeColumnSeparatorStrokeWidth: Int = 0,

    // Header row
    var headerRowTextColor: Int = 0,
    var headerRowBackgroundColor: Int = 0,
    var headerRowTextSize: Int = 0,
    var headerRowPadding: Int = 0,
    var todayHeaderTextColor: Int = 0,
    var singleLineHeader: Boolean = false,

    // Event chips
    var eventCornerRadius: Int = 0,
    var eventTextSize: Int = 0,
    var adaptiveEventTextSize: Boolean = false,
    var eventTextColor: Int = 0,
    var eventPaddingHorizontal: Int = 0,
    var eventPaddingVertical: Int = 0,
    var defaultEventColor: Int = 0,
    var allDayEventTextSize: Int = 0,

    // Event margins
    var columnGap: Int = 0,
    var overlappingEventGap: Int = 0,
    var eventMarginVertical: Int = 0,
    var eventMarginHorizontal: Int = 0,

    // Colors
    var dayBackgroundColor: Int = 0,
    var todayBackgroundColor: Int = 0,
    var showDistinctWeekendColor: Boolean = false,
    var showDistinctPastFutureColor: Boolean = false,
    var pastBackgroundColor: Int = 0,
    var futureBackgroundColor: Int = 0,
    var pastWeekendBackgroundColor: Int = 0,
    var futureWeekendBackgroundColor: Int = 0,

    // Hour height
    var hourHeight: Float = 0f,
    var minHourHeight: Int = 0,
    var maxHourHeight: Int = 0,
    var effectiveMinHourHeight: Int = 0,
    var showCompleteDay: Boolean = false,

    // Now line
    var showNowLine: Boolean = false,
    var nowLineColor: Int = 0,
    var nowLineStrokeWidth: Int = 0,

    // Now line dot
    var showNowLineDot: Boolean = false,
    var nowLineDotColor: Int = 0,
    var nowLineDotRadius: Int = 0,

    // Hour separators
    var showHourSeparators: Boolean = false,
    var hourSeparatorColor: Int = 0,
    var hourSeparatorStrokeWidth: Int = 0,

    // Day separators
    var showDaySeparators: Boolean = false,
    var daySeparatorColor: Int = 0,
    var daySeparatorStrokeWidth: Int = 0,

    // Scrolling
    var verticalScrollingEnabled: Boolean = false,
    var horizontalScrollingEnabled: Boolean = false,

    // Time range
    var minHour: Int = 0,
    var maxHour: Int = 0,

    // Font
    var typeface: Typeface = Typeface.DEFAULT,

    // NEW
    var headerHeight: Float = 0f,

    var minDate: Calendar? = null,
    var maxDate: Calendar? = null,
    var goToDate: Calendar? = null,
    var goToHour: Int? = null,
    var firstVisibleDate: Calendar? = null,
    var lastVisibleDate: Calendar? = null,
    var hasEventsInHeader: Boolean = false,

    var height: Int = 0,
    var width: Int = 0,

    var x: Float = 0f,
    var y: Float = 0f,

    var timeColumnWidth: Float = UNINITIALIZED,
    var newHourHeight: Float = UNINITIALIZED,

    // Dates in the past have origin.x > 0, dates in the future have origin.x < 0
    var currentOrigin: PointF = PointF(0f, 0f),
    var widthPerDay: Float = 0f,

    var currentAllDayEventHeight: Int = 0,
    var timeTextWidth: Float = UNINITIALIZED,
    var timeTextHeight: Float = UNINITIALIZED,

    var headerTextHeight: Float = UNINITIALIZED,

    private var startPixel: Float = 0f,
    val startPixels: MutableList<Float> = mutableListOf(),
    val dateRange: MutableList<Calendar> = mutableListOf(),
    val dateRangeWithStartPixels: MutableList<Pair<Calendar, Float>> = mutableListOf(),

    var hasBeenInvalidated: Boolean = false
) {

    private fun calculateTimeColumnTextWidth(
        dateTimeInterpreter: DateTimeInterpreter
    ) = (0..hoursPerDay)
        .map { dateTimeInterpreter.interpretTime(it) }
        .map { timeTextPaint.measureText(it) }
        .max() ?: 0f

    @IgnoredOnParcel
    var timeTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        textSize = timeColumnTextSize.toFloat()
        color = timeColumnTextColor
        typeface = this@WeekViewViewState.typeface
    }

    @IgnoredOnParcel
    val headerTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = headerRowTextColor
        textAlign = Paint.Align.CENTER
        textSize = headerRowTextSize.toFloat()
        typeface = this@WeekViewViewState.typeface.toBold()
    }.also {
        headerTextHeight = it.descent() - it.ascent() // TODO
    }

    @IgnoredOnParcel
    val headerRowBottomLinePaint: Paint = Paint().apply {
        color = headerRowBottomLineColor
        strokeWidth = headerRowBottomLineWidth.toFloat()
    }

    @IgnoredOnParcel
    val todayHeaderTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = todayHeaderTextColor
        textAlign = Paint.Align.CENTER
        textSize = headerRowTextSize.toFloat()
        typeface = this@WeekViewViewState.typeface.toBold()
    }

    @IgnoredOnParcel
    val timeColumnBackgroundPaint: Paint = Paint().apply {
        color = timeColumnBackgroundColor
    }

    @IgnoredOnParcel
    val startHour: Int
        get() = if (showMidnightHour && showTimeColumnHourSeparator) {
            minHour
        } else {
            timeColumnHoursInterval
        }

    @IgnoredOnParcel
    val minX: Float
        get() {
            return maxDate?.let {
                val date = it - Days(numberOfVisibleDays - 1)
                getXOriginForDate(date)
            } ?: Float.NEGATIVE_INFINITY
        }

    @IgnoredOnParcel
    val maxX: Float
        get() = minDate?.let { getXOriginForDate(it) } ?: Float.POSITIVE_INFINITY

    private fun getXOriginForDate(date: Calendar): Float {
        return date.daysFromToday * totalDayWidth * -1f
    }

    fun refreshHeaderRowHeight(hasEventsInHeader: Boolean) {
        this.hasEventsInHeader = hasEventsInHeader
        refreshHeaderHeight()
    }

    fun refreshHeaderHeight() {
        headerHeight = headerRowPadding * 2 + headerTextHeight

        if (showHeaderRowBottomLine) {
            headerHeight += headerRowBottomLinePaint.strokeWidth
        }

        if (hasEventsInHeader) {
            headerHeight += currentAllDayEventHeight.toFloat()
        }

        if (showCompleteDay) {
            hourHeight = (height - headerHeight) / hoursPerDay
            newHourHeight = hourHeight
        }
    }

    val isSingleDay: Boolean
        get() = numberOfVisibleDays == 1

    val totalDayWidth: Float
        get() = widthPerDay + columnGap

    val minutesPerDay: Int
        get() = (hoursPerDay * Constants.MINUTES_PER_HOUR).toInt()

    fun updateAllDayEventHeight(height: Int) {
        currentAllDayEventHeight = height
        refreshHeaderHeight()
    }

    val allDayEventTextPaint: TextPaint
        get() = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            style = Paint.Style.FILL
            color = eventTextColor
            textSize = allDayEventTextSize.toFloat()
            typeface = typeface
        }

    @IgnoredOnParcel
    val headerBackgroundPaint: Paint = Paint().apply {
        color = headerRowBackgroundColor
    }

    @IgnoredOnParcel
    val dayBackgroundPaint: Paint = Paint().apply {
        color = dayBackgroundColor
    }

    @IgnoredOnParcel
    val hourSeparatorPaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = hourSeparatorStrokeWidth.toFloat()
        color = hourSeparatorColor
    }

    @IgnoredOnParcel
    val daySeparatorPaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = daySeparatorStrokeWidth.toFloat()
        color = daySeparatorColor
    }

    @IgnoredOnParcel
    val todayBackgroundPaint: Paint = Paint().apply {
        color = todayBackgroundColor
    }

    @IgnoredOnParcel
    val futureBackgroundPaint: Paint = Paint().apply {
        color = futureBackgroundColor
    }

    @IgnoredOnParcel
    val pastBackgroundPaint: Paint = Paint().apply {
        color = pastBackgroundColor
    }

    @IgnoredOnParcel
    val futureWeekendBackgroundPaint: Paint = Paint().apply {
        color = futureWeekendBackgroundColor
    }

    @IgnoredOnParcel
    val pastWeekendBackgroundPaint: Paint = Paint().apply {
        color = pastWeekendBackgroundColor
    }

    @IgnoredOnParcel
    val timeColumnSeparatorPaint: Paint = Paint().apply {
        color = timeColumnSeparatorColor
        strokeWidth = timeColumnSeparatorStrokeWidth.toFloat()
    }

    @IgnoredOnParcel
    val nowLinePaint: Paint = Paint().apply {
        strokeWidth = nowLineStrokeWidth.toFloat()
        color = nowLineColor
    }

    @IgnoredOnParcel
    val nowDotPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        strokeWidth = nowLineDotRadius.toFloat()
        color = nowLineDotColor
        isAntiAlias = true
    }

    val eventTextPaint: TextPaint
        get() = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            style = Paint.Style.FILL
            color = eventTextColor
            textSize = eventTextSize.toFloat()
            typeface = typeface
        }

    val hoursPerDay: Int
        get() = maxHour - minHour

    val timeRange: IntRange
        get() = minHour..maxHour

    val totalDayHeight: Float
        get() = (hourHeight * hoursPerDay) + headerHeight

    private fun Paint.getTextHeight(text: String): Float {
        val rect = Rect()
        getTextBounds(text, 0, text.length, rect)
        return rect.height().toFloat()
    }

    fun updateTimeColumnText(dateTimeInterpreter: DateTimeInterpreter) {
        timeTextWidth = calculateTimeColumnTextWidth(dateTimeInterpreter)
        timeTextHeight = calculateTimeTextHeight(dateTimeInterpreter)
    }

    private fun calculateTimeTextHeight(dateTimeInterpreter: DateTimeInterpreter): Float {
        val formattedTime = dateTimeInterpreter.interpretTime(0)
        return timeTextPaint.getTextHeight(formattedTime)
    }

    private fun refreshAfterZooming() {
        if (showCompleteDay) {
            return
        }

        val dayHeight = hourHeight * hoursPerDay

        val isNotFillingEntireHeight = dayHeight < height
        val didZoom = newHourHeight > 0

        if (isNotFillingEntireHeight || didZoom) {
            newHourHeight = max(newHourHeight, effectiveMinHourHeight.toFloat())
            newHourHeight = min(newHourHeight, maxHourHeight.toFloat())

            // Compute a minimum hour height so that users can't zoom out further
            // than the desired hours per day
            val minHourHeight = (height - headerHeight) / hoursPerDay
            newHourHeight = max(newHourHeight, minHourHeight)

            currentOrigin.y = currentOrigin.y / hourHeight * newHourHeight
            hourHeight = newHourHeight
            newHourHeight = UNINITIALIZED
        }
    }

    private fun updateVerticalOrigin() {
        // If the new currentOrigin.y is invalid, make it valid.
        val dayHeight = hourHeight * hoursPerDay
        val potentialNewVerticalOrigin = height - (dayHeight + headerHeight)
        currentOrigin.y = max(currentOrigin.y, potentialNewVerticalOrigin)
        currentOrigin.y = min(currentOrigin.y, 0f)
    }

    private fun calculateTimeColumnWidth(dateTimeInterpreter: DateTimeInterpreter): Float {
        if (timeTextWidth == UNINITIALIZED) {
            timeTextWidth = calculateTimeColumnTextWidth(dateTimeInterpreter)
        }
        return timeTextWidth + timeColumnPadding * 2
    }

    fun onSizeChanged(width: Int, height: Int, dateTimeInterpreter: DateTimeInterpreter) {
        this.width = width
        this.height = height

        if (timeColumnWidth == UNINITIALIZED) {
            timeColumnWidth = calculateTimeColumnWidth(dateTimeInterpreter)
        }

        calculateWidthPerDay(dateTimeInterpreter)

        if (showCompleteDay) {
            updateHourHeight(height)
        }
    }

    fun update() {
        updateMinHourHeight()
        refreshAfterZooming()
        updateVerticalOrigin()
        updateDateRangeAndStartPixels()
    }

    private fun updateMinHourHeight() {
        val totalHeaderHeight = getTotalHeaderHeight().toInt()
        val dynamicHourHeight = (height - totalHeaderHeight) / hoursPerDay
        effectiveMinHourHeight = max(minHourHeight, dynamicHourHeight)
    }

    private fun updateDateRangeAndStartPixels() {
        val originX = currentOrigin.x

        val daysFromOrigin = ceil(originX / totalDayWidth).toInt() * (-1)
        startPixel = timeColumnWidth + originX + totalDayWidth * daysFromOrigin

        val start = daysFromOrigin + 1
        val end = start + numberOfVisibleDays

        // If the user is scrolling, a new view becomes partially visible, so we must add an
        // additional date to the date range
        val isNotScrolling = originX % totalDayWidth == 0f
        val modifiedEnd = if (isNotScrolling) end - 1 else end

        dateRange.clear()
        dateRange += getDateRange(start, modifiedEnd)

        updateStartPixels()

        dateRangeWithStartPixels.clear()
        dateRangeWithStartPixels += dateRange.zip(startPixels)
    }

    private fun updateStartPixels() {
        startPixels.clear()
        startPixels += dateRange.indices.map {
            index -> startPixel + index * totalDayWidth
        }
    }

    fun computeDifferenceWithFirstDayOfWeek(
        date: Calendar
    ): Int {
        val firstDayOfWeek = firstDayOfWeek
        return if (firstDayOfWeek == Calendar.MONDAY && date.dayOfWeek == Calendar.SUNDAY) {
            // Special case, because Calendar.MONDAY has constant value 2 and Calendar.SUNDAY has
            // constant value 1. The correct result to return is 6 days, not -1 days.
            6
        } else {
            date.dayOfWeek - firstDayOfWeek
        }
    }

    fun calculateWidthPerDay(dateTimeInterpreter: DateTimeInterpreter) {
        if (timeColumnWidth == UNINITIALIZED) {
            timeColumnWidth = calculateTimeColumnWidth(dateTimeInterpreter)
        }

        val availableWidth = width.toFloat() - timeColumnWidth - columnGap * numberOfVisibleDays
        widthPerDay = availableWidth / numberOfVisibleDays
    }

    fun getTotalHeaderHeight(): Float {
        return headerHeight + headerRowPadding * 2f
    }

    private fun updateHourHeight(viewHeight: Int) {
        hourHeight = (viewHeight - headerHeight) / hoursPerDay
        newHourHeight = hourHeight
    }

    fun invalidate() {
        hasBeenInvalidated = true
    }
}

private fun Typeface.toBold(): Typeface {
    return Typeface.create(this, Typeface.BOLD)
}
