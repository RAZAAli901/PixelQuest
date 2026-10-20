package com.pixelquest.app.quran.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.quran.ui.reader.DisplayMode
import com.pixelquest.app.quran.ui.reader.ReaderPaper
import com.pixelquest.app.quran.ui.reader.ReadingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** How the player reads the Quran, kept on the phone between visits. */
interface ReadingSettingsStore {
    val settings: Flow<ReadingSettings>

    suspend fun save(settings: ReadingSettings)
}

/**
 * The reading settings in a Preferences DataStore of their own (`quran_reading`). A value is read
 * back within its range, whatever was saved, and a missing one is the reader's default. It's a
 * preference of this phone: Android's backup doesn't copy it (backup_rules.xml lists what it does).
 */
@Singleton
class DataStoreReadingSettingsStore @Inject constructor(
    private val store: DataStore<Preferences>
) : ReadingSettingsStore {

    override val settings: Flow<ReadingSettings> = store.data.map { prefs ->
        ReadingSettings(
            arabicSize = (prefs[ARABIC_SIZE] ?: QuranTypography.ARABIC_DEFAULT)
                .coerceIn(QuranTypography.ARABIC_MIN, QuranTypography.ARABIC_MAX),
            translationSize = (prefs[TRANSLATION_SIZE] ?: QuranTypography.TRANSLATION_DEFAULT)
                .coerceIn(QuranTypography.TRANSLATION_MIN, QuranTypography.TRANSLATION_MAX),
            lineSpacing = (prefs[LINE_SPACING] ?: QuranTypography.SPACING_DEFAULT)
                .coerceIn(QuranTypography.SPACING_MIN, QuranTypography.SPACING_MAX),
            displayMode = prefs[DISPLAY_MODE]?.let { name -> DisplayMode.entries.firstOrNull { it.name == name } }
                ?: DisplayMode.BOTH,
            paper = prefs[PAPER]?.let { name -> ReaderPaper.entries.firstOrNull { it.name == name } } ?: ReaderPaper.THEME
        )
    }

    override suspend fun save(settings: ReadingSettings) {
        store.edit { prefs ->
            prefs[ARABIC_SIZE] = settings.arabicSize
            prefs[TRANSLATION_SIZE] = settings.translationSize
            prefs[LINE_SPACING] = settings.lineSpacing
            prefs[DISPLAY_MODE] = settings.displayMode.name
            prefs[PAPER] = settings.paper.name
        }
    }

    companion object {
        /** The DataStore's name: its file is files/datastore/quran_reading.preferences_pb. */
        const val NAME = "quran_reading"

        private val ARABIC_SIZE = floatPreferencesKey("arabic_size")
        private val TRANSLATION_SIZE = floatPreferencesKey("translation_size")
        private val LINE_SPACING = floatPreferencesKey("line_spacing")
        private val DISPLAY_MODE = stringPreferencesKey("display_mode")
        private val PAPER = stringPreferencesKey("paper")
    }
}
