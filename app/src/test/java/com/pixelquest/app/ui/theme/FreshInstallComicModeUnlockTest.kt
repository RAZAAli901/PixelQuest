package com.pixelquest.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 31 (Day 23): Verification that a completely fresh app install can immediately
 * select Comic mode on their first Settings visit, with zero leftover gating artifacts.
 */
class FreshInstallComicModeUnlockTest {

    @Test
    fun freshInstall_comicModeIsImmediatelyAvailable() {
        // Gating policy must be completely unlocked
        assertTrue("ThemeMode.Comic.isAvailable must be true for all users", ThemeMode.Comic.isAvailable)
        assertFalse(
            "ThemeMode.Comic displayName must not contain (Coming Soon)",
            ThemeMode.Comic.displayName.contains("Coming Soon", ignoreCase = true)
        )
    }

    @Test
    fun freshInstall_themeSelectorAllowsDirectSelection() {
        // Given initial state of fresh install
        val initialTheme = ThemeMode.fromId(null)
        assertEquals(ThemeMode.Pixel, initialTheme)

        // User chooses Comic mode on first visit
        val selectedTheme = ThemeMode.fromId("comic")
        assertEquals(ThemeMode.Comic, selectedTheme)

        // Effective runtime mode immediately resolves to Comic
        val effectiveLight = selectedTheme.resolveEffective(isSystemInDark = false)
        val effectiveDark = selectedTheme.resolveEffective(isSystemInDark = true)

        assertEquals(ThemeMode.Comic, effectiveLight)
        assertEquals(ThemeMode.Comic, effectiveDark)
    }

    @Test
    fun freshInstall_comicColorSchemeIsFullyInitialized() {
        val scheme = DefaultComicColorScheme
        assertNotNull(scheme)
        assertEquals(ThemeMode.Comic, scheme.themeMode)
        assertEquals(ComicTokens.CoralRed, scheme.primary)
        assertEquals(ComicTokens.PaperBackground, scheme.background)
        assertEquals(ComicTokens.PanelSurface, scheme.surface)
        assertEquals(ComicTokens.SolidBlack, scheme.comicBorder)
        assertEquals(ComicTokens.SolidBlack, scheme.comicShadow)
    }

    @Test
    fun freshInstall_zeroDebugArtifactsRequiredToAccessComicMode() {
        // Confirmation that no debug flags, toggles, or special preferences are required
        val comicMode = ThemeMode.valueOf("Comic")
        assertTrue(comicMode.isAvailable)
    }
}
