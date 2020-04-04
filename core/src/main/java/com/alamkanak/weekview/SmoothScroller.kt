package com.alamkanak.weekview

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator

internal class SmoothScroller {

    private var valueAnimator: ValueAnimator? = null

    val isRunning: Boolean
        get() = valueAnimator?.isStarted ?: false

    fun scroll(
        fromValue: Int,
        toValue: Int,
        duration: Long = 300,
        onUpdate: (Int) -> Unit
    ) {
        stop()

        valueAnimator = ValueAnimator.ofInt(fromValue, toValue).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Int
                onUpdate(value)
            }
        }
        valueAnimator?.start()
    }

    fun stop() {
        valueAnimator?.cancel()
    }
}
