package com.pixelquest.app.quran.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** A place in the Quran the player read: a surah, and the ayah first in view. */
data class LastRead(val surah: Int, val ayah: Int)

/** Where the player last read, kept on the phone so they can carry on from there. */
interface LastReadStore {
    /** The last place read; null before they've read anything. */
    val lastRead: Flow<LastRead?>

    suspend fun save(place: LastRead)
}

/**
 * The last place read, beside the reading settings in their DataStore (`quran_reading`). A place
 * outside the Quran (a surah past the 114th, an ayah before the first) reads back as none.
 */
@Singleton
class DataStoreLastReadStore @Inject constructor(
    private val store: DataStore<Preferences>
) : LastReadStore {

    override val lastRead: Flow<LastRead?> = store.data.map { prefs ->
        val surah = prefs[SURAH]
        val ayah = prefs[AYAH]
        if (surah != null && ayah != null && surah in 1..QuranImporter.SURAHS && ayah >= 1) LastRead(surah, ayah) else null
    }

    override suspend fun save(place: LastRead) {
        store.edit { prefs ->
            prefs[SURAH] = place.surah
            prefs[AYAH] = place.ayah
        }
    }

    private companion object {
        val SURAH = intPreferencesKey("last_read_surah")
        val AYAH = intPreferencesKey("last_read_ayah")
    }
}
