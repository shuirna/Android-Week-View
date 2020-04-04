package com.alamkanak.weekview.sample

import android.graphics.RectF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alamkanak.weekview.OnEmptyViewLongClickListener
import com.alamkanak.weekview.OnEventClickListener
import com.alamkanak.weekview.OnEventLongClickListener
import com.alamkanak.weekview.OnMonthChangeListener
import com.alamkanak.weekview.OnRangeChangeListener
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.sample.data.EventsDatabase
import com.alamkanak.weekview.sample.data.model.Event
import com.alamkanak.weekview.sample.util.lazyView
import com.alamkanak.weekview.sample.util.requestFullscreenLayout
import com.alamkanak.weekview.sample.util.setupWithWeekView
import com.alamkanak.weekview.sample.util.showToast
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlinx.android.synthetic.main.activity_static.dateRangeTextView
import kotlinx.android.synthetic.main.activity_static.nextWeekButton
import kotlinx.android.synthetic.main.activity_static.previousWeekButton
import kotlinx.android.synthetic.main.view_toolbar.toolbar

class StaticActivity : AppCompatActivity(), OnEventClickListener<Event>,
    OnMonthChangeListener<Event>, OnEventLongClickListener<Event>, OnEmptyViewLongClickListener {

    private val weekView: WeekView<Event> by lazyView(R.id.weekView)

    private val database: EventsDatabase by lazy { EventsDatabase(this) }
    private val dateFormatter = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_static)

        requestFullscreenLayout()
        toolbar.setupWithWeekView(weekView, this)

        weekView.onEventClickListener = this
        weekView.onMonthChangeListener = this
        weekView.onEventLongClickListener = this
        weekView.onEmptyViewLongClickListener = this

        previousWeekButton.setOnClickListener {
            val date = checkNotNull(weekView.firstVisibleDate)
            date.add(Calendar.DATE, -7)
            weekView.goToDate(date)
        }

        nextWeekButton.setOnClickListener {
            val date = checkNotNull(weekView.firstVisibleDate)
            date.add(Calendar.DATE, 7)
            weekView.goToDate(date)
        }

        weekView.onRangeChangeListener = object : OnRangeChangeListener {
            override fun onRangeChanged(
                firstVisibleDate: Calendar,
                lastVisibleDate: Calendar
            ) = updateDateText(firstVisibleDate, lastVisibleDate)
        }
    }

    override fun onMonthChange(
        startDate: Calendar,
        endDate: Calendar
    ) = database.getEventsInRange(startDate, endDate)

    override fun onEventClick(data: Event, eventRect: RectF) {
        showToast("Clicked ${data.title}")
    }

    override fun onEventLongClick(data: Event, eventRect: RectF) {
        showToast("Long-clicked ${data.title}")
    }

    override fun onEmptyViewLongClick(time: Calendar) {
        val sdf = SimpleDateFormat.getDateTimeInstance()
        showToast("Empty view long-clicked at ${sdf.format(time.time)}")
    }

    internal fun updateDateText(firstVisibleDate: Calendar, lastVisibleDate: Calendar) {
        val formattedFirstDay = dateFormatter.format(firstVisibleDate.time)
        val formattedLastDay = dateFormatter.format(lastVisibleDate.time)
        dateRangeTextView.text = getString(R.string.date_infos, formattedFirstDay, formattedLastDay)
    }
}
