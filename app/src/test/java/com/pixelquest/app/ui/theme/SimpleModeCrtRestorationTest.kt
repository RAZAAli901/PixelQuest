package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.model.CrtFilterPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 38: Verification that the CRT-forced-off decision correctly restores
 * the user's original CRT preference when Simple Mode is turned back off,
 * without requiring manual re-enabling.
 */
class SimpleModeCrtRestorationTest {

    @Test
    fun `crt preference is preserved and automatically restored when exiting simple mode`() {
        // User has CRT enabled in retro Pixel theme
        var userCrtSetting = true
        val theme = ThemeMode.Pixel

        // Initial state: Full Game Mode with CRT ON
        var isSimpleMode = false
        var shouldApplyCrt = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = userCrtSetting,
            effectiveThemeMode = theme,
            isSimpleModeEnabled = isSimpleMode
        )
        assertTrue("CRT must be active initially", shouldApplyCrt)

        // User enables Simple Mode
        isSimpleMode = true
        // The underlying preference must NOT be mutated or overwritten
        assertTrue("Underlying preference remains true in storage", userCrtSetting)
        shouldApplyCrt = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = userCrtSetting,
            effectiveThemeMode = theme,
            isSimpleModeEnabled = isSimpleMode
        )
        assertFalse("CRT must be strictly suppressed while Simple Mode is active", shouldApplyCrt)

        // User disables Simple Mode (switches back to Full Game Mode)
        isSimpleMode = false
        shouldApplyCrt = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = userCrtSetting,
            effectiveThemeMode = theme,
            isSimpleModeEnabled = isSimpleMode
        )
        assertTrue("CRT must be automatically restored without manual re-enabling", shouldApplyCrt)
    }

    @Test
    fun `crt disabled user remains disabled after exiting simple mode`() {
        val userCrtSetting = false
        val theme = ThemeMode.Pixel

        var isSimpleMode = true
        var shouldApply = CrtFilterPolicy.shouldApplyCrt(userCrtSetting, theme, isSimpleMode)
        assertFalse(shouldApply)

        isSimpleMode = false
        shouldApply = CrtFilterPolicy.shouldApplyCrt(userCrtSetting, theme, isSimpleMode)
        assertFalse("If user had CRT disabled originally, it must remain disabled", shouldApply)
    }
}
