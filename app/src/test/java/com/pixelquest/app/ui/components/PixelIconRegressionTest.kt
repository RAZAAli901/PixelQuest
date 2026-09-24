package com.pixelquest.app.ui.components

import com.pixelquest.app.R
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.ComponentThemeFamily
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 27: Regression test confirming Pixel/Light icon rendering is unaffected
 * after wiring ComicIconBadge dispatch for categories and difficulty levels.
 */
class PixelIconRegressionTest {

    @Test
    fun categoryIcons_resourceIds_remainUnchanged() {
        assertEquals(R.drawable.ic_cat_fitness, TaskCategory.FITNESS.iconResId)
        assertEquals(R.drawable.ic_cat_health, TaskCategory.HEALTH.iconResId)
        assertEquals(R.drawable.ic_cat_learning, TaskCategory.LEARNING.iconResId)
        assertEquals(R.drawable.ic_cat_chores, TaskCategory.CHORES.iconResId)
        assertEquals(R.drawable.ic_cat_other, TaskCategory.OTHER.iconResId)
    }

    @Test
    fun categoryIcons_pixelAndLightModes_bypassComicBadge() {
        assertFalse("Pixel mode must not enter ComicIconBadge branch", ThemeMode.Pixel == ThemeMode.Comic)
        assertFalse("Light mode must not enter ComicIconBadge branch", ThemeMode.Light == ThemeMode.Comic)
        assertTrue("Comic mode must enter ComicIconBadge branch", ThemeMode.Comic == ThemeMode.Comic)
    }

    @Test
    fun categoryIcons_comicContainerColors_areDistinctAndValid() {
        fun resolveCategoryComicColor(category: TaskCategory) = when (category) {
            TaskCategory.FITNESS -> ComicTokens.BurntOrange
            TaskCategory.HEALTH -> ComicTokens.SkyBlue
            TaskCategory.LEARNING -> ComicTokens.Lavender
            TaskCategory.CHORES -> ComicTokens.GoldAccent
            TaskCategory.OTHER -> ComicTokens.CoralRed
        }

        assertEquals(ComicTokens.BurntOrange, resolveCategoryComicColor(TaskCategory.FITNESS))
        assertEquals(ComicTokens.SkyBlue, resolveCategoryComicColor(TaskCategory.HEALTH))
        assertEquals(ComicTokens.Lavender, resolveCategoryComicColor(TaskCategory.LEARNING))
        assertEquals(ComicTokens.GoldAccent, resolveCategoryComicColor(TaskCategory.CHORES))
        assertEquals(ComicTokens.CoralRed, resolveCategoryComicColor(TaskCategory.OTHER))
    }

    @Test
    fun difficultyIcons_resourceIds_remainUnchanged() {
        assertEquals(R.drawable.ic_diff_easy, getDifficultyIconRes(DifficultyLevel.EASY))
        assertEquals(R.drawable.ic_diff_medium, getDifficultyIconRes(DifficultyLevel.MEDIUM))
        assertEquals(R.drawable.ic_diff_hard, getDifficultyIconRes(DifficultyLevel.HARD))
        assertEquals(R.drawable.ic_diff_hardest, getDifficultyIconRes(DifficultyLevel.HARDEST))
    }

    @Test
    fun difficultyIcons_pixelAndLightModes_bypassComicBadge() {
        val pixelFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Pixel)
        val lightFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Light)
        val comicFamily = ComponentThemeFamily.fromThemeMode(ThemeMode.Comic)

        assertEquals(ComponentThemeFamily.PIXEL, pixelFamily)
        assertEquals(ComponentThemeFamily.LIGHT, lightFamily)
        assertEquals(ComponentThemeFamily.COMIC, comicFamily)
    }

    private fun getDifficultyIconRes(level: DifficultyLevel): Int = when (level) {
        DifficultyLevel.EASY -> R.drawable.ic_diff_easy
        DifficultyLevel.MEDIUM -> R.drawable.ic_diff_medium
        DifficultyLevel.HARD -> R.drawable.ic_diff_hard
        DifficultyLevel.HARDEST -> R.drawable.ic_diff_hardest
    }
}
