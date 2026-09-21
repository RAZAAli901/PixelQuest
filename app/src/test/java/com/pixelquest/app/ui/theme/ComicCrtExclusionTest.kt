package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.model.CrtFilterPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 24: Unit test confirming that the retro CRT scanline filter is strictly excluded
 * from ever applying in Comic mode under any configuration or setting state.
 */
class ComicCrtExclusionTest {

    @Test
    fun crtFilter_neverAppliesInComicMode_regardlessOfUserSetting() {
        // CRT setting ON, Simple Mode OFF -> must still be FALSE in Comic mode
        val appliesWhenCrtOn = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = true,
            effectiveThemeMode = ThemeMode.Comic,
            isSimpleModeEnabled = false
        )
        assertFalse("CRT filter must never apply in Comic mode when setting is enabled", appliesWhenCrtOn)

        // CRT setting OFF, Simple Mode OFF -> FALSE
        val appliesWhenCrtOff = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = false,
            effectiveThemeMode = ThemeMode.Comic,
            isSimpleModeEnabled = false
        )
        assertFalse("CRT filter must never apply in Comic mode when setting is disabled", appliesWhenCrtOff)

        // CRT setting ON, Simple Mode ON -> FALSE
        val appliesInSimpleMode = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = true,
            effectiveThemeMode = ThemeMode.Comic,
            isSimpleModeEnabled = true
        )
        assertFalse("CRT filter must never apply in Comic mode with Simple Mode active", appliesInSimpleMode)
    }

    @Test
    fun crtFilter_remainsOperationalForPixelMode() {
        // Pixel mode with CRT setting ON and Simple Mode OFF must be TRUE
        val appliesInPixelMode = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = true,
            effectiveThemeMode = ThemeMode.Pixel,
            isSimpleModeEnabled = false
        )
        assertTrue("CRT filter must remain functional in Pixel mode", appliesInPixelMode)
    }

    @Test
    fun crtFilter_excludedWhenResolvingSystemThemeToComic() {
        for (systemDark in listOf(true, false)) {
            val effective = ThemeMode.Comic.resolveEffective(systemDark)
            val shouldApply = CrtFilterPolicy.shouldApplyCrt(
                isCrtSettingEnabled = true,
                effectiveThemeMode = effective,
                isSimpleModeEnabled = false
            )
            assertFalse("CRT filter must not apply when Comic mode is resolved", shouldApply)
        }
    }
}
