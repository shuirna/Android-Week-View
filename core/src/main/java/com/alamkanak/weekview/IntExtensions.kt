package com.alamkanak.weekview

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal fun Int.scaleBy(factor: Float): Int = (this * factor).roundToInt()

internal fun Int.limit(minValue: Int, maxValue: Int): Int = min(max(this, minValue), maxValue)
