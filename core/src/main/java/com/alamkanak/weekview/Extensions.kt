package com.alamkanak.weekview

import java.util.Calendar

internal fun <T> WeekViewViewState<T>.getOrCreateDateLabel(
    key: Int,
    date: Calendar
): String = cache.dateLabels.get(key) ?: provideAndCacheLabel(key, date)

internal fun <T> WeekViewViewState<T>.provideAndCacheLabel(key: Int, date: Calendar): String {
    val label = dateFormatter(date, numberOfVisibleDays)
    cache.dateLabels[key] = label
    return label
}
