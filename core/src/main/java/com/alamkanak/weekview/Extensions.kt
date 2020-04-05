package com.alamkanak.weekview

import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.AlignmentSpan
import android.util.SparseArray
import java.util.Calendar

internal fun <T> WeekViewViewState<T>.getOrCreateDateLabel(
    key: Int,
    date: Calendar
): String = cache.dateLabels.get(key) { provideAndCacheLabel(key, date) }

internal fun <T> WeekViewViewState<T>.getOrCreateDateLabel2(
    key: Int,
    date: Calendar
): StaticLayout = cache.dateLabels2.get(key) { provideAndCacheLabel2(key, date) }

internal fun <T> WeekViewViewState<T>.provideAndCacheLabel(key: Int, date: Calendar): String {
    val label = dateFormatter(date, numberOfVisibleDays)
    cache.dateLabels[key] = label as String
    return label as String
}

internal fun <T> WeekViewViewState<T>.provideAndCacheLabel2(key: Int, date: Calendar): StaticLayout {
    val label = dateFormatter(date, numberOfVisibleDays).toSpannableString()
    val layout = label.toTextLayout(
        textPaint = headerTextPaint,
        alignment = Layout.Alignment.ALIGN_CENTER,
        width = widthPerDay
    )
    cache.dateLabels2[key] = layout
    return layout
}

private fun CharSequence.toSpannableString(): SpannableString {
    return SpannableString.valueOf(this).apply {
        setSpan(AlignmentSpan { Layout.Alignment.ALIGN_CENTER }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

internal fun <E> SparseArray<E>.get(key: Int, providerIfEmpty: () -> E): E {
    return get(key) ?: providerIfEmpty.invoke()
}

internal operator fun <E> SparseArray<E>.set(key: Int, value: E) {
    put(key, value)
}
