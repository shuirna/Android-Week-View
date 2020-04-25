package com.alamkanak.weekview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.StaticLayout
import kotlin.math.roundToInt

internal class EventChipDrawer<T>(
    private val viewState: WeekViewViewState<T>
) {

    private val textFitter = TextFitter(viewState)
    private val textLayoutCache = mutableMapOf<Long, StaticLayout>()

    private val backgroundPaint = Paint()
    private val borderPaint = Paint()

    internal fun draw(
        eventChip: EventChip<T>,
        canvas: Canvas,
        textLayout: StaticLayout? = null
    ) {
        val event = eventChip.event

        val cornerRadius = viewState.eventCornerRadius.toFloat()
        updateBackgroundPaint(event, backgroundPaint)

        val rect = checkNotNull(eventChip.bounds)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        if (event.style.borderWidth != null) {
            updateBorderPaint(event, borderPaint)

            val borderWidth = checkNotNull(event.style.borderWidth)
            val adjustedRect = RectF(
                rect.left + borderWidth / 2f,
                rect.top + borderWidth / 2f,
                rect.right - borderWidth / 2f,
                rect.bottom - borderWidth / 2f)
            canvas.drawRoundRect(adjustedRect, cornerRadius, cornerRadius, borderPaint)
        }

        if (event.isNotAllDay) {
            drawCornersForMultiDayEvents(eventChip, cornerRadius, canvas)
        }

        if (textLayout != null) {
            // The text height has already been calculated
            drawEventTitle(eventChip, textLayout, canvas)
        } else {
            calculateTextHeightAndDrawTitle(eventChip, canvas)
        }
    }

    private fun drawCornersForMultiDayEvents(
        eventChip: EventChip<T>,
        cornerRadius: Float,
        canvas: Canvas
    ) {
        val event = eventChip.event
        val originalEvent = eventChip.originalEvent
        val rect = checkNotNull(eventChip.bounds)

        updateBackgroundPaint(event, backgroundPaint)

        if (event.startTime.toEpochDays() > originalEvent.startTime.toEpochDays()) {
            // This event fraction starts after the original event
            val topRect = rect.copy(bottom = rect.top + cornerRadius.roundToInt())
            canvas.drawRect(topRect, backgroundPaint)
        }

        if (event.endTime.toEpochDays() < originalEvent.endTime.toEpochDays()) {
            // This event fraction ends before the original event
            val bottomRect = rect.copy(top = rect.bottom - cornerRadius.roundToInt())
            canvas.drawRect(bottomRect, backgroundPaint)
        }

        if (event.style.borderWidth != null) {
            drawStroke(eventChip, canvas)
        }
    }

    private fun drawStroke(
        eventChip: EventChip<T>,
        canvas: Canvas
    ) {
        val event = eventChip.event
        val originalEvent = eventChip.originalEvent
        val rect = checkNotNull(eventChip.bounds)

        val borderWidth = checkNotNull(event.style.borderWidth)
        val innerWidth = rect.width - borderWidth * 2

        val borderStartX = rect.left + borderWidth
        val borderEndX = borderStartX + innerWidth

        updateBorderPaint(event, backgroundPaint)

        if (event.startTime.toEpochDays() > originalEvent.startTime.toEpochDays()) {
            // Remove top rounded corners by drawing a rectangle
            val borderStartY = rect.top
            val borderEndY = borderStartY + borderWidth
            val newRect = Rect(borderStartX, borderStartY, borderEndX, borderEndY)
            canvas.drawRect(newRect, backgroundPaint)
        }

        if (event.endTime.toEpochDays() < originalEvent.endTime.toEpochDays()) {
            // Remove bottom rounded corners by drawing a rectangle
            val borderEndY = rect.bottom
            val borderStartY = borderEndY - borderWidth
            val newRect = Rect(borderStartX, borderStartY, borderEndX, borderEndY)
            canvas.drawRect(newRect, backgroundPaint)
        }
    }

    private fun drawEventTitle(
        eventChip: EventChip<T>,
        textLayout: StaticLayout,
        canvas: Canvas
    ) {
        val rect = checkNotNull(eventChip.bounds)
        canvas.apply {
            save()
            translate(
                (rect.left + viewState.eventPaddingHorizontal).toFloat(),
                (rect.top + viewState.eventPaddingVertical).toFloat()
            )
            textLayout.draw(this)
            restore()
        }
    }

    private fun calculateTextHeightAndDrawTitle(
        eventChip: EventChip<T>,
        canvas: Canvas
    ) {
        val event = eventChip.event
        val rect = checkNotNull(eventChip.bounds)

        val fullHorizontalPadding = viewState.eventPaddingHorizontal * 2
        val fullVerticalPadding = viewState.eventPaddingVertical * 2

        val negativeWidth = rect.right - rect.left - fullHorizontalPadding < 0
        val negativeHeight = rect.bottom - rect.top - fullVerticalPadding < 0
        if (negativeWidth || negativeHeight) {
            return
        }

        val chipHeight = rect.bottom - rect.top - fullVerticalPadding
        val chipWidth = rect.right - rect.left - fullHorizontalPadding

        if (chipHeight == 0 || chipWidth == 0) {
            return
        }

        val didAvailableAreaChange = eventChip.didAvailableAreaChange(
            area = rect,
            horizontalPadding = fullHorizontalPadding,
            verticalPadding = fullVerticalPadding
        )
        val isCached = event.id in textLayoutCache

        if (didAvailableAreaChange || !isCached) {
            textLayoutCache[event.id] = textFitter.fit(
                eventChip = eventChip,
                title = event.title,
                location = event.location,
                chipHeight = chipHeight,
                chipWidth = chipWidth
            )
            eventChip.updateAvailableArea(chipWidth, chipHeight)
        }

        val textLayout = textLayoutCache[event.id] ?: return
        if (textLayout.height <= chipHeight) {
            drawEventTitle(eventChip, textLayout, canvas)
        }
    }

    private fun updateBackgroundPaint(
        event: ResolvedWeekViewEvent<T>,
        paint: Paint
    ) {
        paint.color = event.style.backgroundColor ?: viewState.defaultEventColor
        paint.isAntiAlias = true
        paint.strokeWidth = 0f
        paint.style = Paint.Style.FILL
    }

    private fun updateBorderPaint(
        event: ResolvedWeekViewEvent<T>,
        paint: Paint
    ) {
        paint.color = checkNotNull(event.style.borderColor)
        paint.isAntiAlias = true
        paint.strokeWidth = checkNotNull(event.style.borderWidth?.toFloat())
        paint.style = Paint.Style.STROKE
    }
}
