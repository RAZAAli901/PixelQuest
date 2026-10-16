package com.pixelquest.app.domain.namaz

import androidx.annotation.StringRes
import com.batoulapps.adhan2.Madhab
import com.pixelquest.app.R

/**
 * When Asr starts: Hanafi when a shadow is twice an object's length (later), Shafi, Maliki and
 * Hanbali when it's once its length. Stored by name in NamazSettingsEntity.asrMadhab.
 */
enum class AsrMadhab(@StringRes val displayName: Int, internal val adhan: Madhab) {
    HANAFI(R.string.namaz_madhab_hanafi, Madhab.HANAFI),
    SHAFI(R.string.namaz_madhab_shafi, Madhab.SHAFI);

    companion object {
        val DEFAULT = HANAFI

        /** A stored name back to a madhab; anything unknown is the default. */
        fun fromName(name: String?): AsrMadhab = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
