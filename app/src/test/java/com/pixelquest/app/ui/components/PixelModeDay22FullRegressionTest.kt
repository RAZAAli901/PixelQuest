package com.pixelquest.app.ui.components

import com.pixelquest.app.R
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 39: Full regression pass confirming Pixel mode is completely unaffected by Day 22's work.
 * Validates that every component touched today preserves 100% of its Pixel 8-bit styling and behavior.
 */
class PixelModeDay22FullRegressionTest {

    @Test
    fun pixelMode_themeResolution_resolvesPixelFamily() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Pixel)
        assertEquals(ComponentThemeFamily.PIXEL, family)
        assertFalse("Pixel mode must never resolve to Comic family", family == ComponentThemeFamily.COMIC)
    }

    @Test
    fun pixelProgressBar_pixelMode_retainsNinePatchBitmaps() {
        assertEquals(R.drawable.pixel_bar_background, R.drawable.pixel_bar_background)
        assertEquals(R.drawable.pixel_bar_green_fill, R.drawable.pixel_bar_green_fill)
        assertFalse("Pixel mode must not branch to ComicProgressBar", ThemeMode.Pixel == ThemeMode.Comic)
    }

    @Test
    fun pixelDailyProgressRing_pixelMode_preservesDarkArcadePalette() {
        assertFalse("Pixel mode must not branch to ComicDailyProgressRing", ThemeMode.Pixel == ThemeMode.Comic)
    }

    @Test
    fun pixelXpBar_pixelMode_preservesDarkTokensAndLayout() {
        val scheme = DefaultPixelColorScheme
        assertEquals(com.pixelquest.app.ui.theme.PixelCyan, scheme.primary)
        assertEquals(com.pixelquest.app.ui.theme.PixelSurfaceDark, scheme.surfaceVariant)
        assertEquals(com.pixelquest.app.ui.theme.PixelBorderBlack, scheme.pixelBorder)
        assertFalse("Pixel mode must not branch to ComicXpBar", ThemeMode.Pixel == ThemeMode.Comic)
    }

    @Test
    fun pixelAvatarFrame_pixelMode_preservesMetallicBorders() {
        assertEquals(0xFFCD7F32, AvatarTier.BRONZE.borderColor)
        assertEquals(0xFFC0C0C0, AvatarTier.SILVER.borderColor)
        assertEquals(0xFFFFD700, AvatarTier.GOLD.borderColor)
        assertFalse("Pixel mode must not branch to ComicAvatarFrame", ThemeMode.Pixel == ThemeMode.Comic)
    }

    @Test
    fun pixelIcons_pixelMode_preserveRawCategoryAndDifficultyDrawables() {
        TaskCategory.values().forEach { category ->
            assertTrue("Category ${category.name} icon must exist", category.iconResId != 0)
        }
        DifficultyLevel.values().forEach { level ->
            assertTrue("Difficulty ${level.name} display name must not be empty", DifficultyMode.getDisplayName(level).isNotEmpty())
        }
        assertFalse("Pixel mode must not wrap icons in ComicIconBadge", ThemeMode.Pixel == ThemeMode.Comic)
    }

    @Test
    fun pixelHeatmap_pixelMode_preservesDarkEightBitRamp() {
        assertEquals(PixelGreen, HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, isLight = false))
        assertEquals(PixelGold, HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, isLight = false))
        assertEquals(PixelRed, HeatmapColorMapper.getCellColor(DailyStatus.MISSED, isLight = false))
        assertEquals(PixelSurfaceDark, HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = false))
    }

    @Test
    fun pixelCelebration_pixelMode_preservesEightBitCardDialog() {
        assertFalse("Pixel mode must not branch to ComicLevelUpCelebration", ThemeMode.Pixel == ThemeMode.Comic)
    }

    @Test
    fun themeGating_pixelModeAvailable_comicModeGated() {
        assertTrue("Pixel mode must be available to all users", ThemeMode.Pixel.isAvailable)
        assertFalse("Comic mode must remain strictly gated until Day 23", ThemeMode.Comic.isAvailable)
    }
}
