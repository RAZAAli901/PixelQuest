package com.pixelquest.app.quran.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.quran.ui.reader.DisplayMode
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The reading settings in their DataStore: kept between visits, read back within their ranges. */
class ReadingSettingsStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope) { temp.newFile("quran_reading.preferences_pb") } }
    private val store by lazy { DataStoreReadingSettingsStore(dataStore) }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun withNothingKept_theyreTheReadersDefaults() = runBlocking {
        assertEquals(ReadingSettings(), store.settings.first())
    }

    @Test
    fun whatsSaved_comesBack() = runBlocking {
        val chosen = ReadingSettings(arabicSize = 36f, translationSize = 20f, lineSpacing = 1.2f, displayMode = DisplayMode.ARABIC, paper = com.pixelquest.app.quran.ui.reader.ReaderPaper.SEPIA, keepScreenOn = true)
        store.save(chosen)

        assertEquals(chosen, store.settings.first())
    }

    @Test
    fun valuesOutOfRange_orUnknown_comeBackSafe() = runBlocking {
        dataStore.edit {
            it[floatPreferencesKey("arabic_size")] = 400f
            it[floatPreferencesKey("line_spacing")] = 0f
            it[stringPreferencesKey("display_mode")] = "UPSIDE_DOWN"
            it[stringPreferencesKey("paper")] = "PLAID"
        }

        val read = store.settings.first()
        assertEquals(com.pixelquest.app.quran.ui.reader.ReaderPaper.THEME, read.paper)
        assertEquals(QuranTypography.ARABIC_MAX, read.arabicSize)
        assertEquals(QuranTypography.SPACING_MIN, read.lineSpacing)
        assertEquals(DisplayMode.BOTH, read.displayMode)
    }
}

/** A reading settings store in memory, for view models' tests. */
class MemoryReadingSettingsStore(initial: ReadingSettings = ReadingSettings()) : ReadingSettingsStore {
    override val settings = MutableStateFlow(initial)
    override suspend fun save(settings: ReadingSettings) {
        this.settings.value = settings
    }
}
