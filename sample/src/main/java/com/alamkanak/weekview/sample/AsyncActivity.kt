package com.alamkanak.weekview.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.sample.data.EventsApi
import com.alamkanak.weekview.sample.data.model.ApiEvent
import com.alamkanak.weekview.sample.util.lazyView
import com.alamkanak.weekview.sample.util.requestFullscreenLayout
import com.alamkanak.weekview.sample.util.setupWithWeekView
import com.alamkanak.weekview.sample.util.showToast
import java.text.SimpleDateFormat
import kotlinx.android.synthetic.main.activity_async.progressBar
import kotlinx.android.synthetic.main.view_toolbar.toolbar

private data class AsyncViewState(
    val events: List<ApiEvent> = emptyList(),
    val isLoading: Boolean = false
)

private class AsyncViewModel(
    private val eventsApi: EventsApi
) {
    val viewState = MutableLiveData<AsyncViewState>()

    init {
        viewState.value = AsyncViewState(isLoading = true)
        fetchEvents()
    }

    fun fetchEvents() = eventsApi.fetchEvents {
        viewState.value = AsyncViewState(it)
    }

    fun remove(event: ApiEvent) {
        val allEvents = viewState.value?.events ?: return
        viewState.value = AsyncViewState(events = allEvents.minus(event))
    }
}

class AsyncActivity : AppCompatActivity() {

    private val weekView: WeekView<ApiEvent> by lazyView(R.id.weekView)

    private val viewModel: AsyncViewModel by lazy {
        AsyncViewModel(EventsApi(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic)

        requestFullscreenLayout()
        toolbar.setupWithWeekView(weekView, this)

        viewModel.viewState.observe(this, Observer { viewState ->
            progressBar.isVisible = viewState.isLoading
            weekView.submit(viewState.events)
        })

        weekView.setOnEventClickListener { event, _ ->
            viewModel.remove(event)
            showToast("Removed ${event.title}")
        }

        weekView.setOnEventLongClickListener { event, _ ->
            showToast("Long-clicked ${event.title}")
        }

        weekView.setOnEmptyViewLongClickListener { time ->
            val sdf = SimpleDateFormat.getDateTimeInstance()
            showToast("Empty view long-clicked at ${sdf.format(time.time)}")
        }
    }
}
