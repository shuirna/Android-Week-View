package com.alamkanak.weekview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.roundToInt

internal class HeaderRowDrawer<T> : Drawer<T> {

    override fun draw(
        viewState: WeekViewViewState<T>,
        canvas: Canvas
    ) {
        canvas.drawBackground(viewState)

        if (viewState.showWeekNumber) {
            canvas.drawWeekNumber(viewState)
        }

        if (viewState.showHeaderRowBottomLine) {
            canvas.drawHeaderRowBottomLine(viewState)
        }
    }

    private fun Canvas.drawBackground(viewState: WeekViewViewState<T>) {
        val backgroundBounds = viewState.headerBounds.copy(left = 0)

        val backgroundPaint = if (viewState.showHeaderRowBottomShadow) {
            viewState.headerBackgroundPaint.withShadow(
                radius = viewState.headerRowBottomShadowRadius,
                color = viewState.headerRowBottomShadowColor
            )
        } else viewState.headerBackgroundPaint

        drawRect(backgroundBounds, backgroundPaint)
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

    private fun Canvas.drawHeaderRowBottomLine(viewState: WeekViewViewState<T>) = with(viewState) {
        val lineBounds = headerBounds.copy(
            left = 0,
            top = headerBounds.bottom - headerRowBottomLineWidth
        )
        drawRect(lineBounds, headerRowBottomLinePaint)
    }

    private fun Paint.withShadow(radius: Int, color: Int): Paint = Paint(this).apply {
        setShadowLayer(radius.toFloat(), 0f, 0f, color)
    }
}
