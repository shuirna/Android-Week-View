package com.alamkanak.weekview

import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.StyleSpan
import androidx.emoji.text.EmojiCompat

private val emojiCompat: EmojiCompat? = try {
    EmojiCompat.get()
} catch (e: IllegalStateException) {
    // EmojiCompat is not set up in this project
    null
}

internal fun SpannableString.emojify() = emojiCompat?.process(this) as SpannableString

internal val StaticLayout.lineHeight: Int
    get() = height / lineCount

internal fun SpannableString.ellipsized(
    textPaint: TextPaint,
    availableArea: Int,
    where: TextUtils.TruncateAt = TextUtils.TruncateAt.END
) = when (val result = TextUtils.ellipsize(this, textPaint, availableArea.toFloat(), where)) {
    is SpannableStringBuilder -> result.build()
    is SpannableString -> result
    else -> throw IllegalStateException("Invalid ellipsize result: ${result::class.simpleName}")
}

internal fun SpannableString.setSpan(
    styleSpan: StyleSpan
) = setSpan(styleSpan, 0, length, 0)

internal fun CharSequence.bold() = SpannableString(this).apply {
    setSpan(StyleSpan(Typeface.BOLD))
}

internal fun SpannableStringBuilder.build(): SpannableString = SpannableString.valueOf(this)

fun CharSequence.toTextLayout(
    textPaint: TextPaint,
    width: Int,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    spacingMultiplier: Float = 1f,
    spacingExtra: Float = 0f,
    includePad: Boolean = false
) = if (Build.VERSION.SDK_INT >= 23) {
    StaticLayout.Builder
        .obtain(this, 0, this.length, textPaint, width)
        .setAlignment(alignment)
        .setLineSpacing(spacingExtra, spacingMultiplier)
        .setIncludePad(includePad)
        .build()
} else {
    @Suppress("DEPRECATION")
    StaticLayout(this, textPaint, width, alignment, spacingMultiplier, spacingExtra, includePad)
}
