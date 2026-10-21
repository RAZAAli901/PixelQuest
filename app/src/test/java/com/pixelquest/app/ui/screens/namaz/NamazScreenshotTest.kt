package com.pixelquest.app.ui.screens.namaz

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.namazViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Screenshots of the Namaz screen and each button state in every look, drawn with Robolectric's
 * native graphics and saved to app/build/outputs/namaz-screenshots for review. The project has no
 * screenshot-comparison library, and golden images drift between machines' font rendering, so
 * the checks are that each picture has something in it and that the four states look different.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h1500dp-xhdpi")
class NamazScreenshotTest(private val look: Look) {

    enum class Look(val mode: ThemeMode, val simpleMode: Boolean, val dark: Boolean) {
        PIXEL(ThemeMode.Pixel, simpleMode = false, dark = true),
        COMIC(ThemeMode.Comic, simpleMode = false, dark = false),
        LIGHT(ThemeMode.Light, simpleMode = false, dark = false),
        SIMPLE(ThemeMode.Comic, simpleMode = true, dark = false),
        SIMPLE_DARK(ThemeMode.Pixel, simpleMode = true, dark = true)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun looks() = Look.entries.map { arrayOf<Any>(it) }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val out = File("build/outputs/namaz-screenshots/${look.name.lowercase()}").apply { mkdirs() }

    /** The whole window, drawn in software (PixelCopy, behind captureToImage, never finishes in Robolectric). */
    private fun window(): Bitmap {
        val view = composeTestRule.activity.window.decorView.rootView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeTestRule.runOnUiThread { view.draw(Canvas(bitmap)) }
        return bitmap
    }

    private fun Bitmap.cropTo(tag: String): Bitmap {
        val bounds = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        val left = bounds.left.toInt().coerceIn(0, width - 1)
        val top = bounds.top.toInt().coerceIn(0, height - 1)
        return Bitmap.createBitmap(this, left, top, bounds.width.toInt().coerceAtMost(width - left), bounds.height.toInt().coerceAtMost(height - top))
    }

    private fun Bitmap.save(name: String) {
        File(out, "$name.png").outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Has a picture in it: plenty of different colours, not one flat fill. */
    private fun Bitmap.assertDrawn(what: String) {
        val colours = HashSet<Int>()
        for (x in 0 until width step 3) for (y in 0 until height step 3) colours += getPixel(x, y)
        assertTrue("$look $what is blank (${colours.size} colours)", colours.size > 20)
    }

    /** The share of pixels that differ between two pictures of the same size. */
    private fun difference(a: Bitmap, b: Bitmap): Double {
        val w = minOf(a.width, b.width)
        val h = minOf(a.height, b.height)
        var differing = 0
        for (x in 0 until w) for (y in 0 until h) if (a.getPixel(x, y) != b.getPixel(x, y)) differing++
        return differing.toDouble() / (w * h)
    }

    @Test
    fun eachButtonState_isDrawn_andLooksDifferent() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = look.mode) {
                CompositionLocalProvider(LocalNamazSimpleMode provides look.simpleMode) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PrayerButtonState.entries.forEach { state ->
                            PrayerButton(
                                prayer = Prayer.ASR,
                                time = "3:50 PM",
                                state = state,
                                onClick = {},
                                modifier = Modifier.testTag(state.name)
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val window = window()

        val shots = PrayerButtonState.entries.associateWith { state -> window.cropTo(state.name) }
        shots.forEach { (state, shot) ->
            shot.save("button_${state.name.lowercase()}")
            shot.assertDrawn("$state button")
        }
        shots.keys.toList().let { states ->
            for (i in states.indices) for (j in i + 1 until states.size) {
                val diff = difference(shots.getValue(states[i]), shots.getValue(states[j]))
                assertTrue("$look: ${states[i]} and ${states[j]} look the same (${"%.4f".format(diff)})", diff > 0.005)
            }
        }
    }

    @Test
    fun theWholeScreen_isDrawn() {
        val day = LocalDate.of(2026, 10, 17)
        val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
        runBlocking {
            repository.logPrayer(day, Prayer.FAJR, 0)
            Prayer.entries.forEach { repository.logPrayer(day.minusDays(1), it, 0) }
        }
        val viewModel = namazViewModel(
            repository,
            FixedClock(LocalDateTime.of(day, java.time.LocalTime.of(16, 0))),
            settings = FakeSettingsRepository(simpleMode = look.simpleMode)
        )
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = look.mode) { NamazScreen(viewModel = viewModel) }
        }
        composeTestRule.waitForIdle()

        val screen = window()
        screen.save("screen")
        screen.assertDrawn("screen")
    }

    @Test
    fun theScreen_whileOff_isDrawn() {
        val viewModel = namazViewModel(
            FakeNamazRepository(NamazSettingsEntity(enabled = false)),
            FixedClock(LocalDateTime.of(2026, 10, 17, 16, 0)),
            settings = FakeSettingsRepository(simpleMode = look.simpleMode)
        )
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = look.mode) { NamazScreen(viewModel = viewModel) }
        }
        composeTestRule.waitForIdle()

        val screen = window()
        screen.save("screen_off")
        screen.assertDrawn("screen while off")
    }
}
