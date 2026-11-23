package com.pixelquest.app.play.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface UpdatePromptStateStore {
    fun canOfferFlexibleUpdate(nowMillis: Long): Boolean
    fun recordFlexibleDecline(nowMillis: Long)
}

@Singleton
class PrefsUpdatePromptStateStore @Inject constructor(
    @ApplicationContext context: Context,
) : UpdatePromptStateStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun canOfferFlexibleUpdate(nowMillis: Long): Boolean =
        nowMillis - prefs.getLong(KEY_LAST_FLEXIBLE_DECLINE, 0L) >= COOLDOWN_MILLIS

    override fun recordFlexibleDecline(nowMillis: Long) {
        prefs.edit().putLong(KEY_LAST_FLEXIBLE_DECLINE, nowMillis).apply()
    }

    private companion object {
        const val PREFS_NAME = "pixelquest_play_updates"
        const val KEY_LAST_FLEXIBLE_DECLINE = "last_flexible_decline"
        const val COOLDOWN_MILLIS = 24L * 60 * 60 * 1000
    }
}
