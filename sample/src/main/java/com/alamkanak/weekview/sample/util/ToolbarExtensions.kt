@file:JvmName("ToolbarUtils")
package com.alamkanak.weekview.sample.util

import android.app.Activity
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.sample.R
import dev.chrisbanes.insetter.doOnApplyWindowInsets

private enum class WeekViewType(val value: Int) {
    DayView(1),
    ThreeDayView(3),
    WeekView(7);

    companion object {
        fun create(
            numberOfVisibleDays: Int
        ): WeekViewType = values().first { it.value == numberOfVisibleDays }
    }
}

fun Toolbar.setup(activity: Activity) {
    title = activity.label

    if (!activity.isTaskRoot) {
        setNavigationIcon(R.drawable.ic_arrow_back)
        setNavigationOnClickListener { activity.onBackPressed() }
    }

    doOnApplyWindowInsets { view, insets, initialState ->
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = initialState.margins.top + insets.systemWindowInsetTop
        }
    }
}

fun Toolbar.setupWithWeekView(weekView: WeekView<*>, activity: Activity) {
    setup(activity)

    var currentViewType = WeekViewType.create(weekView.numberOfVisibleDays)

    inflateMenu(R.menu.menu_main)
    setOnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.action_today -> {
                weekView.goToToday()
                // TODO Fix weekView.goToCurrentTime()
                true
            }
            else -> {
                val viewType = item.mapToWeekViewType()
                if (viewType != currentViewType) {
                    item.isChecked = !item.isChecked
                    currentViewType = viewType
                    weekView.numberOfVisibleDays = viewType.value
                }
                true
            }
        }
    }
}

private val Activity.label: String
    get() = getString(packageManager.getActivityInfo(componentName, 0).labelRes)

private fun MenuItem.mapToWeekViewType(): WeekViewType {
    return when (itemId) {
        R.id.action_day_view -> WeekViewType.DayView
        R.id.action_three_day_view -> WeekViewType.ThreeDayView
        R.id.action_week_view -> WeekViewType.WeekView
        else -> throw IllegalArgumentException("Invalid menu item ID $itemId")
    }
}
