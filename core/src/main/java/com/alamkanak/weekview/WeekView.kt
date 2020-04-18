package com.alamkanak.weekview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import java.util.Calendar
import kotlin.math.min
import kotlin.math.roundToInt

typealias WeekViewDateFormatter = (date: Calendar, numberOfVisibleDays: Int) -> String
typealias WeekViewTimeFormatter = (hour: Int) -> String

class WeekView<T : Any> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var viewState: WeekViewViewState<T> = WeekViewViewState(context, attrs)

    private val cache = WeekViewCache<T>()
    private val eventChipCache = EventChipCache<T>()

    private val touchHandler = WeekViewTouchHandler(viewState, eventChipCache)

    private val gestureHandler = WeekViewGestureHandler(
        context,
        viewState,
        eventChipCache,
        touchHandler,
        onInvalidation = { ViewCompat.postInvalidateOnAnimation(this) }
    )

    private var accessibilityTouchHelper: WeekViewAccessibilityTouchHelper<T>? = null

    private val eventChipsLoader = EventChipsLoader(viewState, eventChipCache)
    private val eventChipsExpander = EventChipsExpander(viewState, eventChipCache)

    private val eventsCacheWrapper = EventsCacheWrapper<T>()
    private val eventsLoaderWrapper = EventsLoaderWrapper(eventsCacheWrapper)

    private val eventsDiffer = EventsDiffer(eventsCacheWrapper, eventChipsLoader, viewState)

    private val eventsLoader: EventsLoader<T>
        get() = eventsLoaderWrapper.get()

    @PublicApi
    @Deprecated("Use dateFormatter and timeFormatter instead.")
    var dateTimeInterpreter: DateTimeInterpreter
        get() = object : DateTimeInterpreter {
            override fun interpretDate(date: Calendar) = dateFormatter(date, numberOfVisibleDays)
            override fun interpretTime(hour: Int): String = timeFormatter(hour)
        }
        set(value) {
            setDateFormatter { date, _ -> value.interpretDate(date) }
            setTimeFormatter(value::interpretTime)
        }

    @PublicApi
    val dateFormatter: WeekViewDateFormatter
        get() = viewState.dateFormatter

    @PublicApi
    fun setDateFormatter(formatter: WeekViewDateFormatter) {
        viewState.dateFormatter = formatter
    }

    @PublicApi
    val timeFormatter: WeekViewTimeFormatter
        get() = viewState.timeFormatter

    @PublicApi
    fun setTimeFormatter(formatter: WeekViewTimeFormatter) {
        viewState.timeFormatter = formatter
    }

    // Be careful when changing the order of the updaters, as the calculation of any updater might
    // depend on results of previous updaters
    private val updaters = listOf(
        AllDayEventsUpdater(context, cache, eventChipCache),
        DateLabelsUpdater(),
        SingleEventsUpdater(eventChipCache)
    )

    init {
        if (context.isAccessibilityEnabled) {
            accessibilityTouchHelper = WeekViewAccessibilityTouchHelper(
                view = this,
                viewState = viewState,
                gestureHandler = gestureHandler,
                eventChipCache = eventChipCache,
                touchHandler = touchHandler
            )
            ViewCompat.setAccessibilityDelegate(this, accessibilityTouchHelper)
        }
    }

    // Be careful when changing the order of the drawers, as that might cause
    // views to incorrectly draw over each other
    private val drawers = listOf(
        DayBackgroundDrawer(),
        BackgroundGridDrawer(),
        SingleEventsDrawer(context, viewState, eventChipCache),
        NowLineDrawer(),
        TimeColumnDrawer(),
        HeaderRowDrawer(),
        DateLabelsDrawer(),
        AllDayEventsDrawer(context, viewState, cache)
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateViewState()
        notifyScrollListeners()
        refreshEvents()
        updateDimensions()
        performDrawing(canvas)
    }

    private fun updateViewState() {
        viewState.update()

        viewState.goToDate?.let { date ->
            goToDate(date)
        }

        viewState.goToHour?.let { hour ->
            goToHour(hour)
        }
    }

    private fun refreshEvents() {
        if (isInEditMode) {
            return
        }

        // These can either be newly loaded events or previously cached events
        val events = eventsLoader.refresh(viewState.firstVisibleDate)
        eventChipCache.clear()

        if (events.isNotEmpty()) {
            eventChipsLoader.createAndCacheEventChips(events)
            eventChipsExpander.calculateEventChipPositions()
        }
    }

    private fun updateDimensions() {
        for (updater in updaters) {
            if (updater.isRequired(viewState)) {
                updater.update(viewState)
            }
        }
    }

    private fun performDrawing(canvas: Canvas) {
        for (drawer in drawers) {
            drawer.draw(viewState, canvas)
        }

        accessibilityTouchHelper?.invalidateRoot()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return superState?.let {
            SavedState(it, viewState.numberOfVisibleDays, viewState.firstVisibleDate)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)

        if (viewState.restoreNumberOfVisibleDays) {
            viewState.numberOfVisibleDays = savedState.numberOfVisibleDays
        }

        savedState.firstVisibleDate.let {
            goToDate(it)
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        viewState.onSizeChanged(bounds = bounds)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        viewState.x = x.roundToInt()
        viewState.y = y.roundToInt()
    }

    private fun notifyScrollListeners() {
        val oldFirstVisibleDay = viewState.firstVisibleDate
        val totalDayWidth = viewState.widthPerDay

        val daysScrolled = (viewState.currentOrigin.x / totalDayWidth.toFloat()).roundToInt()
        val delta = daysScrolled * (-1)

        viewState.firstVisibleDate = today() + Days(delta)

        val hasFirstVisibleDayChanged = firstVisibleDate.isNotEqual(oldFirstVisibleDay)
        if (hasFirstVisibleDayChanged) {
            scrollListener?.onFirstVisibleDateChanged(firstVisibleDate)
            onRangeChangeListener?.onRangeChanged(firstVisibleDate, lastVisibleDate)
        }
    }

    override fun invalidate() {
        viewState.invalidate()
        super.invalidate()
    }

    /*
     ***********************************************************************************************
     *
     *   Calendar configuration
     *
     ***********************************************************************************************
     */

    /**
     * Returns the first day of the week. Possible values are [java.util.Calendar.SUNDAY],
     * [java.util.Calendar.MONDAY], [java.util.Calendar.TUESDAY],
     * [java.util.Calendar.WEDNESDAY], [java.util.Calendar.THURSDAY],
     * [java.util.Calendar.FRIDAY], [java.util.Calendar.SATURDAY].
     */
    @PublicApi
    var firstDayOfWeek: Int
        get() = viewState.firstDayOfWeek
        set(value) {
            viewState.firstDayOfWeek = value
            invalidate()
        }

    /**
     * Returns the number of visible days.
     */
    @PublicApi
    var numberOfVisibleDays: Int
        get() = viewState.numberOfVisibleDays
        set(value) {
            viewState.numberOfVisibleDays = value
            viewState.clearCaches()
            invalidate()
        }

    /**
     * Returns whether the first day of the week should be displayed at the left-most position
     * when WeekView is displayed for the first time.
     */
    @PublicApi
    var isShowFirstDayOfWeekFirst: Boolean
        get() = viewState.showFirstDayOfWeekFirst
        set(value) {
            viewState.showFirstDayOfWeekFirst = value
        }

    /*
     ***********************************************************************************************
     *
     *   Header bottom line
     *
     ***********************************************************************************************
     */

    /**
     * Returns whether a horizontal line should be displayed at the bottom of the header row.
     */
    @PublicApi
    var isShowHeaderRowBottomLine: Boolean
        get() = viewState.showHeaderRowBottomLine
        set(value) {
            viewState.showHeaderRowBottomLine = value
            invalidate()
        }

    /**
     * Returns the color of the horizontal line at the bottom of the header row.
     */
    @PublicApi
    var headerRowBottomLineColor: Int
        get() = viewState.headerRowBottomLinePaint.color
        set(value) {
            viewState.headerRowBottomLinePaint.color = value
            invalidate()
        }

    /**
     * Returns the stroke width of the horizontal line at the bottom of the header row.
     */
    @PublicApi
    var headerRowBottomLineWidth: Int
        get() = viewState.headerRowBottomLinePaint.strokeWidth.toInt()
        set(value) {
            viewState.headerRowBottomLinePaint.strokeWidth = value.toFloat()
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Time column
     *
     ***********************************************************************************************
     */

    /**
     * Returns the padding in the time column to the left and right side of the time label.
     */
    @PublicApi
    var timeColumnPadding: Int
        get() = viewState.timeColumnPadding
        set(value) {
            viewState.timeColumnPadding = value
            invalidate()
        }

    /**
     * Returns the text color of the labels in the time column.
     */
    @PublicApi
    var timeColumnTextColor: Int
        get() = viewState.timeColumnTextColor
        set(value) {
            viewState.timeColumnTextColor = value
            invalidate()
        }

    /**
     * Returns the background color of the time column.
     */
    @PublicApi
    var timeColumnBackgroundColor: Int
        get() = viewState.timeColumnBackgroundColor
        set(value) {
            viewState.timeColumnBackgroundColor = value
            invalidate()
        }

    /**
     * Returns the text size of the labels in the time column.
     */
    @PublicApi
    var timeColumnTextSize: Int
        get() = viewState.timeColumnTextSize
        set(value) {
            viewState.timeColumnTextSize = value
            invalidate()
        }

    /**
     * Returns whether the label for the midnight hour is displayed in the time column. This setting
     * is only considered if [isShowTimeColumnHourSeparator] is set to true.
     */
    @PublicApi
    var isShowMidnightHour: Boolean
        get() = viewState.showMidnightHour
        set(value) {
            viewState.showMidnightHour = value
            invalidate()
        }

    /**
     * Returns whether a horizontal line is displayed for each hour in the time column.
     */
    @PublicApi
    var isShowTimeColumnHourSeparator: Boolean
        get() = viewState.showTimeColumnHourSeparator
        set(value) {
            viewState.showTimeColumnHourSeparator = value
            invalidate()
        }

    /**
     * Returns the interval in which time labels are displayed in the time column.
     */
    @PublicApi
    var timeColumnHoursInterval: Int
        get() = viewState.timeColumnHoursInterval
        set(value) {
            viewState.timeColumnHoursInterval = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Time column separator
     *
     ***********************************************************************************************
     */

    /**
     * Returns whether a vertical line is displayed at the end of the time column.
     */
    @PublicApi
    var isShowTimeColumnSeparator: Boolean
        get() = viewState.showTimeColumnSeparator
        set(value) {
            viewState.showTimeColumnSeparator = value
            invalidate()
        }

    /**
     * Returns the color of the time column separator.
     */
    @PublicApi
    var timeColumnSeparatorColor: Int
        get() = viewState.timeColumnSeparatorColor
        set(value) {
            viewState.timeColumnSeparatorColor = value
            invalidate()
        }

    /**
     * Returns the stroke width of the time column separator.
     */
    @PublicApi
    var timeColumnSeparatorWidth: Int
        get() = viewState.timeColumnSeparatorStrokeWidth
        set(value) {
            viewState.timeColumnSeparatorStrokeWidth = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Header row
     *
     ***********************************************************************************************
     */

    /**
     * Returns the header row padding, which is applied above and below the all-day event chips.
     */
    @PublicApi
    var headerRowPadding: Int
        get() = viewState.headerRowPadding
        set(value) {
            viewState.headerRowPadding = value
            invalidate()
        }

    /**
     * Returns the header row background color.
     */
    @PublicApi
    var headerRowBackgroundColor: Int
        get() = viewState.headerRowBackgroundColor
        set(value) {
            viewState.headerRowBackgroundColor = value
            invalidate()
        }

    /**
     * Returns the text color used for all date labels except today.
     */
    @PublicApi
    var headerRowTextColor: Int
        get() = viewState.headerRowTextColor
        set(value) {
            viewState.headerRowTextColor = value
            invalidate()
        }

    /**
     * Returns the text color used for today's date label.
     */
    @PublicApi
    var todayHeaderTextColor: Int
        get() = viewState.todayHeaderTextColor
        set(value) {
            viewState.todayHeaderTextColor = value
            invalidate()
        }

    /**
     * Returns the text size of all date labels.
     */
    @PublicApi
    var headerRowTextSize: Int
        get() = viewState.headerRowTextSize
        set(value) {
            viewState.headerRowTextSize = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Event chips
     *
     ***********************************************************************************************
     */

    /**
     * Returns the corner radius of an [EventChip].
     */
    @PublicApi
    var eventCornerRadius: Int
        get() = viewState.eventCornerRadius
        set(value) {
            viewState.eventCornerRadius = value
            invalidate()
        }

    /**
     * Returns the text size of a single-event [EventChip].
     */
    @PublicApi
    var eventTextSize: Int
        get() = viewState.eventTextPaint.textSize.toInt()
        set(value) {
            viewState.eventTextPaint.textSize = value.toFloat()
            invalidate()
        }

    /**
     * Returns whether the text size of the [EventChip] is adapting to the [EventChip] height.
     */
    @PublicApi
    var isAdaptiveEventTextSize: Boolean
        get() = viewState.adaptiveEventTextSize
        set(value) {
            viewState.adaptiveEventTextSize = value
            invalidate()
        }

    /**
     * Returns the text size of an all-day [EventChip].
     */
    @PublicApi
    var allDayEventTextSize: Int
        get() = viewState.allDayEventTextPaint.textSize.toInt()
        set(value) {
            viewState.allDayEventTextPaint.textSize = value.toFloat()
            invalidate()
        }

    /**
     * Returns the default text color of an [EventChip].
     */
    @PublicApi
    var defaultEventTextColor: Int
        get() = viewState.eventTextPaint.color
        set(value) {
            viewState.eventTextPaint.color = value
            invalidate()
        }

    /**
     * Returns the horizontal padding within an [EventChip].
     */
    @PublicApi
    var eventPaddingHorizontal: Int
        get() = viewState.eventPaddingHorizontal
        set(value) {
            viewState.eventPaddingHorizontal = value
            invalidate()
        }

    /**
     * Returns the vertical padding within an [EventChip].
     */
    @PublicApi
    var eventPaddingVertical: Int
        get() = viewState.eventPaddingVertical
        set(value) {
            viewState.eventPaddingVertical = value
            invalidate()
        }

    /**
     * Returns the default text color of an [EventChip].
     */
    @PublicApi
    var defaultEventColor: Int
        get() = viewState.defaultEventColor
        set(value) {
            viewState.defaultEventColor = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Event margins
     *
     ***********************************************************************************************
     */

    /**
     * Returns the column gap at the end of each day.
     */
    @PublicApi
    var columnGap: Int
        get() = viewState.columnGap
        set(value) {
            viewState.columnGap = value
            invalidate()
        }

    /**
     * Returns the horizontal gap between overlapping [EventChip]s.
     */
    @PublicApi
    var overlappingEventGap: Int
        get() = viewState.overlappingEventGap
        set(value) {
            viewState.overlappingEventGap = value
            invalidate()
        }

    /**
     * Returns the vertical margin of an [EventChip].
     */
    @PublicApi
    var eventMarginVertical: Int
        get() = viewState.eventMarginVertical
        set(value) {
            viewState.eventMarginVertical = value
            invalidate()
        }

    /**
     * Returns the horizontal margin of an [EventChip]. This margin is only applied in single-day
     * view and if there are no overlapping events.
     */
    @PublicApi
    var eventMarginHorizontal: Int
        get() = viewState.eventMarginHorizontal
        set(value) {
            viewState.eventMarginHorizontal = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Colors
     *
     ***********************************************************************************************
     */

    /**
     * Returns the background color of a day.
     */
    @PublicApi
    var dayBackgroundColor: Int
        get() = viewState.dayBackgroundPaint.color
        set(value) {
            viewState.dayBackgroundPaint.color = value
            invalidate()
        }

    /**
     * Returns the background color of the current date.
     */
    @PublicApi
    var todayBackgroundColor: Int
        get() = viewState.todayBackgroundPaint.color
        set(value) {
            viewState.todayBackgroundPaint.color = value
            invalidate()
        }

    /**
     * Returns whether weekends should have a background color different from [dayBackgroundColor].
     *
     * The weekend background colors can be defined by [pastWeekendBackgroundColor] and
     * [futureWeekendBackgroundColor].
     */
    @PublicApi
    var isShowDistinctWeekendColor: Boolean
        get() = viewState.showDistinctWeekendColor
        set(value) {
            viewState.showDistinctWeekendColor = value
            invalidate()
        }

    /**
     * Returns whether past and future days should have background colors different from
     * [dayBackgroundColor].
     *
     * The past and future day colors can be defined by [pastBackgroundColor] and
     * [futureBackgroundColor].
     */
    @PublicApi
    var isShowDistinctPastFutureColor: Boolean
        get() = viewState.showDistinctPastFutureColor
        set(value) {
            viewState.showDistinctPastFutureColor = value
            invalidate()
        }

    /**
     * Returns the background color for past dates. If not explicitly set, WeekView will used
     * [dayBackgroundColor].
     */
    @PublicApi
    var pastBackgroundColor: Int
        get() = viewState.pastBackgroundPaint.color
        set(value) {
            viewState.pastBackgroundPaint.color = value
            invalidate()
        }

    /**
     * Returns the background color for past weekend dates. If not explicitly set, WeekView will
     * used [pastBackgroundColor].
     */
    @PublicApi
    var pastWeekendBackgroundColor: Int
        get() = viewState.pastWeekendBackgroundPaint.color
        set(value) {
            viewState.pastWeekendBackgroundPaint.color = value
            invalidate()
        }

    /**
     * Returns the background color for future dates. If not explicitly set, WeekView will used
     * [dayBackgroundColor].
     */
    @PublicApi
    var futureBackgroundColor: Int
        get() = viewState.futureBackgroundPaint.color
        set(value) {
            viewState.futureBackgroundPaint.color = value
            invalidate()
        }

    /**
     * Returns the background color for future weekend dates. If not explicitly set, WeekView will
     * used [futureBackgroundColor].
     */
    @PublicApi
    var futureWeekendBackgroundColor: Int
        get() = viewState.futureWeekendBackgroundPaint.color
        set(value) {
            viewState.futureWeekendBackgroundPaint.color = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Hour height
     *
     ***********************************************************************************************
     */

    /**
     * Returns the current height of an hour.
     */
    @PublicApi
    var hourHeight: Float
        get() = viewState.hourHeight.toFloat()
        set(value) {
            viewState.newHourHeight = value.roundToInt()
            invalidate()
        }

    /**
     * Returns the minimum height of an hour.
     */
    @PublicApi
    var minHourHeight: Int
        get() = viewState.minHourHeight
        set(value) {
            viewState.minHourHeight = value
            invalidate()
        }

    /**
     * Returns the maximum height of an hour.
     */
    @PublicApi
    var maxHourHeight: Int
        get() = viewState.maxHourHeight
        set(value) {
            viewState.maxHourHeight = value
            invalidate()
        }

    /**
     * Returns whether the complete day should be shown, in which case [hourHeight] automatically
     * adjusts to accommodate all hours between [minHour] and [maxHour].
     */
    @PublicApi
    var isShowCompleteDay: Boolean
        get() = viewState.showCompleteDay
        set(value) {
            viewState.showCompleteDay = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Now line
     *
     ***********************************************************************************************
     */

    /**
     * Returns whether a horizontal line should be displayed at the current time.
     */
    @PublicApi
    var isShowNowLine: Boolean
        get() = viewState.showNowLine
        set(value) {
            viewState.showNowLine = value
            invalidate()
        }

    /**
     * Returns the color of the horizontal "now" line.
     */
    @PublicApi
    var nowLineColor: Int
        get() = viewState.nowLinePaint.color
        set(value) {
            viewState.nowLinePaint.color = value
            invalidate()
        }

    /**
     * Returns the stroke width of the horizontal "now" line.
     */
    @PublicApi
    var nowLineStrokeWidth: Int
        get() = viewState.nowLinePaint.strokeWidth.toInt()
        set(value) {
            viewState.nowLinePaint.strokeWidth = value.toFloat()
            invalidate()
        }

    /**
     * Returns whether a dot at the start of the "now" line is displayed. The dot is only displayed
     * if [isShowNowLine] is set to true.
     */
    @PublicApi
    var isShowNowLineDot: Boolean
        get() = viewState.showNowLineDot
        set(value) {
            viewState.showNowLineDot = value
            invalidate()
        }

    /**
     * Returns the color of the dot at the start of the "now" line.
     */
    @PublicApi
    var nowLineDotColor: Int
        get() = viewState.nowDotPaint.color
        set(value) {
            viewState.nowDotPaint.color = value
            invalidate()
        }

    /**
     * Returns the radius of the dot at the start of the "now" line.
     */
    @PublicApi
    var nowLineDotRadius: Int
        get() = viewState.nowDotPaint.strokeWidth.toInt()
        set(value) {
            viewState.nowDotPaint.strokeWidth = value.toFloat()
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Hour separators
     *
     ***********************************************************************************************
     */

    @PublicApi
    var isShowHourSeparators: Boolean
        get() = viewState.showHourSeparators
        set(value) {
            viewState.showHourSeparators = value
            invalidate()
        }

    @PublicApi
    var hourSeparatorColor: Int
        get() = viewState.hourSeparatorPaint.color
        set(value) {
            viewState.hourSeparatorPaint.color = value
            invalidate()
        }

    @PublicApi
    var hourSeparatorStrokeWidth: Int
        get() = viewState.hourSeparatorPaint.strokeWidth.toInt()
        set(value) {
            viewState.hourSeparatorPaint.strokeWidth = value.toFloat()
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Day separators
     *
     ***********************************************************************************************
     */

    /**
     * Returns whether vertical lines are displayed as separators between dates.
     */
    @PublicApi
    var isShowDaySeparators: Boolean
        get() = viewState.showDaySeparators
        set(value) {
            viewState.showDaySeparators = value
            invalidate()
        }

    /**
     * Returns the color of the separators between dates.
     */
    @PublicApi
    var daySeparatorColor: Int
        get() = viewState.daySeparatorPaint.color
        set(value) {
            viewState.daySeparatorPaint.color = value
            invalidate()
        }

    /**
     * Returns the stroke color of the separators between dates.
     */
    @PublicApi
    var daySeparatorStrokeWidth: Int
        get() = viewState.daySeparatorPaint.strokeWidth.toInt()
        set(value) {
            viewState.daySeparatorPaint.strokeWidth = value.toFloat()
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Date range
     *
     ***********************************************************************************************
     */

    /**
     * Returns the minimum date that [WeekView] will display, or null if none is set. Events before
     * this date will not be shown.
     */
    @PublicApi
    var minDate: Calendar?
        get() = viewState.minDate?.copy()
        set(value) {
            val maxDate = viewState.maxDate
            if (maxDate != null && value != null && value.isAfter(maxDate)) {
                throw IllegalArgumentException("Can't set a minDate that's after maxDate")
            }

            viewState.minDate = value
            invalidate()
        }

    /**
     * Returns the maximum date that [WeekView] will display, or null if none is set. Events after
     * this date will not be shown.
     */
    @PublicApi
    var maxDate: Calendar?
        get() = viewState.maxDate?.copy()
        set(value) {
            val minDate = viewState.minDate
            if (minDate != null && value != null && value.isBefore(minDate)) {
                throw IllegalArgumentException("Can't set a maxDate that's before minDate")
            }

            viewState.maxDate = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Time range
     *
     ***********************************************************************************************
     */

    /**
     * Returns the minimum hour that [WeekView] will display. Events before this time will not be
     * shown.
     */
    @PublicApi
    var minHour: Int
        get() = viewState.minHour
        set(value) {
            if (value < 0 || value > viewState.maxHour) {
                throw IllegalArgumentException("minHour must be between 0 and maxHour.")
            }

            viewState.minHour = value
            invalidate()
        }

    /**
     * Returns the maximum hour that [WeekView] will display. Events before this time will not be
     * shown.
     */
    @PublicApi
    var maxHour: Int
        get() = viewState.maxHour
        set(value) {
            if (value > 24 || value < viewState.minHour) {
                throw IllegalArgumentException("maxHour must be between minHour and 24.")
            }

            viewState.maxHour = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Scrolling
     *
     ***********************************************************************************************
     */

    /**
     * Returns the scrolling speed factor in horizontal direction.
     */
    @PublicApi
    @Deprecated("xScrollingSpeed is no longer being used and will be removed in a future release.")
    var xScrollingSpeed: Float = 1f

    /**
     * Returns whether WeekView can scroll horizontally.
     */
    @PublicApi
    @Deprecated(
        message = "isHorizontalFlingEnabled is no longer being used and will be removed in a future release.",
        replaceWith = ReplaceWith("isHorizontalScrollingEnabled")
    )
    var isHorizontalFlingEnabled: Boolean
        get() = isHorizontalScrollingEnabled
        set(value) {
            isHorizontalScrollingEnabled = value
        }

    /**
     * Returns whether WeekView can scroll horizontally.
     */
    @PublicApi
    var isHorizontalScrollingEnabled: Boolean
        get() = viewState.horizontalScrollingEnabled
        set(value) {
            viewState.horizontalScrollingEnabled = value
        }

    /**
     * Returns whether WeekView can fling vertically.
     */
    @PublicApi
    @Deprecated(
        message = "Use isVerticalScrollingEnabled instead",
        replaceWith = ReplaceWith("isVerticalScrollingEnabled")
    )
    var isVerticalFlingEnabled: Boolean
        get() = isVerticalScrollingEnabled
        set(value) {
            isVerticalScrollingEnabled = value
        }

    /**
     * Returns whether WeekView can scroll vertically.
     */
    @PublicApi
    var isVerticalScrollingEnabled: Boolean
        get() = viewState.verticalScrollingEnabled
        set(value) {
            viewState.verticalScrollingEnabled = value
        }

    @PublicApi
    @Deprecated("scrollDuration is no longer being used and will be removed in a future release.")
    var scrollDuration: Int = 250

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = gestureHandler.onTouchEvent(event)

    /*
     ***********************************************************************************************
     *
     *   Date methods
     *
     ***********************************************************************************************
     */

    /**
     * Returns the first visible date.
     */
    @PublicApi
    val firstVisibleDate: Calendar
        get() = viewState.firstVisibleDate.copy()

    /**
     * Returns the last visible date.
     */
    @PublicApi
    val lastVisibleDate: Calendar
        get() = viewState.lastVisibleDate

    /**
     * Shows the current date.
     */
    @PublicApi
    fun goToToday(animated: Boolean = true) {
        goToDate(today(), animated)
    }

    /**
     * Shows the current date and time.
     */
    @PublicApi
    fun goToCurrentTime(animated: Boolean = true) {
        val currentTime = now()
        internalGoToDate(
            date = currentTime,
            animated = animated,
            onEnd = { goToHour(hour = currentTime.hour, animated = animated) }
        )
    }

    private val goToDateAnimator = ValueAnimator()

    /**
     * Shows a specific date. If it is before [minDate] or after [maxDate], these will be shown
     * instead.
     *
     * @param date The date to show.
     */
    @PublicApi
    fun goToDate(date: Calendar, animated: Boolean = true) {
        internalGoToDate(date, animated)
    }

    private fun internalGoToDate(date: Calendar, animated: Boolean, onEnd: () -> Unit = {}) {
        val adjustedDate = viewState.ensureDateRange(date)
        gestureHandler.stopScroll()

        if (viewState.hasBeenInvalidated || isWaitingToBeLaidOut) {
            // If the view's dimensions have just changed or if it hasn't been laid out yet, we
            // postpone the action until onDraw() is called the next time.
            viewState.goToDate = adjustedDate
            return
        } else {
            viewState.goToDate = null
        }

        eventsLoader.requireRefresh()

        val destinationOffset = date.daysFromToday * viewState.widthPerDay * (-1)

        if (animated && !goToDateAnimator.isRunning) {
            goToDateAnimator.animate(
                fromValue = viewState.currentOrigin.x,
                toValue = destinationOffset,
                onUpdate = { value ->
                    viewState.currentOrigin.x = value
                    invalidate()
                },
                onEnd = onEnd
            )
        } else {
            viewState.currentOrigin.x = destinationOffset
            onEnd()
            invalidate()
        }
    }

    private fun WeekViewViewState<T>.ensureDateRange(date: Calendar): Calendar {
        val minDate = minDate ?: date
        val maxDate = maxDate ?: date

        return if (date.isBefore(minDate)) {
            minDate
        } else if (date.isAfter(maxDate)) {
            maxDate + Days(1 - numberOfVisibleDays)
        } else if (numberOfVisibleDays >= 7 && showFirstDayOfWeekFirst) {
            val diff = date.computeDifferenceWithFirstDayOfWeek()
            date - Days(diff)
        } else {
            date
        }
    }

    private val isWaitingToBeLaidOut: Boolean
        get() = ViewCompat.isLaidOut(this).not()

    /**
     * Refreshes the view and loads the events again.
     */
    @PublicApi
    fun notifyDataSetChanged() {
        eventsLoader.requireRefresh()
        invalidate()
    }

    private val goToHourAnimator = ValueAnimator()

    /**
     * Scrolls to a specific hour.
     *
     * @param hour The hour to scroll to, in 24-hour format. Supported values are 0-24.
     *
     * @throws IllegalArgumentException Throws exception if the provided hour is smaller than
     *                                   [minHour] or larger than [maxHour].
     */
    @PublicApi
    fun goToHour(hour: Int, animated: Boolean = true) {
        if (viewState.hasBeenInvalidated || isWaitingToBeLaidOut) {
            // Perform navigation in next onDraw() call
            viewState.goToHour = hour
            return
        } else {
            viewState.goToDate = null
        }

        if (hour !in minHour..maxHour) {
            throw IllegalArgumentException(
                "Hour must be between ${viewState.minHour} - ${viewState.maxHour} (was $hour)"
            )
        }

        val hourHeight = viewState.hourHeight
        val desiredOffset = hourHeight * (hour - viewState.minHour)

        // We make sure that WeekView doesn't "over-scroll" by limiting the offset to the total day
        // height minus the height of WeekView, which would result in scrolling all the way to the
        // bottom.
        val maxOffset = viewState.totalDayHeight - height
        val finalOffset = min(maxOffset, desiredOffset)

        if (animated && !goToHourAnimator.isRunning) {
            goToHourAnimator.animate(
                fromValue = viewState.currentOrigin.y,
                toValue = finalOffset * (-1),
                onUpdate = { value ->
                    viewState.currentOrigin.y = value
                    invalidate()
                }
            )
        } else {
            viewState.currentOrigin.y = finalOffset * (-1)
            invalidate()
        }
    }

    /**
     * Returns the first hour that is visible on the screen.
     */
    @PublicApi
    val firstVisibleHour: Double
        get() = (viewState.currentOrigin.y * -1 / viewState.hourHeight).toDouble()

    /*
     ***********************************************************************************************
     *
     *   Typeface
     *
     ***********************************************************************************************
     */

    /**
     * Returns the typeface used for events, time labels and date labels.
     */
    @PublicApi
    var typeface: Typeface
        get() = viewState.typeface
        set(value) {
            viewState.typeface = value
            invalidate()
        }

    /*
     ***********************************************************************************************
     *
     *   Listeners
     *
     ***********************************************************************************************
     */

    @PublicApi
    var onEventClickListener: OnEventClickListener<T>?
        get() = touchHandler.onEventClickListener
        set(value) {
            touchHandler.onEventClickListener = value
        }

    @PublicApi
    fun setOnEventClickListener(
        block: (data: T, rect: RectF) -> Unit
    ) {
        onEventClickListener = object : OnEventClickListener<T> {
            override fun onEventClick(data: T, eventRect: RectF) {
                block(data, eventRect)
            }
        }
    }

    @PublicApi
    var onMonthChangeListener: OnMonthChangeListener<T>?
        get() = (eventsLoader as? LegacyEventsLoader)?.onMonthChangeListener
        set(value) {
            eventsCacheWrapper.onListenerChanged(value)
            eventsLoaderWrapper.onListenerChanged(value)
        }

    @PublicApi
    fun setOnMonthChangeListener(
        block: (startDate: Calendar, endDate: Calendar) -> List<WeekViewDisplayable<T>>
    ) {
        onMonthChangeListener = object : OnMonthChangeListener<T> {
            override fun onMonthChange(
                startDate: Calendar,
                endDate: Calendar
            ): List<WeekViewDisplayable<T>> {
                return block(startDate, endDate)
            }
        }
    }

    /**
     * Submits a list of [WeekViewDisplayable]s to [WeekView]. If the new events fall into the
     * currently displayed date range, this method will also redraw [WeekView].
     */
    @PublicApi
    fun submit(items: List<WeekViewDisplayable<T>>) {
        eventsDiffer.submit(items) { shouldInvalidate ->
            if (shouldInvalidate) {
                invalidate()
            }
        }
    }

    @PublicApi
    var onLoadMoreListener: OnLoadMoreListener?
        get() = (eventsLoader as? PagedEventsLoader)?.onLoadMoreListener
        set(value) {
            eventsCacheWrapper.onListenerChanged(value)
            eventsLoaderWrapper.onListenerChanged(value)
        }

    /**
     * Registers a block that is called whenever [WeekView] needs to load more events. This is
     * similar to an [OnMonthChangeListener], but does not require anything to be returned.
     */
    @PublicApi
    fun setOnLoadMoreListener(
        block: (startDate: Calendar, endDate: Calendar) -> Unit
    ) {
        onLoadMoreListener = object : OnLoadMoreListener {
            override fun onLoadMore(startDate: Calendar, endDate: Calendar) {
                block(startDate, endDate)
            }
        }
    }

    @PublicApi
    var onEventLongClickListener: OnEventLongClickListener<T>?
        get() = touchHandler.onEventLongClickListener
        set(value) {
            touchHandler.onEventLongClickListener = value
        }

    @PublicApi
    fun setOnEventLongClickListener(
        block: (data: T, rect: RectF) -> Unit
    ) {
        onEventLongClickListener = object : OnEventLongClickListener<T> {
            override fun onEventLongClick(data: T, eventRect: RectF) {
                block(data, eventRect)
            }
        }
    }

    @PublicApi
    var onEmptyViewClickListener: OnEmptyViewClickListener?
        get() = touchHandler.onEmptyViewClickListener
        set(value) {
            touchHandler.onEmptyViewClickListener = value
        }

    @PublicApi
    fun setOnEmptyViewClickListener(
        block: (time: Calendar) -> Unit
    ) {
        onEmptyViewClickListener = object : OnEmptyViewClickListener {
            override fun onEmptyViewClicked(time: Calendar) {
                block(time)
            }
        }
    }

    @PublicApi
    var onEmptyViewLongClickListener: OnEmptyViewLongClickListener?
        get() = touchHandler.onEmptyViewLongClickListener
        set(value) {
            touchHandler.onEmptyViewLongClickListener = value
        }

    @PublicApi
    fun setOnEmptyViewLongClickListener(
        block: (time: Calendar) -> Unit
    ) {
        onEmptyViewLongClickListener = object : OnEmptyViewLongClickListener {
            override fun onEmptyViewLongClick(time: Calendar) {
                block(time)
            }
        }
    }

    @PublicApi
    var scrollListener: ScrollListener?
        get() = gestureHandler.scrollListener
        set(value) {
            gestureHandler.scrollListener = value
        }

    @PublicApi
    fun setScrollListener(
        block: (date: Calendar) -> Unit
    ) {
        scrollListener = object : ScrollListener {
            override fun onFirstVisibleDateChanged(date: Calendar) {
                block(checkNotNull(firstVisibleDate))
            }
        }
    }

    @PublicApi
    var onRangeChangeListener: OnRangeChangeListener? = null

    @PublicApi
    fun setOnRangeChangeListener(
        block: (firstVisibleDate: Calendar, lastVisibleDate: Calendar) -> Unit
    ) {
        onRangeChangeListener = object : OnRangeChangeListener {
            override fun onRangeChanged(firstVisibleDate: Calendar, lastVisibleDate: Calendar) {
                block(firstVisibleDate, lastVisibleDate)
            }
        }
    }

    override fun dispatchHoverEvent(
        event: MotionEvent
    ) = accessibilityTouchHelper?.dispatchHoverEvent(event) ?: super.dispatchHoverEvent(event)
}
