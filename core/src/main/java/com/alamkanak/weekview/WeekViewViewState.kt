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

    // Header bottom shadow
    var showHeaderRowBottomShadow: Boolean = false,
    var headerRowBottomShadowColor: Int = 0,
    var headerRowBottomShadowRadius: Int = 0,

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
    var typeface: Typeface = Typeface.DEFAULT_BOLD,

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
    var timeTextWidth: Int = 0,

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

    private val _weekNumberTextPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    val weekNumberTextPaint: Paint
        get() = _weekNumberTextPaint.apply {
            color = weekNumberTextColor
            textAlign = Paint.Align.CENTER
            textSize = weekNumberTextSize.toFloat()
            typeface = this@WeekViewViewState.typeface
        }

    private val _weekNumberBackgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val weekNumberBackgroundPaint: Paint
        get() = _weekNumberBackgroundPaint.apply {
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

    private var _dateFormatter: WeekViewDateFormatter = { calendar, numberOfVisibleDays ->
        defaultDateFormatter(numberOfDays = numberOfVisibleDays).format(calendar.time)
    }

    private var _timeFormatter: WeekViewTimeFormatter = { hour ->
        val date = now().withTime(hour = hour, minutes = 0)
        defaultTimeFormatter().format(date.time)
    }

    var dateFormatter: WeekViewDateFormatter
        get() = _dateFormatter
        set(value) {
            _dateFormatter = value
            onDateFormatterUpdated()
        }

    var timeFormatter: WeekViewTimeFormatter
        get() = _timeFormatter
        set(value) {
            _timeFormatter = value
            onTimeFormatterUpdated()
        }

    var numberOfVisibleDays: Int
        get() = _numberOfVisibleDays
        set(value) {
            // Scroll to first currently visible day after changing the number of visible days
            _numberOfVisibleDays = value
            goToDate = firstVisibleDate
        }

    private val _timeTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    @IgnoredOnParcel
    val timeTextPaint: Paint
        get() = _timeTextPaint.apply {
            textAlign = Paint.Align.RIGHT
            textSize = timeColumnTextSize.toFloat()
            color = timeColumnTextColor
            typeface = this@WeekViewViewState.typeface
        }

    init {
        val hourRange = effectiveMinHour until maxHour
        val timeLabels = hourRange.map { timeFormatter(it) }

        hourRange.forEachIndexed { index, hour ->
            cache.timeLabels[hour] = timeLabels[index]
        }

        val textWidths = timeLabels.map { timeTextPaint.measureText(it).roundToInt() }
        timeTextWidth = textWidths.max() ?: 0
    }

    private fun calculateTimeColumnTextWidth() = (minHour..maxHour)
        .map { timeFormatter(it) }
        .map { timeTextPaint.measureText(it).roundToInt() }
        .max() ?: 0

    private val _headerTextPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    @IgnoredOnParcel
    val headerTextPaint: TextPaint
        get() = _headerTextPaint.apply {
            color = headerRowTextColor
            textAlign = Paint.Align.CENTER
            textSize = headerRowTextSize.toFloat()
            typeface = this@WeekViewViewState.typeface // .toBold()
        }

    var headerTextHeight: Int
        get() {
            return _headerTextHeight ?: headerTextPaint.textHeight
        }
        set(value) {
            _headerTextHeight = value
        }

    private val _headerRowBottomLinePaint: Paint = Paint()

    @IgnoredOnParcel
    val headerRowBottomLinePaint: Paint
        get() = _headerRowBottomLinePaint.apply {
            color = headerRowBottomLineColor
            strokeWidth = headerRowBottomLineWidth.toFloat()
        }

    private val _todayHeaderTextPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    @IgnoredOnParcel
    val todayHeaderTextPaint: TextPaint
        get() = _todayHeaderTextPaint.apply {
            color = todayHeaderTextColor
            textAlign = Paint.Align.CENTER
            textSize = headerRowTextSize.toFloat()
            typeface = this@WeekViewViewState.typeface // .toBold()
        }

    private val _timeColumnBackgroundPaint: Paint = Paint()

    @IgnoredOnParcel
    val timeColumnBackgroundPaint: Paint
        get() = _timeColumnBackgroundPaint.apply {
            color = timeColumnBackgroundColor
        }

    @IgnoredOnParcel
    val effectiveMinHour: Int
        get() = if (showMidnightHour && showTimeColumnHourSeparator) {
            minHour
        } else {
            minHour + 1
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

    private val _headerBackgroundPaint: Paint = Paint()

    @IgnoredOnParcel
    val headerBackgroundPaint: Paint
        get() = _headerBackgroundPaint.apply {
            color = headerRowBackgroundColor
        }

    private val _dayBackgroundPaint: Paint = Paint()

    @IgnoredOnParcel
    val dayBackgroundPaint: Paint
        get() = _dayBackgroundPaint.apply {
            color = dayBackgroundColor
        }

    private val _hourSeparatorPaint: Paint = Paint()

    @IgnoredOnParcel
    val hourSeparatorPaint: Paint
        get() = _hourSeparatorPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = hourSeparatorStrokeWidth.toFloat()
            color = hourSeparatorColor
        }

    private val _daySeparatorPaint: Paint = Paint()

    @IgnoredOnParcel
    val daySeparatorPaint: Paint
        get() = _daySeparatorPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = daySeparatorStrokeWidth.toFloat()
            color = daySeparatorColor
        }

    private val _todayBackgroundPaint: Paint = Paint()

    @IgnoredOnParcel
    val todayBackgroundPaint: Paint
        get() = _todayBackgroundPaint.apply {
            color = todayBackgroundColor
        }

    private val _futureBackgroundPaint: Paint = Paint()

    @IgnoredOnParcel
    val futureBackgroundPaint: Paint
        get() = _futureBackgroundPaint.apply {
            color = futureBackgroundColor
        }

    private val _pastBackgroundPaint: Paint = Paint()

    @IgnoredOnParcel
    val pastBackgroundPaint: Paint
        get() = _pastBackgroundPaint.apply {
            color = pastBackgroundColor
        }

    private val _futureWeekendBackgroundPaint: Paint = Paint()

    @IgnoredOnParcel
    val futureWeekendBackgroundPaint: Paint
        get() = _futureWeekendBackgroundPaint.apply {
            color = futureWeekendBackgroundColor
        }

    private val _pastWeekendBackgroundPaint: Paint = Paint()

    @IgnoredOnParcel
    val pastWeekendBackgroundPaint: Paint
        get() = _pastWeekendBackgroundPaint.apply {
            color = pastWeekendBackgroundColor
        }

    private val _timeColumnSeparatorPaint: Paint = Paint()

    @IgnoredOnParcel
    val timeColumnSeparatorPaint: Paint
        get() = _timeColumnSeparatorPaint.apply {
            color = timeColumnSeparatorColor
            strokeWidth = timeColumnSeparatorStrokeWidth.toFloat()
        }

    private val _nowLinePaint: Paint = Paint()

    @IgnoredOnParcel
    val nowLinePaint: Paint
        get() = _nowLinePaint.apply {
            strokeWidth = nowLineStrokeWidth.toFloat()
            color = nowLineColor
        }

    private val _nowDotPaint: Paint = Paint()

    @IgnoredOnParcel
    val nowDotPaint: Paint
        get() = _nowDotPaint.apply {
            style = Paint.Style.FILL
            strokeWidth = nowLineDotRadius.toFloat()
            color = nowLineDotColor
            isAntiAlias = true
        }

    private val _eventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG)

    val eventTextPaint: TextPaint
        get() = _eventTextPaint.apply {
            style = Paint.Style.FILL
            color = eventTextColor
            textSize = eventTextSize.toFloat()
            typeface = typeface
        }

    val hoursPerDay: Int
        get() = maxHour - minHour

    val totalDayHeight: Int
        get() = headerBounds.height + (hourHeight * hoursPerDay)

    private fun onDateFormatterUpdated() {
        val headerHeight = calculateHeaderHeight()
        _headerBounds = bounds.copy(
            top = 0,
            bottom = headerHeight
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

        val textWidth = calculateTimeColumnTextWidth()
        val columnWidth = textWidth + timeColumnPadding * 2
        _timeColumnBounds = bounds.copy(
            top = headerHeight,
            right = bounds.left + columnWidth
        )

        _headerBounds = _headerBounds.copy(
            left = _timeColumnBounds.width
        )

        _calendarAreaBounds = bounds.copy(
            left = columnWidth,
            top = headerHeight
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
            val potentialNewHourHeight = maxOf(newHourHeight, effectiveMinHourHeight)
            newHourHeight = minOf(potentialNewHourHeight, maxHourHeight)

            // Compute a minimum hour height so that users can't zoom out further
            // than the desired hours per day
            val minHourHeight = (bounds.height - headerBounds.height) / hoursPerDay
            newHourHeight = max(checkNotNull(newHourHeight), minHourHeight)

            currentOrigin.y = currentOrigin.y / hourHeight * checkNotNull(newHourHeight)
            hourHeight = checkNotNull(newHourHeight)
            newHourHeight = null
        }
    }

    private fun minOf(vararg values: Int?): Int? = values.mapNotNull { it }.min()
    private fun maxOf(vararg values: Int?): Int? = values.mapNotNull { it }.max()

    private fun updateVerticalOrigin() {
        // If the new currentOrigin.y is invalid, make it valid.
        val dayHeight = hourHeight * hoursPerDay
        val headerHeight = headerBounds.height
        val newVerticalOrigin = bounds.height - (dayHeight + headerHeight)
        currentOrigin.y = newVerticalOrigin.limit(
            minValue = currentOrigin.y,
            maxValue = 0
        )
    }

    private fun updateHeaderBounds() {
        val headerHeight = calculateHeaderHeight()
        _headerBounds = bounds.copy(
            top = 0,
            bottom = headerHeight
        )
    }

    private fun updateTimeColumnBounds() {
        val headerHeight = _headerBounds.height
        val textWidth = calculateTimeColumnTextWidth()
        val columnWidth = textWidth + timeColumnPadding * 2
        _timeColumnBounds = bounds.copy(
            top = headerHeight,
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

        clearCaches()

        updateHeaderBounds()
        updateTimeColumnBounds()
        updateCalendarAreaBounds()
        updateMinHourHeight()

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
        val dynamicHourHeight = calendarAreaBounds.height / hoursPerDay
        effectiveMinHourHeight = max(minHourHeight, dynamicHourHeight)
    }

    private fun updateDateRangeAndStartPixels() {
        val originX = currentOrigin.x

        val daysFromOrigin = ceil(originX / widthPerDay.toFloat()).roundToInt() * (-1)
        val timeColumnWidth = _timeColumnBounds.width + timeColumnSeparatorStrokeWidth
        val startPixel = timeColumnWidth + originX + (widthPerDay * daysFromOrigin)

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

    fun Calendar.computeDifferenceWithFirstDayOfWeek(): Int {
        val firstDayOfWeek = firstDayOfWeek
        return if (firstDayOfWeek == Calendar.MONDAY && dayOfWeek == Calendar.SUNDAY) {
            // Special case, because Calendar.MONDAY has constant value 2 and Calendar.SUNDAY has
            // constant value 1. The correct result to return is 6 days, not -1 days.
            6
        } else {
            dayOfWeek - firstDayOfWeek
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
        currentOrigin.x = currentOrigin.x.limit(minValue = minX, maxValue = maxX)
        isFirstDraw = false
    }

    private fun scrollToFirstDayOfWeek() {
        val difference = today().computeDifferenceWithFirstDayOfWeek()
        currentOrigin.x += widthPerDay * difference
    }

    private fun scrollToCurrentTime() {
        val desired = now()
        if (desired.hour > minHour) {
            // Add some padding above the current time (and thus: the now line)
            desired -= Hours(1)
        }

        val minTime = now().withTime(hour = minHour, minutes = 0)
        val maxTime = now().withTime(hour = maxHour, minutes = 0)
        desired.limitBy(minTime, maxTime)

        val fraction = desired.minute.toFloat() / Constants.MINUTES_PER_HOUR
        val verticalOffset = (hourHeight * (desired.hour + fraction)).roundToInt()
        val desiredOffset = totalDayHeight - bounds.height

        currentOrigin.y = min(desiredOffset, verticalOffset) * -1
    }

    fun invalidate() {
        hasBeenInvalidated = true
    }

    fun clearCaches() {
        cache.clear()
        cacheTimeLabels()
    }

    private fun cacheTimeLabels() {
        for (hour in effectiveMinHour until maxHour step timeColumnHoursInterval) {
            cache.timeLabels[hour] = timeFormatter(hour)
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
