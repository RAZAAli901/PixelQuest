package com.pixelquest.app.chess

import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.chess.ui.ChessCue
import com.pixelquest.app.chess.ui.ChessFeedback
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Each move's cue (J161). */
class ChessFeedbackTest {
    private fun move(san: String) = ChessMove("g", 1, san, "a2a3", "fen", 1, 1, Instant.EPOCH)

    @Test
    fun mateCheckCaptureMove() {
        assertEquals(ChessCue.END, ChessFeedback.cueOf(move("Qxf7#")))
        assertEquals(ChessCue.CHECK, ChessFeedback.cueOf(move("Bxf7+")))
        assertEquals(ChessCue.CAPTURE, ChessFeedback.cueOf(move("exd5")))
        assertEquals(ChessCue.MOVE, ChessFeedback.cueOf(move("Nf3")))
        // Turned off: nothing plays, and nothing breaks without a view.
        ChessFeedback.play(ChessCue.END, null, null, on = false)
        ChessFeedback.play(ChessCue.MOVE, null, null, on = true)
    }
}
