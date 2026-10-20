package com.pixelquest.app.quran.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The reading settings: sizes and spacing on their steps, the display mode, and the reader using them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class ReadingSettingsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var latest = ReadingSettings()

    private fun show() {
        composeTestRule.setContent {
            var settings by mutableStateOf(ReadingSettings())
            PixelQuestTheme {
                ReadingSettingsContent(settings = settings, preview = "ـ" to "made-up", onChange = { settings = it; latest = it })
            }
        }
    }

    private fun slide(tag: String, to: Float) =
        composeTestRule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(to) }

    @Test
    fun theDefaults_areTheReadersOwn() {
        assertEquals(ReadingSettings(QuranTypography.ARABIC_DEFAULT, QuranTypography.TRANSLATION_DEFAULT, 1f, DisplayMode.BOTH), ReadingSettings())
    }

    @Test
    fun theSliders_changeTheSizes_onTheirSteps() {
        show()

        slide(ReaderTags.ARABIC_SIZE, 35.3f)
        slide(ReaderTags.TRANSLATION_SIZE, 19.6f)
        slide(ReaderTags.LINE_SPACING, 1.23f)

        assertEquals(36f, latest.arabicSize)
        assertEquals(20f, latest.translationSize)
        assertEquals(1.2f, latest.lineSpacing, 0.001f)
    }

    @Test
    fun theSliders_sayTheirValue() {
        show()
        slide(ReaderTags.LINE_SPACING, 1.4f)

        val said = composeTestRule.onNodeWithTag(ReaderTags.LINE_SPACING).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals("140%", said)
    }

    @Test
    fun theDisplayMode_isOneOfTheSettings() {
        show()

        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.ARABIC)).performClick()

        assertEquals(DisplayMode.ARABIC, latest.displayMode)
    }

    @Test
    fun aValue_snapsToItsStep() {
        assertEquals(28f, snap(27.3f, 20f, 2f))
        assertEquals(0.9f, snap(0.86f, 0.8f, 0.1f), 0.0001f)
    }

    @Test
    fun theReader_usesWhatsChosen() {
        val repository = NoQuran
        val vm = SurahReaderViewModel(SavedStateHandle(mapOf("surah" to 1)), repository)

        vm.updateSettings(ReadingSettings(arabicSize = 40f, displayMode = DisplayMode.TRANSLATION))

        assertEquals(40f, vm.state.value.settings.arabicSize)
        assertEquals(DisplayMode.TRANSLATION, vm.state.value.displayMode)
    }

    /** A repository with nothing in it, for the settings alone. */
    private object NoQuran : com.pixelquest.app.quran.data.QuranRepository {
        override val isReady = kotlinx.coroutines.flow.flowOf(true)
        override fun surahs() = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelquest.app.quran.domain.Surah>())
        override suspend fun surah(number: Int): com.pixelquest.app.quran.domain.Surah? = null
        override fun juzs() = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelquest.app.quran.domain.Juz>())
        override suspend fun ayahs(surah: Int, page: Int, pageSize: Int) = emptyList<com.pixelquest.app.quran.domain.Ayah>()
        override suspend fun ayahRange(surah: Int, from: Int, to: Int) = emptyList<com.pixelquest.app.quran.domain.Ayah>()
        override suspend fun ayah(surah: Int, number: Int): com.pixelquest.app.quran.domain.Ayah? = null
        override suspend fun searchTranslation(text: String, limit: Int) = emptyList<com.pixelquest.app.quran.domain.Ayah>()
        override fun translations() = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelquest.app.quran.domain.Translation>())
    }
}
