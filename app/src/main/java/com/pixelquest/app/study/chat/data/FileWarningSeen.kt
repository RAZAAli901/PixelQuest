package com.pixelquest.app.study.chat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The parties whose files the player has been warned about on this phone (docs/study-chat.md):
 * files come from other people, so the first one opened from each party asks first.
 */
interface FileWarningSeen {
    fun seen(partyId: String): Boolean
    fun markSeen(partyId: String)
}

@Singleton
class PrefsFileWarningSeen @Inject constructor(
    @ApplicationContext context: Context
) : FileWarningSeen {
    private val prefs = context.getSharedPreferences("pixelquest_study_chat", Context.MODE_PRIVATE)

    override fun seen(partyId: String): Boolean = partyId in prefs.getStringSet(KEY, emptySet()).orEmpty()

    @Synchronized
    override fun markSeen(partyId: String) {
        prefs.edit().putStringSet(KEY, prefs.getStringSet(KEY, emptySet()).orEmpty() + partyId).apply()
    }

    private companion object {
        const val KEY = "file_warning_seen"
    }
}

/** In memory, for tests. */
class MemoryFileWarningSeen : FileWarningSeen {
    private val seen = mutableSetOf<String>()
    override fun seen(partyId: String) = partyId in seen
    override fun markSeen(partyId: String) {
        seen += partyId
    }
}
