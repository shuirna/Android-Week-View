package com.alamkanak.weekview.sample.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.sample.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class DayLabelBuilder(
    weekView: WeekView<*>
) {

    private val dayOfMonthSpan = DayOfMonthSpan(
        textColor = weekView.headerRowBackgroundColor,
        circleColor = weekView.context.resolveAttribute(R.attr.colorAccent)
    )

    private val weekdaySpan = WeekdaySpan(textColor = weekView.headerRowTextColor)

    private val dayOfMonthFormat = SimpleDateFormat("dd", Locale.getDefault())
    private val weekdayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    fun build(
        date: Calendar
    ): SpannableString {
        val dayOfMonth = dayOfMonthFormat.format(date.time)
        val weekday = weekdayFormat.format(date.time)

        return buildSpannableString {
            withSpan(dayOfMonthSpan) { dayOfMonth }
            appendln()
            withSpan(weekdaySpan) { weekday }
        }
    }
}

private fun buildSpannableString(
    block: SpannableStringBuilder.() -> SpannableStringBuilder
): SpannableString {
    val builder = SpannableStringBuilder()
    return builder.block().build()
}

private fun SpannableStringBuilder.withSpan(
    span: Any,
    block: () -> CharSequence
): SpannableStringBuilder {
    val text = block()
    val currentLength = length
    append(text)
    setSpan(span, currentLength, currentLength + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return this
}

private fun SpannableStringBuilder.build(): SpannableString = SpannableString.valueOf(this)

private class DayOfMonthSpan(
    private val textColor: Int,
    private val circleColor: Int,
    private val padding: Int = 16
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fontMetricsInt: FontMetricsInt?
    ): Int = measureWidth(paint, text, start, end).roundToInt()

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        paint.color = circleColor
        val size = measureWidth(paint, text, start, end)

        val centerX = x + size / 2
        val centerY = (bottom - top) / 2f

        canvas.drawCircle(centerX, centerY, size / 2, paint)
        paint.color = textColor
        canvas.drawText(text, start, end, centerX, y.toFloat(), paint)
    }

    private fun measureWidth(paint: Paint, text: CharSequence, start: Int, end: Int): Float {
        return paint.measureText(text, start, end) + 2 * padding
    }
}

private class WeekdaySpan(
    private val textColor: Int
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fontMetricsInt: FontMetricsInt?
    ): Int = paint.measureText(text, start, end).roundToInt()

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val size = paint.measureText(text, start, end)
        val centerX = x + size / 2
        paint.color = textColor
        canvas.drawText(text, start, end, centerX, y.toFloat() + 16, paint)
    }
}
