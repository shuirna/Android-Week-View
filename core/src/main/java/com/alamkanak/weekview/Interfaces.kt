package com.alamkanak.weekview

import android.graphics.Canvas

internal interface Updater<T> {
    fun isRequired(viewState: WeekViewViewState<T>): Boolean
    fun update(viewState: WeekViewViewState<T>)
}

internal interface Drawer<T> {
    fun draw(viewState: WeekViewViewState<T>, canvas: Canvas)
}
