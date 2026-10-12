package com.pixelquest.app.data.repository

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Settings saved by a released version survive the update. They live in SharedPreferences under
 * fixed names, so renaming one would quietly put that setting back to its default for every player.
 * These are the names v1.0.0 to v1.1.0 used (unchanged since), with values that differ from the defaults.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReleasedSettingsKeysTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun settingsWrittenByV1_1_0_areReadByThisVersion() = runBlocking {
        context.getSharedPreferences("pixelquest_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("key_sound_enabled", false)
            .putBoolean("key_crt_enabled", true)
            .putBoolean("key_haptics_enabled", false)
            .putBoolean("key_reduce_motion_enabled", true)
            .putBoolean("key_onboarding_complete", true)
            .putBoolean("key_notifications_enabled", false)
            .putBoolean("key_notification_sound", false)
            .putBoolean("key_notification_vibration", false)
            .commit()

        val settings = SettingsRepositoryImpl(context)

        assertEquals(false, settings.isSoundEnabled.first())
        assertEquals(true, settings.isCrtEnabled.first())
        assertEquals(false, settings.isHapticsEnabled.first())
        assertEquals(true, settings.isReduceMotionEnabled.first())
        assertEquals("not shown onboarding again", true, settings.onboardingComplete.first())
        assertEquals(false, settings.isNotificationsEnabled.first())
        assertEquals(false, settings.isNotificationSoundEnabled.first())
        assertEquals(false, settings.isNotificationVibrationEnabled.first())
    }
}
