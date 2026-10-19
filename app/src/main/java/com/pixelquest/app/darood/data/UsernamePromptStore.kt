package com.pixelquest.app.darood.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Which accounts were already asked to choose a username on this phone: each is asked once. */
interface UsernamePromptStore {
    fun wasPrompted(accountId: String): Boolean
    fun markPrompted(accountId: String)
}

@Singleton
class PrefsUsernamePromptStore @Inject constructor(
    @ApplicationContext context: Context
) : UsernamePromptStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun wasPrompted(accountId: String): Boolean =
        prefs.getStringSet(KEY_PROMPTED, emptySet()).orEmpty().contains(accountId)

    override fun markPrompted(accountId: String) {
        val prompted = prefs.getStringSet(KEY_PROMPTED, emptySet()).orEmpty() + accountId
        prefs.edit().putStringSet(KEY_PROMPTED, prompted).apply()
    }

    private companion object {
        const val PREFS = "pixelquest_darood"
        const val KEY_PROMPTED = "username_prompted_accounts"
    }
}
