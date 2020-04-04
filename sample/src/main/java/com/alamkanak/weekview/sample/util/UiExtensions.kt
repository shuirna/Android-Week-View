@file:JvmName("UiUtils")
package com.alamkanak.weekview.sample.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.Calendar
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

inline fun <reified T : View> Activity.lazyView(
    @IdRes viewId: Int
): Lazy<T> = lazy { findViewById<T>(viewId) }

inline fun <reified T : View> Fragment.lazyView(
    @IdRes viewId: Int
): Lazy<T> = lazy { requireActivity().findViewById<T>(viewId) }

fun LocalDate.toCalendar(): Calendar {
    val instant = atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
    val calendar = Calendar.getInstance()
    calendar.time = DateTimeUtils.toDate(instant)
    return calendar
}

fun <T> LiveData<T>.observe(owner: LifecycleOwner, observe: (T) -> Unit) {
    observe(owner, Observer { observe(it) })
}

fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun Activity.requestFullscreenLayout() {
    window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

    when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        UI_MODE_NIGHT_NO -> Unit
        UI_MODE_NIGHT_YES -> Unit
    }
}

fun Context.resolveAttribute(@AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}
