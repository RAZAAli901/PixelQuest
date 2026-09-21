package com.pixelquest.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 37: Unit test for the comicBorder modifier.
 * Verifies default border token alignment, shape integration, modifier chaining,
 * and high-contrast ink specifications.
 */
class ComicBorderTest {

    @Test
    fun defaultBorderTokens_matchComicDesignTokens() {
        assertEquals(2.5.dp, DefaultComicBorderWidth)
        assertEquals(ComicShapeTokens.BorderWidthDefault, DefaultComicBorderWidth)
        assertEquals(Color.Black, DefaultComicBorderColor)
        assertEquals(ComicTokens.SolidBlack, DefaultComicBorderColor)
        assertEquals(10.dp, DefaultComicCornerRadius)
        assertEquals(ComicShapeTokens.RadiusDefault, DefaultComicCornerRadius)
    }

    @Test
    fun borderWidthHierarchy_isStrictlyOrdered() {
        assertTrue(
            "Default border width (2.5dp) must be greater than zero",
            ComicShapeTokens.BorderWidthDefault > 0.dp
        )
        assertTrue(
            "Thick border width (3.5dp) must be greater than default (2.5dp)",
            ComicShapeTokens.BorderWidthThick > ComicShapeTokens.BorderWidthDefault
        )
    }

    @Test
    fun comicBorderModifier_chainsSuccessfullyOnModifier() {
        val baseModifier: Modifier = Modifier

        // 1. Default modifier call
        val withDefaultBorder = baseModifier.comicBorder()
        assertNotNull(withDefaultBorder)

        // 2. Overload with custom corner radius
        val withCornerRadius = baseModifier.comicBorder(
            width = ComicShapeTokens.BorderWidthThick,
            color = Color.Black,
            cornerRadius = ComicShapeTokens.RadiusSmall
        )
        assertNotNull(withCornerRadius)

        // 3. Overload with custom shape
        val withCustomShape = baseModifier.comicBorder(
            width = 3.dp,
            color = Color.Black,
            shape = RoundedCornerShape(12.dp)
        )
        assertNotNull(withCustomShape)
    }

    @Test
    fun borderStrokeColor_isPureBlackInkForCrispPopArtOutline() {
        // Comic book borders must never be semi-transparent or soft
        assertEquals(1.0f, DefaultComicBorderColor.alpha, 0.0f)
        assertEquals(0.0f, DefaultComicBorderColor.red, 0.0f)
        assertEquals(0.0f, DefaultComicBorderColor.green, 0.0f)
        assertEquals(0.0f, DefaultComicBorderColor.blue, 0.0f)
    }
}
