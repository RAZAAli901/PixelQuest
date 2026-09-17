package com.pixelquest.app.domain.model

import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 35: Unit test for the CRT-filter-forced-off-in-Simple-Mode logic from Section B step 11.
 * Confirms that CrtFilterPolicy forces CRT filter OFF whenever Simple Mode is enabled,
 * regardless of whether the user previously enabled CRT or what theme is active.
 */
class CrtFilterSimpleModeSuppressionTest {

    @Test
    fun testSimpleModeActive_forcesCrtFilterOff_underAllConditions() {
        // Simple Mode = true with CRT setting enabled and Pixel theme -> MUST be false
        assertFalse(
            "CRT must be forced OFF in Simple Mode even if setting is enabled in Pixel theme",
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Pixel,
                isSimpleModeEnabled = true
            )
        )

        // Simple Mode = true with CRT setting enabled in other themes -> false
        assertFalse(
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Dark,
                isSimpleModeEnabled = true
            )
        )
        assertFalse(
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Light,
                isSimpleModeEnabled = true
            )
        )

        // Simple Mode = true with CRT setting disabled -> false
        assertFalse(
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = false,
                effectiveThemeMode = ThemeMode.Pixel,
                isSimpleModeEnabled = true
            )
        )
    }

    @Test
    fun testGamifiedMode_respectsCrtSettingAndTheme() {
        // Gamified Mode (Simple Mode = false) with Pixel theme and CRT enabled -> true
        assertTrue(
            "CRT should be enabled when Simple Mode is false, CRT enabled, and Pixel theme active",
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Pixel,
                isSimpleModeEnabled = false
            )
        )

        // Gamified Mode with CRT setting disabled -> false
        assertFalse(
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = false,
                effectiveThemeMode = ThemeMode.Pixel,
                isSimpleModeEnabled = false
            )
        )

        // Gamified Mode with non-Pixel themes -> false (CRT scanlines are Pixel-theme exclusive)
        assertFalse(
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Dark,
                isSimpleModeEnabled = false
            )
        )
        assertFalse(
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Light,
                isSimpleModeEnabled = false
            )
        )
    }
}
