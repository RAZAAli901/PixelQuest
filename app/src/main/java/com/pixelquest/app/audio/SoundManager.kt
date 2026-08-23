package com.pixelquest.app.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    var isSoundEnabled: Boolean = true

    fun playClickSound() {
        // SoundPool playback implementation in Step 4
    }

    fun playTaskCompleteSound() {
        // SoundPool playback implementation in Step 4
    }

    fun playTaskMissedSound() {
        // SoundPool playback implementation in Step 4
    }

    fun playLevelUpSound() {
        // SoundPool playback implementation in Step 4
    }

    fun release() {
        // Release resources
    }
}
