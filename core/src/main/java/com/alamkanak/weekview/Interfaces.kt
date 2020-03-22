package com.alamkanak.weekview

import android.graphics.Canvas

internal interface Updater {
    fun isRequired(viewState: WeekViewViewState): Boolean
    fun update(viewState: WeekViewViewState)
}

internal interface Drawer {
    fun draw(viewState: WeekViewViewState, canvas: Canvas)
}

internal interface CachingDrawer : Drawer {
    fun clear(viewState: WeekViewViewState)
}
