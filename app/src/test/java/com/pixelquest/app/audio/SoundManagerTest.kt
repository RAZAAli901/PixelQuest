package com.pixelquest.app.audio

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

// SoundManager builds a SoundPool with AudioAttributes, which only Robolectric implements on the JVM.
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
class SoundManagerTest {

    private lateinit var mockContext: Context
    private lateinit var soundManager: SoundManager

    @Before
    fun setUp() {
        mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext()
        soundManager = SoundManager(mockContext)
    }

    @Test
    fun defaultSoundState_isEnabled() {
        assertTrue(soundManager.isSoundEnabled)
    }

    @Test
    fun soundState_canBeDisabledAndReenabled() {
        soundManager.isSoundEnabled = false
        assertFalse(soundManager.isSoundEnabled)

        soundManager.isSoundEnabled = true
        assertTrue(soundManager.isSoundEnabled)
    }
}
