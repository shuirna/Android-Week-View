package com.alamkanak.weekview

import android.util.SparseArray
import java.util.Calendar

internal fun <T> WeekViewViewState<T>.getOrCreateDateLabel(
    key: Int,
    date: Calendar
): String = cache.dateLabels.get(key) { provideAndCacheLabel(key, date) }

internal fun <T> WeekViewViewState<T>.provideAndCacheLabel(key: Int, date: Calendar): String {
    val label = dateFormatter(date, numberOfVisibleDays)
    cache.dateLabels[key] = label
    return label
}

internal fun <E> SparseArray<E>.get(key: Int, providerIfEmpty: () -> E): E {
    return get(key) ?: providerIfEmpty.invoke()
}

internal operator fun <E> SparseArray<E>.set(key: Int, value: E) {
    put(key, value)
}
