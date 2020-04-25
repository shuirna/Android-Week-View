package com.alamkanak.weekview.util

import android.text.SpannableString
import com.alamkanak.weekview.ResolvedWeekViewEvent
import com.alamkanak.weekview.ResourceResolver
import com.alamkanak.weekview.WeekViewEvent

internal class FakeResourceResolver : ResourceResolver {

    override fun resolve(colorResource: WeekViewEvent.ColorResource?): Int? = 0

    override fun resolve(dimenResource: WeekViewEvent.DimenResource?): Int? = 0

    override fun resolve(
        style: WeekViewEvent.Style
    ): ResolvedWeekViewEvent.Style = ResolvedWeekViewEvent.Style()

    override fun resolve(
        textResource: WeekViewEvent.TextResource?
    ): SpannableString? {
        val valueResource = textResource as? WeekViewEvent.TextResource.Value
        return SpannableString.valueOf(valueResource?.text ?: "")
    }
}
