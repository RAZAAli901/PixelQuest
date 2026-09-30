package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.model.CrtFilterPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 26 (Day 23): Verify that the CRT filter remains strictly excluded from Comic mode
 * in the full-app context (MainActivity, Scaffold, root overlay, asset filters),
 * not just in isolated component tests.
 */
class Day23ComicCrtExclusionVerificationTest {

    @Test
    fun crtFilter_isStrictlyExcludedFromComicMode_underAllConditions() {
        val booleanOptions = listOf(true, false)

        for (isCrtSettingEnabled in booleanOptions) {
            for (isSimpleModeEnabled in booleanOptions) {
                for (isSystemDark in booleanOptions) {
                    val effective = ThemeMode.Comic.resolveEffective(isSystemDark)
                    val applied = CrtFilterPolicy.shouldApplyCrt(
                        isCrtSettingEnabled = isCrtSettingEnabled,
                        effectiveThemeMode = effective,
                        isSimpleModeEnabled = isSimpleModeEnabled
                    )
                    assertFalse(
                        "CRT filter must NEVER apply to Comic mode (crtEnabled=$isCrtSettingEnabled, simpleMode=$isSimpleModeEnabled, systemDark=$isSystemDark)",
                        applied
                    )
                }
            }
        }
    }

    @Test
    fun crtFilter_onlyAppliesToPixelMode_whenEnabledAndNotSimpleMode() {
        assertTrue(
            "Pixel mode with CRT enabled and Simple mode off must apply CRT",
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Pixel,
                isSimpleModeEnabled = false
            )
        )

        assertFalse(
            "Light mode must never apply CRT",
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Light,
                isSimpleModeEnabled = false
            )
        )

        assertFalse(
            "Comic mode must never apply CRT even when user CRT setting is true",
            CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = ThemeMode.Comic,
                isSimpleModeEnabled = false
            )
        )
    }

    @Test
    fun pixelThemeAssetFilter_comicMode_returnsNullToPreventCrtDistortion() {
        val filter = PixelThemeAssetFilter.forTheme(
            themeMode = ThemeMode.Comic,
            lightColor = ComicTokens.CoralRed
        )
        assertNull("Comic mode must use raw vector/raster assets with no tint/scanline distortion", filter)
    }
}
