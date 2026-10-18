package com.pixelquest.app.namaz

import android.content.Context
import android.content.SharedPreferences
import com.pixelquest.app.domain.namaz.NamazReminderOptions
import com.pixelquest.app.domain.namaz.NamazReminderOptionsStore
import com.pixelquest.app.domain.namaz.NamazSound
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** [NamazReminderOptions] in their own SharedPreferences file. The key names are released: add, don't rename. */
@Singleton
class PrefsNamazReminderOptionsStore @Inject constructor(
    @ApplicationContext private val context: Context
) : NamazReminderOptionsStore {

    private val prefs: SharedPreferences by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private fun read() = NamazReminderOptions(
        nudge = prefs.getBoolean(KEY_NUDGE, false),
        batteryTipDismissed = prefs.getBoolean(KEY_BATTERY_TIP_DISMISSED, false),
        sound = NamazSound.fromName(prefs.getString(KEY_SOUND, null))
    )

    override val options: Flow<NamazReminderOptions> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun get(): NamazReminderOptions = read()

    override suspend fun update(change: (NamazReminderOptions) -> NamazReminderOptions) {
        val next = change(read())
        prefs.edit()
            .putBoolean(KEY_NUDGE, next.nudge)
            .putBoolean(KEY_BATTERY_TIP_DISMISSED, next.batteryTipDismissed)
            .putString(KEY_SOUND, next.sound.name)
            .apply()
    }

    companion object {
        const val PREFS = "pixelquest_namaz_reminders"
        private const val KEY_NUDGE = "nudge"
        private const val KEY_BATTERY_TIP_DISMISSED = "battery_tip_dismissed"
        private const val KEY_SOUND = "sound"
    }
}
