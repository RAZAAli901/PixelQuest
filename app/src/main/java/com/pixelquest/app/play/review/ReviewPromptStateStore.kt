package com.pixelquest.app.play.review

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface ReviewPromptStateStore {
    fun lastAttemptAt(): Long
    fun lastAttemptVersionCode(): Int
    fun recordAttempt(atMillis: Long, versionCode: Int)
}

@Singleton
class PrefsReviewPromptStateStore @Inject constructor(
    @ApplicationContext context: Context,
) : ReviewPromptStateStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun lastAttemptAt(): Long = prefs.getLong(KEY_LAST_ATTEMPT_AT, 0L)
    override fun lastAttemptVersionCode(): Int = prefs.getInt(KEY_LAST_ATTEMPT_VERSION, 0)

    override fun recordAttempt(atMillis: Long, versionCode: Int) {
        prefs.edit().putLong(KEY_LAST_ATTEMPT_AT, atMillis).putInt(KEY_LAST_ATTEMPT_VERSION, versionCode).apply()
    }

    private companion object {
        const val PREFS_NAME = "pixelquest_play_review"
        const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
        const val KEY_LAST_ATTEMPT_VERSION = "last_attempt_version"
    }
}
