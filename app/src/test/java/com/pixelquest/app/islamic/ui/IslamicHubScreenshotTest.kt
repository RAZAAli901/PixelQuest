package com.pixelquest.app.islamic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pixelquest.app.R
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
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
 * Screenshots of the Islamic hub in every look, drawn with Robolectric's native graphics and saved
 * to app/build/outputs/islamic-screenshots for review. As with the chat's, golden images would drift
 * between machines' fonts, so the checks are that each has a picture in it, and that Pixel and Comic
 * draw their icons while the other looks keep the emoji.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h860dp-xhdpi")
class IslamicHubScreenshotTest(private val look: Look) {

    enum class Look(val mode: ThemeMode, val dark: Boolean, val simpleMode: Boolean, val drawn: Boolean) {
        PIXEL(ThemeMode.Pixel, dark = true, simpleMode = false, drawn = true),
        COMIC(ThemeMode.Comic, dark = false, simpleMode = false, drawn = true),
        LIGHT(ThemeMode.Light, dark = false, simpleMode = false, drawn = false),
        SIMPLE(ThemeMode.Light, dark = false, simpleMode = true, drawn = false),
        SIMPLE_DARK(ThemeMode.Pixel, dark = true, simpleMode = true, drawn = false)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun looks() = Look.entries.map { arrayOf<Any>(it) }

        val items = listOf(
            IslamicItem(
                "namaz", R.string.namaz_title, R.string.islamic_namaz_description,
                IslamicIcon("🕌", R.drawable.ic_namaz_pixel, R.drawable.ic_namaz_comic), "islamic/namaz",
                badgeCount = 2, badgeLabel = R.plurals.islamic_namaz_badge
            ),
            IslamicItem(
                "darood", R.string.darood_title, R.string.islamic_darood_description,
                IslamicIcon("📿", R.drawable.ic_darood_pixel, R.drawable.ic_darood_comic), "islamic/darood",
                badgeCount = 1, badgeLabel = R.plurals.islamic_darood_badge
            ),
            IslamicItem(
                "quran", R.string.quran_title, R.string.quran_description,
                IslamicIcon("📖", R.drawable.ic_quran_pixel, R.drawable.ic_quran_comic), "islamic/quran"
            )
        )
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theHub_isDrawn_withItsLooksIcons() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = look.mode, darkTheme = look.dark) {
                IslamicHubContent(
                    state = IslamicHubUiState(items = items, isSimpleMode = look.simpleMode, loaded = true),
                    onOpen = {},
                    showComingSoon = true
                )
            }
        }
        composeTestRule.waitForIdle()

        val view = composeTestRule.activity.window.decorView.rootView
        val window = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeTestRule.runOnUiThread { view.draw(Canvas(window)) }
        File("build/outputs/islamic-screenshots").apply { mkdirs() }.let { dir ->
            File(dir, "hub-${look.name.lowercase()}.png").outputStream().use { window.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        val colours = HashSet<Int>()
        for (x in 0 until window.width step 3) for (y in 0 until window.height step 3) colours += window.getPixel(x, y)
        assertTrue("$look is blank (${colours.size} colours)", colours.size > 20)

        val emoji = listOf("🕌", "📿", "📖").sumOf { e ->
            composeTestRule.onAllNodes(hasText(e), useUnmergedTree = true).fetchSemanticsNodes().size
        }
        assertEquals("$look: emoji shown", if (look.drawn) 0 else 3, emoji)
    }
}
