package com.pixelquest.app.quran.ui.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranFile
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.TranslationFile
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.LocalQuranSimpleMode
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Screenshots of the reader on Al-Fatihah, from the bundle as it ships: the Arabic alone, the
 * translation alone, and both, in every look (and on sepia paper). Drawn with Robolectric's native
 * graphics and saved to app/build/outputs/quran-screenshots for review. As with the hub's, golden
 * images would drift between machines' fonts, so the checks are that each has a picture in it and
 * shows what its mode shows.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h860dp-xhdpi")
class QuranReaderScreenshotTest(private val look: Look, private val mode: DisplayMode) {

    enum class Look(val theme: ThemeMode, val dark: Boolean, val simpleMode: Boolean = false, val paper: ReaderPaper = ReaderPaper.THEME) {
        PIXEL(ThemeMode.Pixel, dark = true),
        COMIC(ThemeMode.Comic, dark = false),
        LIGHT(ThemeMode.Light, dark = false),
        SIMPLE(ThemeMode.Light, dark = false, simpleMode = true),
        SIMPLE_DARK(ThemeMode.Pixel, dark = true, simpleMode = true),
        SEPIA(ThemeMode.Pixel, dark = true, paper = ReaderPaper.SEPIA)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
        fun cases() = Look.entries.flatMap { look -> DisplayMode.entries.map { arrayOf<Any>(look, it) } }

        private val json = Json { ignoreUnknownKeys = true }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Al-Fatihah, exactly as the bundle holds it. */
    private fun alFatihah(): Pair<Surah, List<Ayah>> {
        val assets = AndroidQuranAssetSource(ApplicationProvider.getApplicationContext())
        val surah = json.decodeFromString<QuranFile>(assets.read(QuranImporter.QURAN_FILE)).surahs.first()
        val translation = json.decodeFromString<TranslationFile>(assets.read(QuranImporter.TRANSLATION_FILE))
        val translated = translation.surahs.first().ayahs
        return Surah(
            surah.number, surah.arabicName, surah.englishName, surah.englishMeaning,
            Revelation.of(surah.revelation), surah.ayahs.size, surah.juz, surah.bismillah
        ) to surah.ayahs.mapIndexed { i, arabic -> Ayah(i + 1, 1, i + 1, arabic, translated[i], translation.id) }
    }

    @Test
    fun theReader_isDrawn_showingWhatItsModeShows() {
        val (surah, ayahs) = alFatihah()
        val state = ReaderUiState(
            surah = surah,
            ayahs = ayahs,
            hasMore = false,
            settings = ReadingSettings(displayMode = mode, paper = look.paper),
            bookmarked = setOf(2)
        )
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = look.theme, darkTheme = look.dark) {
                CompositionLocalProvider(LocalQuranSimpleMode provides look.simpleMode) {
                    SurahReaderScreen(state = state, onNavigateBack = {})
                }
            }
        }
        composeTestRule.waitForIdle()

        val view = composeTestRule.activity.window.decorView.rootView
        val window = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeTestRule.runOnUiThread { view.draw(Canvas(window)) }
        File("build/outputs/quran-screenshots").apply { mkdirs() }.let { dir ->
            File(dir, "reader-${look.name.lowercase()}-${mode.name.lowercase()}.png").outputStream().use { window.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        val colours = HashSet<Int>()
        for (x in 0 until window.width step 3) for (y in 0 until window.height step 3) colours += window.getPixel(x, y)
        assertTrue("$look $mode is blank (${colours.size} colours)", colours.size > 20)

        fun shown(tag: String) = composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size
        assertEquals("$look $mode: the Arabic", if (mode == DisplayMode.TRANSLATION) 0 else 1, shown(ReaderTags.arabic(1)))
        assertEquals("$look $mode: the translation", if (mode == DisplayMode.ARABIC) 0 else 1, shown(ReaderTags.translation(1)))
        assertEquals("$look $mode: the bookmark's ribbon", 1, shown(ReaderTags.RIBBON))
    }
}
