package com.pixelquest.app.chess

import com.pixelquest.app.chess.domain.ChessClock
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ClockSync
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.TimeControl
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** The clocks shown from the server's numbers, with the phone's clock off by minutes (J132). */
class ChessClockTest {
    private val moved = Instant.parse("2026-10-22T10:00:00Z")
    private val game = ChessGame(
        "g", "p", "w", "b", TimeControl.THREE, true, GameStatus.ACTIVE, null, null, "fen", ChessColour.BLACK,
        whiteMs = 170_000, blackMs = 175_000, ply = 3, lastMoveAt = moved, createdAt = moved, startedAt = moved, endedAt = null
    )

    @Test
    fun theSideToMove_countsDown_theOtherWaits() {
        val sync = ClockSync().apply { sample(moved, moved.toEpochMilli()) }
        val later = moved.toEpochMilli() + 5_000
        assertEquals(170_000, ChessClock.remaining(game, ChessColour.WHITE, later, sync))
        assertEquals(170_000, ChessClock.remaining(game, ChessColour.BLACK, later, sync))
        assertEquals("no lower than 0", 0, ChessClock.remaining(game, ChessColour.BLACK, later + 999_999, sync))
    }

    @Test
    fun aPhoneThreeMinutesSlow_showsTheRightTime() {
        val slow = 180_000L
        val sync = ClockSync()
        // Updates arrive 300 ms and 80 ms after the server stamped them: the less delayed wins.
        sync.sample(moved, moved.toEpochMilli() - slow + 300)
        sync.sample(moved.plusSeconds(10), moved.plusSeconds(10).toEpochMilli() - slow + 80)
        assertEquals(slow - 80, sync.offsetMs)
        val phoneNow = moved.toEpochMilli() - slow + 2_000
        assertEquals("2 s gone, less the 80 ms the update took", 175_000 - 1_920L, ChessClock.remaining(game, ChessColour.BLACK, phoneNow, sync))
    }

    @Test
    fun firstMoves_andFinishedGames_dontRun() {
        val sync = ClockSync()
        assertEquals(175_000, ChessClock.remaining(game.copy(ply = 1), ChessColour.BLACK, moved.toEpochMilli() + 20_000, sync))
        assertEquals(175_000, ChessClock.remaining(game.copy(status = GameStatus.FINISHED), ChessColour.BLACK, moved.toEpochMilli() + 20_000, sync))
        assertEquals("2:55", ChessClock.format(175_000))
        assertEquals("0:09.4", ChessClock.format(9_450))
    }
}
