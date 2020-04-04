package com.alamkanak.weekview

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.roundToInt

internal class ScaleGestureDetector(
    context: Context,
    private val viewState: WeekViewViewState<*>,
    private val valueAnimator: ValueAnimator,
    private val onInvalidation: () -> Unit
) {

    private val listener = object : ScaleGestureDetector.OnScaleGestureListener {

        override fun onScaleBegin(
            detector: ScaleGestureDetector
        ): Boolean = !valueAnimator.isRunning

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val hourHeight = viewState.hourHeight
            viewState.newHourHeight = (hourHeight * detector.scaleFactor).roundToInt()
            onInvalidation()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            onInvalidation()
        }
    }

    private val detector = ScaleGestureDetector(context, listener)

    fun onTouchEvent(event: MotionEvent) {
        if (!valueAnimator.isRunning) {
            detector.onTouchEvent(event)
        }
    }
}
