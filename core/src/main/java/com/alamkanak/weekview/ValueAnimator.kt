package com.alamkanak.weekview

import android.animation.ValueAnimator as AndroidValueAnimator
import android.view.animation.DecelerateInterpolator

internal class ValueAnimator {

    private var valueAnimator: AndroidValueAnimator? = null

    val isRunning: Boolean
        get() = valueAnimator?.isStarted ?: false

    fun animate(
        fromValue: Int,
        toValue: Int,
        duration: Long = 300,
        onUpdate: (Int) -> Unit
    ) {
        valueAnimator?.cancel()

        valueAnimator = AndroidValueAnimator.ofInt(fromValue, toValue).apply {
            setDuration(duration)
            interpolator = DecelerateInterpolator()

            addUpdateListener {
                val value = it.animatedValue as Int
                onUpdate(value)
            }

            start()
        }
    }

    fun stop() {
        valueAnimator?.cancel()
    }
}
