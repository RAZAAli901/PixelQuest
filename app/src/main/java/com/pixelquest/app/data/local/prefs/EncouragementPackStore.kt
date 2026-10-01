package com.pixelquest.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.pixelquest.app.domain.ai.HabitInsightTone
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** The day's AI-written reminder lines, kept locally so reminders never wait on the network. */
data class EncouragementPack(
    val generatedOn: LocalDate,
    val tone: HabitInsightTone,
    val messages: List<String>
)

@Singleton
class EncouragementPackStore internal constructor(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) :
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    fun save(pack: EncouragementPack) {
        prefs.edit()
            .putString(KEY_DATE, pack.generatedOn.toString())
            .putString(KEY_TONE, pack.tone.name)
            .putString(KEY_MESSAGES, JSONArray(pack.messages).toString())
            .apply()
    }

    fun load(): EncouragementPack? {
        val date = prefs.getString(KEY_DATE, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val tone = prefs.getString(KEY_TONE, null)
            ?.let { name -> HabitInsightTone.values().firstOrNull { it.name == name } } ?: return null
        val array = runCatching { JSONArray(prefs.getString(KEY_MESSAGES, "[]")) }.getOrNull() ?: return null
        val messages = (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        return EncouragementPack(date, tone, messages)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "pixelquest_encouragement_pack"
        private const val KEY_DATE = "generated_on"
        private const val KEY_TONE = "tone"
        private const val KEY_MESSAGES = "messages"
    }
}
