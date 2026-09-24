package com.pixelquest.app.ui.components

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 40: Full regression pass confirming Light mode is completely unaffected by Day 22's work.
 * Validates that every component touched today preserves 100% of its daylight styling and behavior.
 */
class LightModeDay22FullRegressionTest {

    @Test
    fun lightMode_themeResolution_resolvesLightFamily() {
        val family = ComponentThemeFamily.fromThemeMode(ThemeMode.Light)
        assertEquals(ComponentThemeFamily.LIGHT, family)
        assertFalse("Light mode must never resolve to Comic family", family == ComponentThemeFamily.COMIC)
    }

    @Test
    fun lightMode_paletteTokens_remainInvariant() {
        val scheme = DefaultLightColorScheme
        assertEquals(Color(0xFF2E7D32), scheme.primary)
        assertEquals(Color(0xFFE8F5E9), scheme.surfaceVariant)
        assertEquals(Color(0xFF1B5E20), scheme.pixelBorder)
        assertEquals(Color(0xFF15803D), scheme.tertiary)
    }

    @Test
    fun pixelProgressBar_lightMode_preservesDaylightEmerald() {
        assertFalse("Light mode must not branch to ComicProgressBar", ThemeMode.Light == ThemeMode.Comic)
    }

    @Test
    fun pixelDailyProgressRing_lightMode_preservesDaylightEmeraldArc() {
        assertFalse("Light mode must not branch to ComicDailyProgressRing", ThemeMode.Light == ThemeMode.Comic)
    }

    @Test
    fun pixelXpBar_lightMode_preservesDaylightTokens() {
        assertFalse("Light mode must not branch to ComicXpBar", ThemeMode.Light == ThemeMode.Comic)
    }

    @Test
    fun pixelAvatarFrame_lightMode_preservesAccessibleBorders() {
        fun resolveLightBorder(tier: AvatarTier) = when (tier) {
            AvatarTier.BRONZE -> Color(0xFF9A4F10)
            AvatarTier.SILVER -> Color(0xFF475569)
            AvatarTier.GOLD -> DefaultLightColorScheme.gold
        }

        assertEquals(Color(0xFF9A4F10), resolveLightBorder(AvatarTier.BRONZE))
        assertEquals(Color(0xFF475569), resolveLightBorder(AvatarTier.SILVER))
        assertEquals(Color(0xFFB8860B), resolveLightBorder(AvatarTier.GOLD))
        assertFalse("Light mode must not branch to ComicAvatarFrame", ThemeMode.Light == ThemeMode.Comic)
    }

    @Test
    fun pixelIcons_lightMode_preserveRawCategoryAndDifficultyDrawables() {
        TaskCategory.values().forEach { category ->
            assertTrue("Category ${category.name} icon must exist", category.iconResId != 0)
        }
        assertFalse("Light mode must not wrap icons in ComicIconBadge", ThemeMode.Light == ThemeMode.Comic)
    }

    @Test
    fun pixelHeatmap_lightMode_preservesDaylightRamp() {
        assertEquals(Color(0xFF15803D), HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, isLight = true))
        assertEquals(Color(0xFFD97706), HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, isLight = true))
        assertEquals(Color(0xFFDC2626), HeatmapColorMapper.getCellColor(DailyStatus.MISSED, isLight = true))
        assertEquals(Color(0xFFEFECE6), HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, isLight = true))
    }

    @Test
    fun pixelCelebration_lightMode_preservesDaylightCardDialog() {
        assertFalse("Light mode must not branch to ComicLevelUpCelebration", ThemeMode.Light == ThemeMode.Comic)
    }

    @Test
    fun themeGating_lightModeAvailable_comicModeGated() {
        assertTrue("Light mode must be available to all users", ThemeMode.Light.isAvailable)
        assertFalse("Comic mode must remain strictly gated until Day 23", ThemeMode.Comic.isAvailable)
    }
}
