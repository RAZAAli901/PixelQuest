package com.pixelquest.app.domain.namaz

import androidx.annotation.StringRes
import com.pixelquest.app.R

/** The five daily prayers, in the order they fall in a day (docs/namaz-design.md). */
enum class Prayer(@StringRes val displayName: Int) {
    FAJR(R.string.prayer_fajr),
    DHUHR(R.string.prayer_dhuhr),
    ASR(R.string.prayer_asr),
    MAGHRIB(R.string.prayer_maghrib),
    ISHA(R.string.prayer_isha);

    /** The prayer after this one the same day; null after Isha (the next is tomorrow's Fajr). */
    val nextInDay: Prayer? get() = entries.getOrNull(ordinal + 1)
}
