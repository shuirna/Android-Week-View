package com.alamkanak.weekview

import android.graphics.Canvas

internal object HeaderRowDrawer : Drawer {

    override fun draw(
        viewState: WeekViewViewState,
        canvas: Canvas
    ) {
        val width = viewState.width.toFloat()
        canvas.drawRect(0f, 0f, width, viewState.headerHeight, viewState.headerBackgroundPaint)

        if (viewState.showHeaderRowBottomLine) {
            val top = viewState.headerHeight - viewState.headerRowBottomLineWidth
            canvas.drawLine(0f, top, width, top, viewState.headerRowBottomLinePaint)
        }
    }
}
