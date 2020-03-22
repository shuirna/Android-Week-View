package com.alamkanak.weekview

import android.os.Parcel
import android.os.Parcelable
import android.view.View.BaseSavedState

internal class SavedState : BaseSavedState {

    lateinit var viewState: WeekViewViewState

    constructor(superState: Parcelable) : super(superState)

    constructor(
        superState: Parcelable,
        viewState: WeekViewViewState
    ) : super(superState) {
        this.viewState = viewState
    }

    constructor(source: Parcel) : super(source) {
        viewState = checkNotNull(source.readParcelable(WeekViewViewState::class.java.classLoader))
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeParcelable(viewState, flags)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}
