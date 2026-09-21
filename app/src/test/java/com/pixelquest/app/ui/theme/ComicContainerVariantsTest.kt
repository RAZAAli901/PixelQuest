package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Step 33: Validate that all three reference container colors (burnt orange, sky blue, lavender)
 * work as usable container variants within [ComicColorScheme], not just as one-off card colors.
 */
class ComicContainerVariantsTest {

    private val scheme = DefaultComicColorScheme

    @Test
    fun containerVariants_mapDirectlyToSemanticSchemeTokens() {
        // Burnt orange is primaryContainer
        assertEquals(ComicTokens.BurntOrange, scheme.primaryContainer)
        assertEquals(ComicTokens.BurntOrange, scheme.burntOrange)

        // Sky blue is secondary / secondaryContainer
        assertEquals(ComicTokens.SkyBlue, scheme.secondary)
        assertEquals(ComicTokens.SkyBlue, scheme.secondaryContainer)
        assertEquals(ComicTokens.SkyBlue, scheme.skyBlue)

        // Lavender is tertiary / tertiaryContainer
        assertEquals(ComicTokens.Lavender, scheme.tertiary)
        assertEquals(ComicTokens.Lavender, scheme.tertiaryContainer)
        assertEquals(ComicTokens.Lavender, scheme.lavender)
    }

    @Test
    fun containerColor_resolvesCorrectlyForEachVariant() {
        assertEquals(
            ComicTokens.BurntOrange,
            scheme.containerColor(ComicContainerVariant.BURNT_ORANGE)
        )
        assertEquals(
            ComicTokens.SkyBlue,
            scheme.containerColor(ComicContainerVariant.SKY_BLUE)
        )
        assertEquals(
            ComicTokens.Lavender,
            scheme.containerColor(ComicContainerVariant.LAVENDER)
        )
    }

    @Test
    fun containerForIndex_cyclesPredictablyThroughAllThreeVariants() {
        assertEquals(ComicTokens.BurntOrange, scheme.containerForIndex(0))
        assertEquals(ComicTokens.SkyBlue, scheme.containerForIndex(1))
        assertEquals(ComicTokens.Lavender, scheme.containerForIndex(2))
        assertEquals(ComicTokens.BurntOrange, scheme.containerForIndex(3))
        assertEquals(ComicTokens.SkyBlue, scheme.containerForIndex(4))
        assertEquals(ComicTokens.Lavender, scheme.containerForIndex(5))

        // Negative index handling
        assertEquals(ComicTokens.SkyBlue, scheme.containerForIndex(-1))
    }

    @Test
    fun containerColors_areDistinctFromEachOther() {
        val colors = listOf(scheme.burntOrange, scheme.skyBlue, scheme.lavender)

        // Ensure all are distinct colors
        assertNotEquals(colors[0], colors[1])
        assertNotEquals(colors[0], colors[2])
        assertNotEquals(colors[1], colors[2])

        // Verify Euclidean distance in RGB color space is significant (> 0.2f) for clarity
        fun distance(c1: Color, c2: Color): Float {
            val dr = c1.red - c2.red
            val dg = c1.green - c2.green
            val db = c1.blue - c2.blue
            return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
        }

        assertTrue("Burnt orange vs Sky blue distance", distance(colors[0], colors[1]) > 0.3f)
        assertTrue("Burnt orange vs Lavender distance", distance(colors[0], colors[2]) > 0.3f)
        assertTrue("Sky blue vs Lavender distance", distance(colors[1], colors[2]) > 0.2f)
    }

    @Test
    fun containerColors_maintainHighContrastInkBorderSeparation() {
        // All containers must share the same high-contrast black border token
        assertEquals(ComicTokens.SolidBlack, scheme.comicBorder)
        assertEquals(ComicTokens.SolidBlack, scheme.pixelBorder)
        assertEquals(ComicTokens.SolidBlack, scheme.comicShadow)
    }
}
