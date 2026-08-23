package com.pixelquest.app.audio

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SoundManagerTest {

    private lateinit var mockContext: Context
    private lateinit var soundManager: SoundManager

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
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
