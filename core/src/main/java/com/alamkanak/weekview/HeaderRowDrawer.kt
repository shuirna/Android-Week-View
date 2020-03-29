package com.alamkanak.weekview

import android.graphics.Canvas
import android.graphics.Rect
import kotlin.math.roundToInt

internal class HeaderRowDrawer<T>() : Drawer<T> {

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) = with(viewState) {
        canvas.drawRect(headerBounds, headerBackgroundPaint)

        if (showWeekNumber) {
            canvas.drawWeekNumber(viewState = this)
        }

        if (showHeaderRowBottomLine) {
            // val width = bounds.width
            // val headerHeight = headerBounds.height
            // val top = headerHeight - headerRowBottomLineWidth
            // canvas.drawLine(0, top, width, top, headerRowBottomLinePaint)

            val lineBounds = headerBounds.copy(
                left = 0, // TODO
                top = headerBounds.bottom - headerRowBottomLineWidth
            )
            canvas.drawRect(lineBounds, headerRowBottomLinePaint)
        }
    }

    private fun Canvas.drawWeekNumber(viewState: WeekViewViewState<T>) {
        val weekNumber = viewState.firstVisibleDate?.weekOfYear?.toString() ?: return

        val bounds = viewState.weekNumberBounds
        val paint = viewState.weekNumberTextPaint

        val textHeight = paint.textHeight
        val textOffset = (textHeight / 2f).roundToInt() - paint.descent().roundToInt()

        val width = paint.getTextBounds("52").width.scaleBy(factor = 2.5f)
        val height = textHeight.scaleBy(factor = 1.5f)

        val backgroundRect = Rect(
            bounds.centerX() - width / 2,
            bounds.centerY() - height / 2,
            bounds.centerX() + width / 2,
            bounds.centerY() + height / 2
        )

        drawRect(bounds, viewState.headerBackgroundPaint)

        val backgroundPaint = viewState.weekNumberBackgroundPaint
        val radius = viewState.weekNumberBackgroundCornerRadius.toFloat()
        drawRoundRect(backgroundRect, radius, radius, backgroundPaint)

        drawText(weekNumber, bounds.centerX(), bounds.centerY() + textOffset, paint)
    }
}
