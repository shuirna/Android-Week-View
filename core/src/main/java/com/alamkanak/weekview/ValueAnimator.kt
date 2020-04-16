package com.alamkanak.weekview

import android.animation.Animator
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
        onUpdate: (Int) -> Unit,
        onEnd: () -> Unit = {}
    ) {
        valueAnimator?.cancel()

        valueAnimator = AndroidValueAnimator.ofInt(fromValue, toValue).apply {
            setDuration(duration)
            interpolator = DecelerateInterpolator()

            addUpdateListener {
                val value = it.animatedValue as Int
                onUpdate(value)
            }

            addEndListener { onEnd() }

            start()
        }
    }

    fun stop() {
        valueAnimator?.cancel()
    }
}

private fun AndroidValueAnimator.addEndListener(listener: (AndroidValueAnimator) -> Unit) {
    addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) {
            listener(animator as AndroidValueAnimator)
        }

        override fun onAnimationEnd(animator: Animator) = Unit
        override fun onAnimationCancel(animator: Animator) = Unit
        override fun onAnimationRepeat(animator: Animator) = Unit
    })
}
