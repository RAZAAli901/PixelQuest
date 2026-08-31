package com.pixelquest.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pixelquest.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSoundManager = staticCompositionLocalOf<SoundManager?> { null }

@Singleton
class SoundManager @Inject constructor(

    @ApplicationContext private val context: Context
) {
    var isSoundEnabled: Boolean = true

    private var soundPool: SoundPool? = null
    private var soundClickId: Int = 0
    private var soundCompleteId: Int = 0
    private var soundMissedId: Int = 0
    private var soundLevelUpId: Int = 0

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        soundPool?.let { pool ->
            soundClickId = pool.load(context, R.raw.sfx_click, 1)
            soundCompleteId = pool.load(context, R.raw.sfx_complete, 1)
            soundMissedId = pool.load(context, R.raw.sfx_missed, 1)
            soundLevelUpId = pool.load(context, R.raw.sfx_levelup, 1)
        }
    }

    fun playClickSound() {
        if (!isSoundEnabled) return
        if (soundClickId != 0) {
            soundPool?.play(soundClickId, 0.60f, 0.60f, 0, 0, 1.0f)
        }
    }

    fun playNavSound() {
        if (!isSoundEnabled) return
        if (soundClickId != 0) {
            soundPool?.play(soundClickId, 0.35f, 0.35f, 0, 0, 1.2f)
        }
    }

    fun playTaskCompleteSound() {
        if (!isSoundEnabled) return
        if (soundCompleteId != 0) {
            soundPool?.play(soundCompleteId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
    }

    fun playTaskMissedSound() {
        if (!isSoundEnabled) return
        if (soundMissedId != 0) {
            soundPool?.play(soundMissedId, 0.85f, 0.85f, 0, 0, 1.0f)
        }
    }

    fun playLevelUpSound() {
        if (!isSoundEnabled) return
        if (soundLevelUpId != 0) {
            soundPool?.play(soundLevelUpId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
    }

    fun playQuestBeginSound() {
        if (!isSoundEnabled) return
        if (soundLevelUpId != 0) {
            soundPool?.play(soundLevelUpId, 1.0f, 1.0f, 0, 0, 1.25f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
