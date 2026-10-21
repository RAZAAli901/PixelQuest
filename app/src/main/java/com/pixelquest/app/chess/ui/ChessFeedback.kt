package com.pixelquest.app.chess.ui

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.ui.haptics.PixelHaptics

/** What a move or an ending sounds and feels like (J161). */
enum class ChessCue { MOVE, CAPTURE, CHECK, END }

object ChessFeedback {
    /** A move's cue from its notation: mate and check over capture, capture over a plain move. */
    fun cueOf(move: ChessMove): ChessCue = when {
        move.san.endsWith("#") -> ChessCue.END
        move.san.endsWith("+") -> ChessCue.CHECK
        'x' in move.san -> ChessCue.CAPTURE
        else -> ChessCue.MOVE
    }

    /**
     * Plays [cue] with the system's own click (no sounds were supplied with the design) and the
     * app's haptics, unless the player turned chess sounds off ([on]) or the app's haptics are off.
     */
    fun play(cue: ChessCue, view: View?, haptic: HapticFeedback?, on: Boolean) {
        if (!on) return
        view?.playSoundEffect(if (cue == ChessCue.MOVE) SoundEffectConstants.CLICK else SoundEffectConstants.NAVIGATION_DOWN)
        when (cue) {
            ChessCue.MOVE -> PixelHaptics.performLightTap(haptic)
            ChessCue.CAPTURE -> PixelHaptics.performMediumConfirm(haptic)
            ChessCue.CHECK -> PixelHaptics.performWarning(haptic)
            ChessCue.END -> PixelHaptics.performSuccessPattern(haptic)
        }
    }
}
