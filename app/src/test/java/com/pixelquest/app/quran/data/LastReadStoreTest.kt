package com.pixelquest.app.quran.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.pixelquest.app.quran.ui.reader.ReadingSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The last place read in its DataStore: none at first, kept, and never a place outside the Quran. */
class LastReadStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope) { temp.newFile("quran_reading.preferences_pb") } }
    private val store by lazy { DataStoreLastReadStore(dataStore) }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun beforeAnyReading_thereIsNone() = runBlocking {
        assertNull(store.lastRead.first())
    }

    @Test
    fun theLastPlace_comesBack() = runBlocking {
        store.save(LastRead(2, 120))
        store.save(LastRead(36, 12))

        assertEquals(LastRead(36, 12), store.lastRead.first())
    }

    @Test
    fun itSitsBesideTheReadingSettings_withoutTouchingThem() = runBlocking {
        val settings = DataStoreReadingSettingsStore(dataStore)
        val chosen = ReadingSettings(arabicSize = 36f)
        settings.save(chosen)

        store.save(LastRead(2, 255))

        assertEquals(chosen, settings.settings.first())
        assertEquals(LastRead(2, 255), store.lastRead.first())
    }

    @Test
    fun aPlaceOutsideTheQuran_readsBackAsNone() = runBlocking {
        dataStore.edit {
            it[intPreferencesKey("last_read_surah")] = 115
            it[intPreferencesKey("last_read_ayah")] = 1
        }
        assertNull(store.lastRead.first())

        dataStore.edit {
            it[intPreferencesKey("last_read_surah")] = 2
            it[intPreferencesKey("last_read_ayah")] = 0
        }
        assertNull(store.lastRead.first())
    }
}

/** The last place read, in memory, for tests. */
class MemoryLastReadStore(initial: LastRead? = null) : LastReadStore {
    override val lastRead = MutableStateFlow(initial)
    override suspend fun save(place: LastRead) {
        lastRead.value = place
    }
}
