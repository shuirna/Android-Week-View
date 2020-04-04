package com.alamkanak.weekview

import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.SparseArray
import androidx.collection.ArrayMap
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.android.parcel.IgnoredOnParcel

internal data class WeekViewViewState<T>(
    // Calendar configuration
    var firstDayOfWeek: Int = Calendar.MONDAY,
    private var _numberOfVisibleDays: Int = 0,
    var restoreNumberOfVisibleDays: Boolean = true,
    var showFirstDayOfWeekFirst: Boolean = false,
    var showCurrentTimeFirst: Boolean = false,
    var isFirstDraw: Boolean = true,

    // Header bottom line
    var showHeaderRowBottomLine: Boolean = false,
    var headerRowBottomLineColor: Int = 0,
    var headerRowBottomLineWidth: Int = 0,

    // Week number
    var showWeekNumber: Boolean = false,
    var weekNumberTextColor: Int = 0,
    var weekNumberTextSize: Int = 0,
    var weekNumberBackgroundColor: Int = 0,
    var weekNumberBackgroundCornerRadius: Int = 0,

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
    var hourHeight: Int = 0,
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
    // var headerHeight: Int = 0,

    var minDate: Calendar? = null,
    var maxDate: Calendar? = null,
    var goToDate: Calendar? = null,
    var goToHour: Int? = null,
    var firstVisibleDate: Calendar? = null,
    var lastVisibleDate: Calendar? = null,
    var hasEventsInHeader: Boolean = false,

//    var height: Int = 0,
//    var width: Int = 0,

    private var _bounds: Rect = Rect(-1, -1, -1, -1),
    private var _timeColumnBounds: Rect = Rect(-1, -1, -1, -1),
    private var _calendarAreaBounds: Rect = Rect(-1, -1, -1, -1),
    private var _headerBounds: Rect = Rect(-1, -1, -1, -1),

    var x: Int = 0,
    var y: Int = 0,

    var newHourHeight: Int? = null,

    // Dates in the past have origin.x > 0, dates in the future have origin.x < 0
    var currentOrigin: Point = Point(0, 0),
    // var drawableWidthPerDay: Float = 0f,
    // var drawableWidthPerDay: Int = 0,

    var currentAllDayEventHeight: Int = 0,
    var timeTextWidth: Int? = null, // TODO Must be set at some point

    private var _headerTextHeight: Int? = null,

    val startPixels: MutableList<Int> = mutableListOf(),
    val dateRange: MutableList<Calendar> = mutableListOf(),
    val dateRangeWithStartPixels: MutableList<Pair<Calendar, Int>> = mutableListOf(),

    var hasBeenInvalidated: Boolean = false,

    val cache: Cache<T> = Cache()
) {

    val bounds: Rect
        get() = _bounds

    val timeColumnBounds: Rect
        get() = _timeColumnBounds

    val calendarAreaBounds: Rect
        get() = _calendarAreaBounds

    val headerBounds: Rect
        get() = _headerBounds

    val weekNumberBounds: Rect
        get() = Rect(
            left = 0,
            top = 0,
            right = timeColumnBounds.right,
            bottom = headerBounds.bottom
        )

    val weekNumberTextPaint: Paint
        get() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = weekNumberTextColor
            textAlign = Paint.Align.CENTER
            textSize = weekNumberTextSize.toFloat()
            typeface = this@WeekViewViewState.typeface
        }

    val weekNumberBackgroundPaint: Paint
        get() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = weekNumberBackgroundColor
        }

    val timeTextHeight: Int
        get() = timeTextPaint.textHeight

    val drawableWidthPerDay: Int
        get() {
            val availableWidth = calendarAreaBounds.width - columnGap * numberOfVisibleDays
            return (availableWidth / numberOfVisibleDays.toFloat()).roundToInt()
        }

    val timeColumnWidth: Int
        get() = _timeColumnBounds.width

    private var _dateFormatter: (Calendar) -> String = { calendar ->
        defaultTimeFormatter().format(calendar.time)
    }

    var dateFormatter: (Calendar) -> String
        get() = _dateFormatter
        set(value) {
            _dateFormatter = value
            onDateFormatterUpdated()
        }

    private var _timeFormatter: (Int) -> String = { hour ->
        val date = now().withTime(hour = hour, minutes = 0)
        defaultTimeFormatter().format(date.time)
    }

    var timeFormatter: (Int) -> String
        get() = _timeFormatter
        set(value) {
            _timeFormatter = value
            onTimeFormatterUpdated()
        }

    var numberOfVisibleDays: Int
        get() = _numberOfVisibleDays
        set(value) {
            // todo not really needed: onSizeChanged(bounds = bounds)
            // Scroll to first visible day after changing the number of visible days
            _numberOfVisibleDays = value
            goToDate = firstVisibleDate
        }

    @IgnoredOnParcel
    var timeTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        textSize = timeColumnTextSize.toFloat()
        color = timeColumnTextColor
        typeface = this@WeekViewViewState.typeface
    }

    init {
        // timeTextHeight = timeTextPaint.textHeight
        timeTextWidth = calculateTimeColumnTextWidth()
    }

    private fun calculateTimeColumnTextWidth() = (0..hoursPerDay)
        .map { timeFormatter(it) }
        .map { timeTextPaint.measureText(it).roundToInt() }
        .max() ?: 0

    @IgnoredOnParcel
    val headerTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = headerRowTextColor
        textAlign = Paint.Align.CENTER
        textSize = headerRowTextSize.toFloat()
        typeface = this@WeekViewViewState.typeface.toBold()
    }
        // .also {
        //   headerTextHeight = (it.descent() - it.ascent()).roundToInt()
        // }

    var headerTextHeight: Int
        get() {
            return _headerTextHeight ?: headerTextPaint.textHeight
        }
        set(value) {
            _headerTextHeight = value
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
            max(minHour, timeColumnHoursInterval)
        }

    @IgnoredOnParcel
    val minX: Int
        get() {
            return maxDate?.let {
                val date = it - Days(numberOfVisibleDays - 1)
                date.xOrigin
            } ?: Int.MIN_VALUE
        }

    @IgnoredOnParcel
    val maxX: Int
        get() = minDate?.xOrigin ?: Int.MAX_VALUE

    private val Calendar.xOrigin: Int
        get() = daysFromToday * widthPerDay * (-1)

    fun getXOriginForDate(date: Calendar): Int {
        return date.daysFromToday * widthPerDay * -1
    }

