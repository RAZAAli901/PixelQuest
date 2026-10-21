package com.pixelquest.app.islamic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerMoment
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.ui.screens.namaz.NamazSummary
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Screenshots of Deen (UI v2, mock 2D) in both skins, plain and in Simple Mode, drawn with
 * Robolectric's native graphics and saved to app/build/outputs/islamic-screenshots for review. As
 * with the chat's, golden images would drift between machines' fonts, so the checks are that each
 * has a picture in it, the Namaz card is across the top, and the tiles are two to a row (one in
 * Simple Mode).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h860dp-xhdpi")
class IslamicHubScreenshotTest(private val look: Look) {

    enum class Look(val skin: PqSkin, val simpleMode: Boolean) {
        COMIC(PqSkin.Comic, simpleMode = false),
        PIXEL(PqSkin.Pixel, simpleMode = false),
        SIMPLE(PqSkin.Comic, simpleMode = true),
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun looks() = Look.entries.map { arrayOf<Any>(it) }

        val items = listOf(
            IslamicItem(
                "namaz", R.string.namaz_title, R.string.islamic_namaz_description, IslamicIcon("🕌"), "islamic/namaz",
                badgeCount = 2, badgeLabel = R.plurals.islamic_namaz_badge
            ),
            IslamicItem(
                "darood", R.string.darood_title, R.string.islamic_darood_description, IslamicIcon("📿"), "islamic/darood",
                badgeCount = 1, badgeLabel = R.plurals.islamic_darood_badge
            ),
            IslamicItem("quran", R.string.quran_title, R.string.quran_description, IslamicIcon("📖"), "islamic/quran")
        )

        private val isha = ZonedDateTime.of(2026, 10, 20, 18, 46, 0, 0, ZoneId.of("UTC"))
        val prayer = NamazSummary(
            NextPrayer(PrayerMoment(Prayer.MAGHRIB, isha.toLocalDate(), isha.minusHours(1)), PrayerMoment(Prayer.ISHA, isha.toLocalDate(), isha), Duration.ofMinutes(36), isha.toLocalDate()),
            prayed = 4, prayedPrayers = setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB), allFiveStreak = 2
        )
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun resetSkin() {
        PqSkinState.skin = PqSkin.Comic
    }

    @Test
    fun deen_isDrawn() {
        PqSkinState.skin = look.skin
        composeTestRule.setContent {
            IslamicHubContent(
                state = IslamicHubUiState(items = items, isSimpleMode = look.simpleMode, loaded = true),
                onOpen = {},
                showComingSoon = true,
                prayer = prayer,
                quran = DeenQuran("Al-Baqara", 142, 286),
                today = LocalDate.of(2026, 10, 20),
            )
        }
        composeTestRule.waitForIdle()

        val view = composeTestRule.activity.window.decorView.rootView
        val window = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeTestRule.runOnUiThread { view.draw(Canvas(window)) }
        File("build/outputs/islamic-screenshots").apply { mkdirs() }.let { dir ->
            File(dir, "deen-${look.name.lowercase()}.png").outputStream().use { window.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        val colours = HashSet<Int>()
        for (x in 0 until window.width step 3) for (y in 0 until window.height step 3) colours += window.getPixel(x, y)
        assertTrue("$look is blank (${colours.size} colours)", colours.size > 20)

        fun badges() = composeTestRule.onAllNodes(hasTestTag(IslamicTestTags.BADGE), useUnmergedTree = true).fetchSemanticsNodes().size
        assertEquals("$look: Namaz's and Darood's badges", 2, badges())

        val namaz = composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).getBoundsInRoot()
        val darood = composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).getBoundsInRoot()
        val quran = composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).getBoundsInRoot()
        assertTrue("$look: the Namaz card first", darood.top >= namaz.bottom)
        if (look.simpleMode) {
            assertEquals("$look: one a row", darood.left, quran.left)
            assertTrue("$look: the Quran below Darood", quran.top >= darood.bottom)
        } else {
            assertEquals("$look: two a row", darood.top, quran.top)
        }
    }
}
