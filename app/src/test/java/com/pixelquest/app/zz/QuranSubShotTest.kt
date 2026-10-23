package com.pixelquest.app.zz

import com.pixelquest.app.quran.data.BundledTranslation
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.data.QuranSources
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.domain.Translation
import com.pixelquest.app.quran.ui.about.QuranSourcesScreen
import com.pixelquest.app.quran.ui.about.QuranSourcesUiState
import com.pixelquest.app.quran.ui.list.GoToAyahContent
import com.pixelquest.app.quran.ui.list.GoToCheck
import com.pixelquest.app.quran.ui.reader.ReaderUiState
import com.pixelquest.app.quran.ui.reader.ReadingSettings
import com.pixelquest.app.quran.ui.reader.ReadingSettingsContent
import com.pixelquest.app.quran.ui.reader.SurahReaderScreen
import com.pixelquest.app.quran.ui.search.AyahResult
import com.pixelquest.app.quran.ui.search.QuranSearchScreen
import com.pixelquest.app.quran.ui.search.QuranSearchUiState
import com.pixelquest.app.quran.ui.translations.TranslationPickerScreen
import com.pixelquest.app.quran.ui.translations.TranslationPickerUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

/** Placeholder text only: no ayah is typed here. */
class QuranSubShotTest : ScratchShot() {
    private val surah = Surah(2, "-", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 87, null)
    private val ayah = Ayah(8, 2, 1, "placeholder arabic", "placeholder translation text for the layout", "en.test")

    @Test fun search() = shot("q-search", PqSkin.Comic) {
        PixelQuestTheme { QuranSearchScreen("text", QuranSearchUiState("text", surahs = listOf(surah), ayahs = listOf(AyahResult(ayah, "Al-Baqara"))), {}, {}, {}, { _, _ -> }, focusOnOpen = false) }
    }
    @Test fun searchPixel() = shot("q-search", PqSkin.Pixel) {
        PixelQuestTheme { QuranSearchScreen("text", QuranSearchUiState("text", surahs = listOf(surah), ayahs = listOf(AyahResult(ayah, "Al-Baqara"))), {}, {}, {}, { _, _ -> }, focusOnOpen = false) }
    }

    private val sources = QuranSourcesUiState(
        sources = QuranSources("v1", "# A NOTICE\n# kept whole", "Metadata notice"),
        translation = Translation("en.sahih", "Saheeh International", "Saheeh International", "English", "April 24, 2011", "Non-commercial use only.", "# Its notice")
    )
    @Test fun sources() = shot("q-sources", PqSkin.Comic) { PixelQuestTheme { QuranSourcesScreen(sources, {}) } }

    private val picker = TranslationPickerUiState(
        options = listOf(BundledTranslation("en.sahih", "Saheeh International", "Saheeh International", "English", "f"), BundledTranslation("ur.jalandhry", "Jalandhry", "Fateh Muhammad Jalandhry", "Urdu", "g")),
        selected = "en.sahih", onPhone = sources.translation, loaded = true
    )
    @Test fun picker() = shot("q-picker", PqSkin.Comic) { PixelQuestTheme { TranslationPickerScreen(picker, {}, {}) } }
    @Test fun pickerPixel() = shot("q-picker", PqSkin.Pixel) { PixelQuestTheme { TranslationPickerScreen(picker, {}, {}) } }

    @Test fun settings() = shot("q-settings", PqSkin.Comic) { PixelQuestTheme { ReadingSettingsContent(settings = ReadingSettings(), preview = "-" to "placeholder", onChange = {}) } }
    @Test fun goTo() = shot("q-goto", PqSkin.Comic) {
        PixelQuestTheme { GoToAyahContent(surah = "2", ayah = "300", check = GoToCheck.of("2", "300", mapOf(2 to 286)), onSurah = {}, onAyah = {}, onGo = { _, _ -> }, onCancel = {}) }
    }
    @Test fun reader() = shot("q-reader", PqSkin.Comic) { PixelQuestTheme { SurahReaderScreen(state = ReaderUiState(surah = surah, ayahs = listOf(ayah)), onNavigateBack = {}) } }
    @Test fun readerPixel() = shot("q-reader", PqSkin.Pixel) { PixelQuestTheme { SurahReaderScreen(state = ReaderUiState(surah = surah, ayahs = listOf(ayah)), onNavigateBack = {}) } }
}
