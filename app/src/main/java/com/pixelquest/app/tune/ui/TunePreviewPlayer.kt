package com.pixelquest.app.tune.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.pixelquest.app.util.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TunePreviewPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val _playingTuneId = MutableStateFlow<String?>(null)
    val playingTuneId: StateFlow<String?> = _playingTuneId.asStateFlow()

    companion object {
        private const val TAG = "TunePreviewPlayer"
    }

    fun playPreview(tuneId: String, previewUrl: String) {
        stop()

        try {
            _playingTuneId.value = tuneId
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(previewUrl))
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    AppLog.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    stop()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to start preview playback", e)
            stop()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            AppLog.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _playingTuneId.value = null
        }
    }

    fun release() {
        stop()
    }
}
