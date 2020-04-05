package com.alamkanak.weekview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import kotlin.math.roundToInt

fun Canvas.drawLine(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    paint: Paint
) {
    drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), paint)
}

fun Canvas.drawRect(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    paint: Paint
) {
    drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
}

fun Canvas.drawRoundRect(
    rect: Rect,
    rx: Float,
    ry: Float,
    paint: Paint
) {
    drawRoundRect(RectF(rect), rx, ry, paint)
}

fun Canvas.drawText(
    text: String,
    x: Int,
    y: Int,
    paint: Paint
) {
    drawText(text, x.toFloat(), y.toFloat(), paint)
}

fun Canvas.drawLines(
    points: IntArray,
    paint: Paint
) {
    drawLines(points.map { it.toFloat() }.toFloatArray(), paint)
}

fun Canvas.drawCircle(
    cx: Int,
    cy: Int,
    radius: Int,
    paint: Paint
) {
    drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), paint)
}

fun Canvas.withTranslation(x: Int, y: Int, block: Canvas.() -> Unit) {
    save()
    translate(x.toFloat(), y.toFloat())
    block()
    restore()
}

fun Canvas.drawInRect(
    bounds: Rect,
    block: Canvas.() -> Unit
) {
    save()
    clipRect(bounds)
    block()
    restore()
}

internal fun Rect.copy(
    left: Int = this.left,
    top: Int = this.top,
    right: Int = this.right,
    bottom: Int = this.bottom
): Rect = Rect(left, top, right, bottom)

@Suppress("FunctionName")
internal fun Rect(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int
): Rect = Rect(left, top, right, bottom)

internal val Rect.width: Int
    get() = width()

internal val Rect.height: Int
    get() = height()

internal val View.bounds: Rect
    get() = Rect(left, top, right, bottom)

internal val Paint.textHeight: Int
    get() = (descent() - ascent()).roundToInt()

internal fun Paint.getTextBounds(text: String): Rect {
    val rect = Rect()
    getTextBounds(text, 0, text.length, rect)
    return rect
}
