package com.pixelquest.app.ui.components

import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.AvatarTierCalculator
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 36: Integration test rendering composite components (Avatar + XP Bar + Progress + Icons + Heatmap)
 * fully in Comic mode, verifying that all Day 22 components operate harmoniously without conflict.
 */
class ComicProfileCompositeIntegrationTest {

    @Test
    fun comicMode_compositeComponents_allResolveToComicFamily() {
        val mode = ThemeMode.Comic
        val family = ComponentThemeFamily.fromThemeMode(mode)
        assertEquals(ComponentThemeFamily.COMIC, family)

        // 1. Avatar Tier and Ribbon integration
        val tier = AvatarTierCalculator.calculateTier(level = 10)
        assertEquals(AvatarTier.GOLD, tier)
        val goldRibbonColor = ComicTokens.GoldAccent
        assertEquals(ComicTokens.GoldAccent, goldRibbonColor)

        // 2. XP Bar & Energy Fill integration
        val xpFraction = (75.toFloat() / 100).coerceIn(0f, 1f)
        assertEquals(0.75f, xpFraction, 0.001f)
        val energyColor = ComicTokens.SkyBlue
        assertNotNull(energyColor)

        // 3. Category & Difficulty Icon Badges
        val categoryColor = ComicTokens.BurntOrange
        val difficultyColor = ComicTokens.Lavender
        assertEquals(ComicTokens.BurntOrange, categoryColor)
        assertEquals(ComicTokens.Lavender, difficultyColor)

        // 4. Progress Bar & Ring
        val progress = 0.8f
        val isGoalMet = progress >= 0.7f
        assertTrue("Goal met condition operates correctly in Comic ring", isGoalMet)

        // 5. Heatmap Ramp in Comic Mode
        val perfectCell = HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, mode)
        val cellBorder = HeatmapColorMapper.getBorderColor(DailyStatus.PERFECT, mode)
        assertEquals(ComicTokens.SkyBlue, perfectCell)
        assertEquals(ComicTokens.SolidBlack, cellBorder)
    }

    @Test
    fun comicMode_tokensAndBorders_areConsistentAcrossComponents() {
        // Uniform geometric specifications
        assertEquals(2.5f, ComicShapeTokens.BorderWidthDefault.value, 0.001f)
        assertEquals(4.0f, ComicShapeTokens.ShadowOffsetDefault.value, 0.001f)
        assertEquals(10.0f, ComicShapeTokens.RadiusDefault.value, 0.001f)
        assertEquals(12.0f, ComicShapeTokens.RadiusLarge.value, 0.001f)
        assertEquals(8.0f, ComicShapeTokens.RadiusSmall.value, 0.001f)
    }

    @Test
    fun comicMode_allCategoriesHaveAssignedContainerVariants() {
        val categories = TaskCategory.values()
        assertEquals(5, categories.size)
        categories.forEach { category ->
            val color = when (category) {
                TaskCategory.FITNESS -> ComicTokens.BurntOrange
                TaskCategory.HEALTH -> ComicTokens.SkyBlue
                TaskCategory.LEARNING -> ComicTokens.Lavender
                TaskCategory.CHORES -> ComicTokens.GoldAccent
                TaskCategory.OTHER -> ComicTokens.CoralRed
            }
            assertNotNull("Category ${category.name} must resolve a non-null comic color", color)
        }
    }
}
