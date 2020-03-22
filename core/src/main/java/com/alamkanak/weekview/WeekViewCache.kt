package com.alamkanak.weekview

import android.text.StaticLayout
import android.util.SparseArray
import androidx.collection.ArrayMap

internal data class WeekViewCache<T>(
    val allDayEventLayouts: ArrayMap<EventChip<T>, StaticLayout> = ArrayMap(),
    val dateLabels: SparseArray<String> = SparseArray(),
    val multiLineDayLabels: SparseArray<StaticLayout> = SparseArray()
)
