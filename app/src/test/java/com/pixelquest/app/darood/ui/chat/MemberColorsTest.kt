package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ComicSpec
import com.pixelquest.app.ui.theme.PixelSpec
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each member's colour: from their id alone, the same everywhere, and readable in every theme. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MemberColorsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val ids = listOf(
        "0b6f2c1e-4a77-4c1f-9a0e-5d2b7c9e1f01", "7e3d9a40-2b18-4f6c-8d55-1a9c3e7b2d02",
        "c41a8f2b-9e63-4d0a-b7c2-3f5e8d1a6c03", "5f9e2d7c-1a84-4b3e-9c60-8e2d4f7a1b04",
        "a2c7e5f1-6d39-4e8b-a1f4-7b3c9d2e5f05", "e8b4d1a6-3f72-4c95-8e1b-2d6a5c9f3e06"
    )

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test
    fun theColour_comesFromTheIdAlone_theSameEveryTime() {
        for (id in ids) {
            val first = MemberColors.indexOf(id, 8)
            assertEquals(first, MemberColors.indexOf(String(id.toCharArray()), 8))
            assertTrue(first in 0 until 8)
        }
        // A fixed value, so a change to how it's worked out (which would recolour everyone) shows up here.
        assertEquals(Math.floorMod("0b6f2c1e-4a77-4c1f-9a0e-5d2b7c9e1f01".hashCode(), 8), MemberColors.indexOf(ids[0], 8))
    }

    @Test
    fun differentMembers_mostlyGetDifferentColours() {
        val used = ids.map { MemberColors.indexOf(it, 8) }.toSet()
        assertTrue("six members share at most two colours: $used", used.size >= 4)
    }

    @Test
    fun everyColour_readsOnBothSkins_groundAndPaper() {
        for (spec in listOf(ComicSpec, PixelSpec)) {
            for (surface in listOf(spec.palette.ground, spec.palette.paper)) {
                for (colour in MemberColors.palette) {
                    val ratio = contrast(colour, surface)
                    assertTrue("$colour on $surface is ${"%.2f".format(ratio)}:1", ratio >= 4.5)
                }
            }
        }
        assertEquals(ids.map { MemberColors.palette[MemberColors.indexOf(it, 8)] }, ids.map { memberColor(it) })
    }

    @Test
    fun anUnknownPoster_getsThePrimaryTextColour() {
        var unknown = Color.Unspecified
        var text = Color.Unspecified
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                unknown = memberColor(null)
                text = PixelTheme.colors.primaryText
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(text, unknown)
    }
}
