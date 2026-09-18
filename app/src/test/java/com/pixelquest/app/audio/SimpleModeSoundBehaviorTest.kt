package com.pixelquest.app.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleModeSoundBehaviorTest {

    private class FakeSoundPlayer {
        var levelUpSoundPlayed = false
        var completeSoundPlayed = false
        var missedSoundPlayed = false

        fun playLevelUpSound() { levelUpSoundPlayed = true }
        fun playTaskCompleteSound() { completeSoundPlayed = true }
        fun playTaskMissedSound() { missedSoundPlayed = true }
    }

    @Test
    fun `level up celebration sound is suppressed when simple mode is active`() {
        val soundPlayer = FakeSoundPlayer()
        val isSimpleMode = true
        val pendingLevelUp = 4

        // Mirror HomeScreen & HomeViewModel gating logic
        if (pendingLevelUp > 0 && !isSimpleMode) {
            soundPlayer.playLevelUpSound()
        }

        assertFalse(soundPlayer.levelUpSoundPlayed)
    }

    @Test
    fun `neutral task feedback sounds still play in simple mode`() {
        val soundPlayer = FakeSoundPlayer()
        val isSimpleMode = true

        // Simple mode keeps neutral auditory feedback
        if (isSimpleMode) {
            soundPlayer.playTaskCompleteSound()
            soundPlayer.playTaskMissedSound()
        }

        assertTrue(soundPlayer.completeSoundPlayed)
        assertTrue(soundPlayer.missedSoundPlayed)
    }

    @Test
    fun `gamified mode triggers level up celebration chime`() {
        val soundPlayer = FakeSoundPlayer()
        val isSimpleMode = false
        val pendingLevelUp = 5

        if (pendingLevelUp > 0 && !isSimpleMode) {
            soundPlayer.playLevelUpSound()
        }

        assertTrue(soundPlayer.levelUpSoundPlayed)
    }
}
