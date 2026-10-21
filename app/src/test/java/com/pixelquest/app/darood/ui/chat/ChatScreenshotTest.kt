package com.pixelquest.app.darood.ui.chat

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DayTotals
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Screenshots of a party's chat in every look, drawn with Robolectric's native graphics and saved to
 * app/build/outputs/darood-screenshots for review. As with the Namaz screenshots, golden images
 * would drift between machines' fonts, so the checks are that each has a picture in it, and that
 * the player's bubbles wear that look's own accent.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h860dp-xhdpi")
class ChatScreenshotTest(private val look: Look) {

    enum class Look(val mode: ThemeMode, val dark: Boolean, val simpleMode: Boolean) {
        PIXEL(ThemeMode.Pixel, dark = true, simpleMode = false),
        COMIC(ThemeMode.Comic, dark = false, simpleMode = false),
        SIMPLE(ThemeMode.Comic, dark = false, simpleMode = true),
        SIMPLE_DARK(ThemeMode.Pixel, dark = true, simpleMode = true)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun looks() = Look.entries.map { arrayOf<Any>(it) }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theChat_isDrawn_inItsLooksOwnColours() {
        var n = 1L
        fun entry(user: String, amount: Int, minute: Long, state: EntryState = EntryState.SYNCED) = DaroodEntry(
            n, "c${n++}", "p1", user, amount, LocalDate.of(2026, 10, 19), "UTC",
            Instant.parse("2026-10-19T09:00:00Z").plusSeconds(60 * minute), state
        )
        val entries = listOf(
            entry("bram-id", 300, 1),
            entry("bram-id", 33, 2),
            entry("cleo-id", 1_000, 5),
            entry("aria-id", 313, 7),
            entry("aria-id", 11, 8, EntryState.PENDING)
        )
        val events = listOf(PartyEvent.Joined("cleo-id", "cleo_r", Instant.parse("2026-10-19T09:03:00Z")))
        var accent = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = look.mode) {
                accent = PixelTheme.colors.primary.toArgb()
                PartyChatScreen(
                    partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, memberCount = 3,
                    items = chatItemsOf(entries, "aria-id", ZoneId.of("UTC"), events),
                    usernames = mapOf("aria-id" to "aria_k", "bram-id" to "bram", "cleo-id" to "cleo_r"),
                    today = LocalDate.of(2026, 10, 19), canPost = true, draft = "33",
                    todayTotals = DayTotals(324, 1_657), milestone = 10_000, total = 2_450,
                    simpleMode = look.simpleMode
                )
            }
        }
        composeTestRule.waitForIdle()

        val view = composeTestRule.activity.window.decorView.rootView
        val window = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeTestRule.runOnUiThread { view.draw(Canvas(window)) }
        File("build/outputs/darood-screenshots").apply { mkdirs() }.let { dir ->
            File(dir, "chat-${look.name.lowercase()}.png").outputStream().use { window.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }

        val colours = HashSet<Int>()
        for (x in 0 until window.width step 3) for (y in 0 until window.height step 3) colours += window.getPixel(x, y)
        assertTrue("$look is blank (${colours.size} colours)", colours.size > 20)

        // The player's sent bubble: mostly the look's accent.
        val bounds = composeTestRule.onNodeWithTag(DaroodTestTags.bubble("c4")).fetchSemanticsNode().boundsInWindow
        var inAccent = 0
        var all = 0
        for (x in bounds.left.toInt() until bounds.right.toInt()) for (y in bounds.top.toInt() until bounds.bottom.toInt()) {
            all++
            if (window.getPixel(x, y) == accent) inAccent++
        }
        assertTrue("$look: my bubble is ${100 * inAccent / all}% its accent", inAccent * 2 > all)
    }
}
