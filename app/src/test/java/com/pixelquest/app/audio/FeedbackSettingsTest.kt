package com.pixelquest.app.audio

import android.app.Application
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.ui.haptics.PixelHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SFX: OFF used to silence nothing (the setting never reached SoundManager), and HAPTICS: OFF only
 * stopped the light tap: completing or skipping a quest and confirm dialogs still vibrated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FeedbackSettingsTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
        PixelHaptics.isHapticsEnabledGlobal = true
    }

    private class RecordingHaptics : HapticFeedback {
        var count = 0
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { count++ }
    }

    @Test
    fun theSettings_reachSoundAndHaptics_andFollowChanges() {
        val settings = FakeSettingsRepository()
        val sound = SoundManager(ApplicationProvider.getApplicationContext())

        FeedbackSettingsSync.start(scope, settings, sound)
        settings.isSoundEnabled.value = false
        settings.isHapticsEnabled.value = false

        assertFalse(sound.isSoundEnabled)
        assertFalse(PixelHaptics.isHapticsEnabledGlobal)

        settings.isSoundEnabled.value = true
        assertTrue(sound.isSoundEnabled)
    }

    @Test
    fun withHapticsOff_noPatternVibrates() {
        val haptics = RecordingHaptics()
        PixelHaptics.isHapticsEnabledGlobal = false

        PixelHaptics.performLightTap(haptics)
        PixelHaptics.performMediumConfirm(haptics)
        PixelHaptics.performSuccessPattern(haptics)
        PixelHaptics.performWarning(haptics)

        assertEquals(0, haptics.count)
    }

    @Test
    fun withHapticsOn_everyPatternVibrates_unlessTheCallerSaysNot() {
        val haptics = RecordingHaptics()
        PixelHaptics.isHapticsEnabledGlobal = true

        PixelHaptics.performLightTap(haptics)
        PixelHaptics.performSuccessPattern(haptics)
        PixelHaptics.performWarning(haptics, enabled = false)

        assertEquals(2, haptics.count)
    }
}
