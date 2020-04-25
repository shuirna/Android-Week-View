package com.alamkanak.weekview

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alamkanak.weekview.model.Event
import com.alamkanak.weekview.util.FakeResourceResolver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ResolvedWeekViewEventTest {

    @Suppress("UNCHECKED_CAST")
    private val viewState = mock(WeekViewViewState::class.java) as WeekViewViewState<Event>
    private val eventSplitter = WeekViewEventSplitter(viewState)

    private val resourceResolver: ResourceResolver = FakeResourceResolver()

    init {
        MockitoAnnotations.initMocks(this)
        whenever(viewState.minHour).thenReturn(0)
        whenever(viewState.maxHour).thenReturn(24)
    }

    @Test
    fun `single-day event is recognized correctly`() {
        val startTime = (today() + Days(1)).withHour(6).withMinutes(0)
        val endTime = startTime + Hours(10)
        val event = Event(startTime, endTime)

        val originalEvent = event.toWeekViewEvent().resolve(resourceResolver)
        val childEvents = eventSplitter.split(originalEvent)
        assertTrue(childEvents.size == 1)

        val child = childEvents.first()
        assertFalse(child.endTime.toEpochDays() < originalEvent.endTime.toEpochDays())
        assertFalse(child.startTime.toEpochDays() > originalEvent.endTime.toEpochDays())
    }

    @Test
    fun `two-day event is recognized correctly`() {
        val startTime = (today() + Days(1)).withHour(14).withMinutes(0)
        val endTime = (today() + Days(2)).withHour(14).withMinutes(0)
        val event = Event(startTime, endTime)

        val originalEvent = event.toWeekViewEvent().resolve(resourceResolver)
        val childEvents = eventSplitter.split(originalEvent)
        assertTrue(childEvents.size == 2)

        val first = childEvents.first()
        val last = childEvents.last()

        assertTrue(first.endTime.toEpochDays() < originalEvent.endTime.toEpochDays())
        assertTrue(last.startTime.toEpochDays() > originalEvent.startTime.toEpochDays())

        assertFalse(first.startTime.toEpochDays() > originalEvent.startTime.toEpochDays())
        assertFalse(last.endTime.toEpochDays() < originalEvent.endTime.toEpochDays())
    }

    @Test
    fun `multi-day event is recognized correctly`() {
        val startTime = (today() + Days(1)).withHour(14).withMinutes(0)
        val endTime = (today() + Days(3)).withHour(1).withMinutes(0)
        val event = Event(startTime, endTime)

        val originalEvent = event.toWeekViewEvent().resolve(resourceResolver)
        val childEvents = eventSplitter.split(originalEvent)
        assertTrue(childEvents.size == 3)

        val first = childEvents.first()
        val second = childEvents[1]
        val last = childEvents.last()

        assertTrue(first.endTime.toEpochDays() < originalEvent.endTime.toEpochDays())
        assertTrue(second.startTime.toEpochDays() > originalEvent.startTime.toEpochDays())
        assertTrue(second.endTime.toEpochDays() < originalEvent.endTime.toEpochDays())
        assertTrue(last.startTime.toEpochDays() > originalEvent.startTime.toEpochDays())

        assertFalse(first.startTime.toEpochDays() > originalEvent.startTime.toEpochDays())
        assertFalse(last.endTime.toEpochDays() < originalEvent.endTime.toEpochDays())
    }

    @Test
    fun `non-colliding events are recognized correctly`() {
        val firstStartTime = now()
        val firstEndTime = firstStartTime + Hours(1)
        val first = Event(firstStartTime, firstEndTime).toWeekViewEvent().resolve(resourceResolver)

        val secondStartTime = firstStartTime + Hours(2)
        val secondEndTime = secondStartTime + Hours(1)
        val second = Event(secondStartTime, secondEndTime).toWeekViewEvent().resolve(resourceResolver)

        assertFalse(first.collidesWith(second))
    }

    @Test
    fun `overlapping events are recognized as colliding`() {
        val firstStartTime = now()
        val firstEndTime = firstStartTime + Hours(1)
        val first = Event(firstStartTime, firstEndTime).toWeekViewEvent().resolve(resourceResolver)

        val secondStartTime = firstStartTime - Hours(1)
        val secondEndTime = firstEndTime + Hours(1)
        val second = Event(secondStartTime, secondEndTime).toWeekViewEvent().resolve(resourceResolver)

        assertTrue(first.collidesWith(second))
    }

    @Test
    fun `partly-overlapping events are recognized as colliding`() {
        val firstStartTime = now().withMinutes(0)
        val firstEndTime = firstStartTime + Hours(1)
        val first = Event(firstStartTime, firstEndTime).toWeekViewEvent().resolve(resourceResolver)

        val secondStartTime = firstStartTime.withMinutes(30)
        val secondEndTime = secondStartTime + Hours(1)
        val second = Event(secondStartTime, secondEndTime).toWeekViewEvent().resolve(resourceResolver)

        assertTrue(first.collidesWith(second))
    }
}