//    fun refreshHeaderRowHeight(hasEventsInHeader: Boolean) {
//        this.hasEventsInHeader = hasEventsInHeader
//        refreshHeaderHeight()
//    }

    // TODO Deprecate?
    fun refreshHeaderHeight() {
        val headerHeight = calculateHeaderHeight()
        _headerBounds = _headerBounds.copy(bottom = headerBounds.top + headerHeight)

        updateTimeColumnBounds()
        updateCalendarAreaBounds()

        if (showCompleteDay) {
            val contentHeight = bounds.height - headerHeight
            hourHeight = (contentHeight / hoursPerDay.toFloat()).roundToInt()
            newHourHeight = hourHeight
        }
    }

    private fun calculateHeaderHeight(): Int {
        var headerHeight = headerTextHeight + headerRowPadding * 2

        if (showHeaderRowBottomLine) {
            headerHeight += headerRowBottomLinePaint.strokeWidth.roundToInt()
        }

        if (hasEventsInHeader) {
            headerHeight += currentAllDayEventHeight
        }

        return headerHeight
    }

    val isSingleDay: Boolean
        get() = numberOfVisibleDays == 1

    val widthPerDay: Int
        get() = drawableWidthPerDay + columnGap

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

    @Deprecated("")
    val totalDayHeight: Int
        get() = (hourHeight * hoursPerDay) + headerBounds.height

    private fun onDateFormatterUpdated() {
        // Calculate header bounds
        val headerHeight = calculateHeaderHeight()
        _headerBounds = bounds.copy(
            bottom = bounds.top + headerHeight
        )

        _timeColumnBounds = _timeColumnBounds.copy(
            top = headerHeight
        )

        _calendarAreaBounds = _calendarAreaBounds.copy(
            top = headerHeight
        )
    }

    private fun onTimeFormatterUpdated() {
        val headerHeight = headerBounds.height

        // TODO Calculate time column text height when paint is updated

        // Time column
        val textWidth = calculateTimeColumnTextWidth()
        val columnWidth = textWidth + timeColumnPadding * 2
        _timeColumnBounds = bounds.copy(
            top = headerHeight,
            right = bounds.left + columnWidth
        )

        _headerBounds = _headerBounds.copy(
            left = _timeColumnBounds.width // TODO + 1
        )

        // Calendar area
        _calendarAreaBounds = bounds.copy(
            left = columnWidth + 1,
            top = headerHeight + 1
        )
    }

    private fun refreshAfterZooming() {
        if (showCompleteDay) {
            return
        }

        val dayHeight = hourHeight * hoursPerDay

        val isNotFillingEntireHeight = dayHeight < bounds.height
        val didZoom = newHourHeight != null

        if (isNotFillingEntireHeight || didZoom) {
            // val newHourHeight = checkNotNull(newHourHeight)
            newHourHeight = max(checkNotNull(newHourHeight), effectiveMinHourHeight)
            newHourHeight = min(checkNotNull(newHourHeight), maxHourHeight)

            // Compute a minimum hour height so that users can't zoom out further
            // than the desired hours per day
            val minHourHeight = (bounds.height - headerBounds.height) / hoursPerDay
            newHourHeight = max(checkNotNull(newHourHeight), minHourHeight)

            currentOrigin.y = currentOrigin.y / hourHeight * checkNotNull(newHourHeight)
            hourHeight = checkNotNull(newHourHeight)
            newHourHeight = null
        }
    }

    private fun updateVerticalOrigin() {
        // If the new currentOrigin.y is invalid, make it valid.
        val dayHeight = hourHeight * hoursPerDay
        val headerHeight = headerBounds.height
        val potentialNewVerticalOrigin = bounds.height - (dayHeight + headerHeight)
        currentOrigin.y = max(currentOrigin.y, potentialNewVerticalOrigin)
        currentOrigin.y = min(currentOrigin.y, 0)
    }

    private fun updateHeaderBounds() {
        val headerHeight = calculateHeaderHeight()
        _headerBounds = bounds.copy(
            top = 0,
            bottom = bounds.top + headerHeight
        )
    }

    private fun updateTimeColumnBounds() {
        val headerHeight = _headerBounds.height
        val textWidth = calculateTimeColumnTextWidth()
        val columnWidth = textWidth + timeColumnPadding * 2
        _timeColumnBounds = bounds.copy(
            top = headerHeight, // todo: +1 the right approach?
            right = bounds.left + columnWidth
        )

        _headerBounds = _headerBounds.copy(
            left = _timeColumnBounds.right
        )
    }

    private fun updateCalendarAreaBounds() {
        val columnWidth = _timeColumnBounds.width
        val headerHeight = _headerBounds.height
        _calendarAreaBounds = bounds.copy(
            left = columnWidth,
            top = headerHeight
        )
    }

    fun onSizeChanged(bounds: Rect) {
        val oldHeight = _bounds.height
        val newHeight = bounds.height
        this._bounds = bounds

        updateHeaderBounds()
        updateTimeColumnBounds()
        updateCalendarAreaBounds()

        if (newHeight != oldHeight && showCompleteDay) {
            val calendarAreaHeight = bounds.height - _headerBounds.height
            hourHeight = calendarAreaHeight / hoursPerDay
            newHourHeight = hourHeight
        }
    }

    fun update() {
        updateMinHourHeight()
        refreshAfterZooming()
        updateVerticalOrigin()
        moveCurrentOriginIfFirstDraw()
        updateDateRangeAndStartPixels()
    }

    private fun updateMinHourHeight() {
        val dynamicHourHeight = (bounds.height - getTotalHeaderHeight()) / hoursPerDay
        effectiveMinHourHeight = max(minHourHeight, dynamicHourHeight)
    }

    private fun updateDateRangeAndStartPixels() {
        val originX = currentOrigin.x

        val daysFromOrigin = ceil(originX / widthPerDay.toFloat()).roundToInt() * (-1)
        val timeColumnWidth = _timeColumnBounds.width + timeColumnSeparatorStrokeWidth.scaleBy(0.5f) // TODO Why scale?
        val startPixel = timeColumnWidth + originX + widthPerDay * daysFromOrigin

        val start = daysFromOrigin + 1
        val end = start + numberOfVisibleDays

        // If the user is scrolling, a new view becomes partially visible, so we must add an
        // additional date to the date range
        val isNotScrolling = originX % widthPerDay == 0
        val modifiedEnd = if (isNotScrolling) end - 1 else end

        dateRange.clear()
        dateRange += getDateRange(start, modifiedEnd)

        startPixels.clear()
        startPixels += dateRange.indices.map {
            index -> startPixel + index * widthPerDay
        }

        dateRangeWithStartPixels.clear()
        dateRangeWithStartPixels += dateRange.zip(startPixels)
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

    private fun moveCurrentOriginIfFirstDraw() {
        if (isFirstDraw.not()) {
            return
        }

        if (showFirstDayOfWeekFirst && numberOfVisibleDays == 7) {
            scrollToFirstDayOfWeek()
        }

        if (showCurrentTimeFirst) {
            scrollToCurrentTime()
        }

        // Overwrites the origin when today is out of date range
        currentOrigin.x = min(currentOrigin.x, maxX)
        currentOrigin.x = max(currentOrigin.x, minX)
        isFirstDraw = false
    }

    private fun scrollToFirstDayOfWeek() {
        val difference = computeDifferenceWithFirstDayOfWeek(today())
        currentOrigin.x += (widthPerDay + columnGap) * difference
    }

    private fun scrollToCurrentTime() {
        val now = now()

        val desired = if (now.hour > 0) {
            // Add some padding above the current time (and thus: the now line)
            now - Hours(1)
        } else {
            now.atStartOfDay
        }

        val hour = desired.hour
        val minutes = desired.minute
        val fraction = minutes.toFloat() / Constants.MINUTES_PER_HOUR

        val verticalOffset = (hourHeight * (hour + fraction)).roundToInt()
        val desiredOffset = totalDayHeight - bounds.height

        currentOrigin.y = min(desiredOffset, verticalOffset) * -1
    }

    @Deprecated("")
    fun getTotalHeaderHeight(): Int {
        return headerBounds.height + headerRowPadding * 2
    }

    fun invalidate() {
        hasBeenInvalidated = true
    }

    fun clearCaches(dateTimeInterpreter: DateTimeInterpreter) {
        cache.clear()
        cacheTimeLabels(dateTimeInterpreter)
    }

    fun cacheTimeLabels(dateTimeInterpreter: DateTimeInterpreter) {
        val cache = cache.timeLabels
        for (hour in startHour until hoursPerDay step timeColumnHoursInterval) {
            cache.put(hour, dateTimeInterpreter.interpretTime(hour + minHour))
        }
    }

    internal data class Cache<T>(
        val allDayEventLayouts: ArrayMap<EventChip<T>, StaticLayout> = ArrayMap(),
        val dateLabels: SparseArray<String> = SparseArray(),
        val multiLineDayLabels: SparseArray<StaticLayout> = SparseArray(),
        val timeLabels: SparseArray<String> = SparseArray<String>()
    ) {
        fun clear() {
            allDayEventLayouts.clear()
            dateLabels.clear()
            multiLineDayLabels.clear()
            timeLabels.clear()
        }
    }
}

private fun Typeface.toBold(): Typeface {
    return Typeface.create(this, Typeface.BOLD)
}
