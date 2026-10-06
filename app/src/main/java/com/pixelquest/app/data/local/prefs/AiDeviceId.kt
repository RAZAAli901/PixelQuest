package com.pixelquest.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * A random id this install sends with AI requests through the gemini-proxy Edge Function, which
 * counts calls per device per day. It is made on first use and kept in this install's preferences.
 * It is not tied to the player's account, name or hardware, and it is not in backups, so a new
 * install gets a new id.
 */
class AiDeviceId(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    @Synchronized
    fun get(): String {
        prefs.getString(KEY_DEVICE_ID, null)
            ?.takeIf { isValid(it) }
            ?.let { return it }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    companion object {
        const val PREFS_NAME = "pixelquest_ai_usage"
        private const val KEY_DEVICE_ID = "key_ai_device_id"
        private val FORMAT = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

        fun isValid(id: String): Boolean = FORMAT.matches(id)
    }
}
