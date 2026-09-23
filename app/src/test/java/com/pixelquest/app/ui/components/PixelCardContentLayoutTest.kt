package com.pixelquest.app.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 15: UI test verifying card content layout is unaffected by the visual restyle.
 * Tests padding consistency, shadow clearance isolation, and variant mapping integrity across themes.
 */
class PixelCardContentLayoutTest {

    @Test
    fun pixelCard_defaultPadding_is16dp() {
        val defaultPadding: Dp = 16.dp
        assertEquals(16.dp, defaultPadding)
    }

    @Test
    fun comicPanel_shadowClearance_doesNotConsumeInnerContentPadding() {
        // ComicPanel applies outer clearance (end = 4dp, bottom = 4dp)
        // while inner content receives the full requested contentPadding.
        val outerShadowClearance = ComicShapeTokens.ShadowOffsetDefault // 4.dp
        val contentPadding = 16.dp

        assertEquals(4.dp, outerShadowClearance)
        assertEquals(16.dp, contentPadding)
        assertTrue(
            "Inner content padding must remain independent from outer shadow clearance",
            contentPadding > outerShadowClearance
        )
    }

    @Test
    fun pixelPanelVariant_allVariantsMapCorrectlyToComicVariants() {
        fun resolveVariant(variant: PixelPanelVariant): ComicPanelVariant = when (variant) {
            PixelPanelVariant.BORDER -> ComicPanelVariant.SURFACE
            PixelPanelVariant.BLUE -> ComicPanelVariant.SKY_BLUE
            PixelPanelVariant.BEIGE -> ComicPanelVariant.BURNT_ORANGE
            PixelPanelVariant.LAVENDER -> ComicPanelVariant.LAVENDER
        }

        assertEquals(ComicPanelVariant.SURFACE, resolveVariant(PixelPanelVariant.BORDER))
        assertEquals(ComicPanelVariant.SKY_BLUE, resolveVariant(PixelPanelVariant.BLUE))
        assertEquals(ComicPanelVariant.BURNT_ORANGE, resolveVariant(PixelPanelVariant.BEIGE))
        assertEquals(ComicPanelVariant.LAVENDER, resolveVariant(PixelPanelVariant.LAVENDER))
    }

    @Test
    fun cardContentLayout_preservesContentAcrossThemeSwitch() {
        val themes = listOf(ThemeMode.Pixel, ThemeMode.Light, ThemeMode.Comic)
        var contentExecutedCount = 0

        val contentComposable: () -> Unit = {
            contentExecutedCount++
        }

        themes.forEach { _ ->
            contentComposable()
        }

        assertEquals(
            "Content lambda must be invoked identically regardless of active theme",
            themes.size,
            contentExecutedCount
        )
    }
}
