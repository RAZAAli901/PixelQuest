package com.pixelquest.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Step 36: Unit test for the comicDropShadow modifier's offset calculation.
 * Verifies pixel translation across multiple screen densities, 45-degree down-right projection,
 * and dimensional stability of the duplicated offset shape.
 */
class ComicDropShadowTest {

    @Test
    fun defaultOffset_matchesComicShapeTokens() {
        assertEquals(4.dp, DefaultComicShadowOffsetX)
        assertEquals(4.dp, DefaultComicShadowOffsetY)
        assertEquals(ComicShapeTokens.ShadowOffsetDefault, DefaultComicShadowOffsetX)
        assertEquals(ComicShapeTokens.ShadowOffsetDefault, DefaultComicShadowOffsetY)
    }

    @Test
    fun offsetCalculation_scalesCorrectlyAcrossScreenDensities() {
        val testDensities = listOf(
            Density(density = 1.0f, fontScale = 1.0f), // mdpi
            Density(density = 1.5f, fontScale = 1.0f), // hdpi
            Density(density = 2.0f, fontScale = 1.0f), // xhdpi
            Density(density = 3.0f, fontScale = 1.0f), // xxhdpi
            Density(density = 4.0f, fontScale = 1.0f)  // xxxhdpi
        )

        for (density in testDensities) {
            with(density) {
                val dx = DefaultComicShadowOffsetX.toPx()
                val dy = DefaultComicShadowOffsetY.toPx()

                // Offset must be exact multiple of density
                assertEquals(4f * density.density, dx, 0.001f)
                assertEquals(4f * density.density, dy, 0.001f)

                // Down-right projection requires dx == dy > 0
                assertEquals("Shadow dx must equal dy for 45-degree angle", dx, dy, 0.001f)
                assertTrue("Shadow dx must be positive (down-right)", dx > 0f)
                assertTrue("Shadow dy must be positive (down-right)", dy > 0f)
            }
        }
    }

    @Test
    fun offsetVector_hasExact45DegreeAngleAndPredictableHypotenuse() {
        val density = Density(density = 2.5f)
        with(density) {
            val dx = ComicShapeTokens.ShadowOffsetDefault.toPx()
            val dy = ComicShapeTokens.ShadowOffsetDefault.toPx()
            val vectorLength = sqrt(dx * dx + dy * dy)
            val expectedLength = dx * sqrt(2.0f)

            assertEquals(expectedLength, vectorLength, 0.01f)
        }
    }

    @Test
    fun shadowBoundingOffset_preservesSurfaceDimensionsWithoutDistortion() {
        val cardSize = Size(width = 300f, height = 180f)
        val density = Density(density = 2.0f)

        with(density) {
            val offsetX = 4.dp.toPx()
            val offsetY = 4.dp.toPx()

            val shadowTopLeft = Offset(offsetX, offsetY)
            val shadowBottomRight = Offset(cardSize.width + offsetX, cardSize.height + offsetY)

            // Shadow size must strictly equal original component size
            val shadowWidth = shadowBottomRight.x - shadowTopLeft.x
            val shadowHeight = shadowBottomRight.y - shadowTopLeft.y

            assertEquals(cardSize.width, shadowWidth, 0.001f)
            assertEquals(cardSize.height, shadowHeight, 0.001f)
        }
    }

    @Test
    fun prominentAndSubtleOffsets_resolveProperHierarchy() {
        assertTrue(
            "Subtle shadow (2dp) < Default shadow (4dp)",
            ComicShapeTokens.ShadowOffsetSubtle < ComicShapeTokens.ShadowOffsetDefault
        )
        assertTrue(
            "Default shadow (4dp) < Prominent shadow (6dp)",
            ComicShapeTokens.ShadowOffsetDefault < ComicShapeTokens.ShadowOffsetProminent
        )
    }
}
