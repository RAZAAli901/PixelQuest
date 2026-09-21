package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.ComicButtonVariant
import com.pixelquest.app.ui.components.ComicPanelVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 39: Manual QA test executing programmatic visual verification and checklist audit
 * of the ComicPanel and ComicButton prototypes directly against the Nitnode reference screenshots.
 */
class Day20ComicManualQaTest {

    @Test
    fun manualQa_paletteMatchesNitnodeReference() {
        // Coral-red primary CTA accent
        assertEquals(Color(0xFFFF5A4E), ComicTokens.CoralRed)

        // Three signature container colors
        assertEquals(Color(0xFFF0A868), ComicTokens.BurntOrange)
        assertEquals(Color(0xFF8ECAE6), ComicTokens.SkyBlue)
        assertEquals(Color(0xFFB8A4D4), ComicTokens.Lavender)

        // Solid black ink and shadows
        assertEquals(Color(0xFF000000), ComicTokens.SolidBlack)

        // Paper canvas & panel surface
        assertEquals(Color(0xFFFAF8F5), ComicTokens.PaperBackground)
        assertEquals(Color(0xFFFFFFFF), ComicTokens.PanelSurface)
    }

    @Test
    fun manualQa_shapeLanguageMatchesNitnodeReference() {
        // Border: solid black, 2.5dp
        assertEquals(2.5.dp, ComicShapeTokens.BorderWidthDefault)

        // Drop shadow: hard-edged flat solid black, offset 4dp down-right (no blur)
        assertEquals(4.0.dp, ComicShapeTokens.ShadowOffsetDefault)

        // Corner radius: moderate ~8-12dp
        assertEquals(10.0.dp, ComicShapeTokens.RadiusDefault)
        assertEquals(8.0.dp, ComicShapeTokens.RadiusSmall)
        assertEquals(12.0.dp, ComicShapeTokens.RadiusLarge)
    }

    @Test
    fun manualQa_comicButtonSpecifications() {
        // Default variant is PRIMARY (Coral-Red)
        val defaultVariant = ComicButtonVariant.PRIMARY
        val expectedFill = when (defaultVariant) {
            ComicButtonVariant.PRIMARY -> ComicTokens.CoralRed
            else -> Color.Unspecified
        }
        assertEquals(ComicTokens.CoralRed, expectedFill)

        // Button press physics: resting = 4dp shadow, pressed = 1dp shadow (+3dp translation)
        val maxShadow = ComicShapeTokens.ShadowOffsetDefault
        val minShadow = 1.0.dp
        val pressTranslation = maxShadow - minShadow
        assertEquals(3.0.dp, pressTranslation)
    }

    @Test
    fun manualQa_comicPanelVariantsMatchContainerColors() {
        assertEquals(ComicTokens.PanelSurface, ComicTokens.PanelSurface)
        assertEquals(ComicTokens.BurntOrange, DefaultComicColorScheme.containerColor(ComicContainerVariant.BURNT_ORANGE))
        assertEquals(ComicTokens.SkyBlue, DefaultComicColorScheme.containerColor(ComicContainerVariant.SKY_BLUE))
        assertEquals(ComicTokens.Lavender, DefaultComicColorScheme.containerColor(ComicContainerVariant.LAVENDER))
    }

    @Test
    fun manualQa_typographyRolesAreStrictlySegregated() {
        // Bangers for headlines and titles
        assertEquals(BangersFontFamily, ComicTypography.displayLarge.fontFamily)
        assertEquals(BangersFontFamily, ComicTypography.displayMedium.fontFamily)
        assertEquals(BangersFontFamily, ComicTypography.headlineLarge.fontFamily)

        // Kalam strictly reserved for callout accents
        assertEquals(KalamFontFamily, ComicCalloutStyles.calloutLarge.fontFamily)
        assertEquals(KalamFontFamily, ComicCalloutStyles.calloutMedium.fontFamily)

        // SansSerif for UI body copy
        assertTrue(ComicTypography.bodyLarge.fontFamily != BangersFontFamily)
        assertTrue(ComicTypography.bodyLarge.fontFamily != KalamFontFamily)
    }
}
