package com.alamkanak.weekview

import androidx.emoji.text.EmojiCompat

private val emojiCompat: EmojiCompat? = try {
    EmojiCompat.get()
} catch (e: IllegalStateException) {
    // EmojiCompat is not set up in this project
    null
}

internal fun CharSequence.emojify(): CharSequence = emojiCompat?.process(this) ?: this
