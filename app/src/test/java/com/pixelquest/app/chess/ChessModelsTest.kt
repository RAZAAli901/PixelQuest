package com.pixelquest.app.chess

import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChallengeStatus
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.Outcome
import com.pixelquest.app.chess.domain.Tier
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.domain.isAnswerable
import com.pixelquest.app.chess.domain.isFor
import com.pixelquest.app.chess.domain.isTurnOf
import com.pixelquest.app.chess.domain.outcomeFor
import com.pixelquest.app.chess.domain.startingRating
import com.pixelquest.app.chess.domain.tier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ChessModelsTest {
    private val t0 = Instant.parse("2026-10-22T10:00:00Z")
    private val game = ChessGame(
        "g", "p", "white", "black", TimeControl.THREE, rated = true, status = GameStatus.ACTIVE, result = null, reason = null,
        fen = "start", turn = ChessColour.BLACK, whiteMs = 1, blackMs = 2, ply = 1, lastMoveAt = t0, createdAt = t0, startedAt = t0, endedAt = null,
    )

    @Test
    fun turnsAndOutcomes() {
        assertTrue(game.isTurnOf("black"))
        assertFalse(game.isTurnOf("white"))
        assertFalse(game.isTurnOf("watcher"))
        val won = game.copy(status = GameStatus.FINISHED, result = GameResult.WHITE)
        assertFalse(won.isTurnOf("black"))
        assertEquals(Outcome.WIN, won.outcomeFor("white"))
        assertEquals(Outcome.LOSS, won.outcomeFor("black"))
        assertEquals(Outcome.DRAW, won.copy(result = GameResult.DRAW).outcomeFor("black"))
        assertNull(won.outcomeFor("watcher"))
        assertNull(game.copy(status = GameStatus.ABORTED).outcomeFor("white"))
    }

    @Test
    fun challenges_whoMayTakeThem_andUntilWhen() {
        val direct = Challenge("c", "p", "aria", "bram", TimeControl.ONE, ColourChoice.RANDOM, true, ChallengeStatus.OPEN, t0.plusSeconds(600), t0)
        assertTrue(direct.isFor("bram"))
        assertFalse(direct.isFor("cleo"))
        val open = direct.copy(toUser = null)
        assertTrue(open.isFor("cleo"))
        assertFalse(open.isFor("aria"), "not their own")
        assertTrue(direct.isAnswerable(t0.plusSeconds(599)))
        assertFalse(direct.isAnswerable(t0.plusSeconds(600)))
        assertFalse(direct.copy(status = ChallengeStatus.DECLINED).isAnswerable(t0))
    }

    @Test
    fun aNewPlayer_startsAt200_asAPawn() {
        val start = startingRating("aria", TimeControl.FIVE)
        assertEquals(200, start.points)
        assertEquals(Tier.PAWN, start.tier)
        assertEquals(Tier.QUEEN, start.copy(points = 1650).tier)
        assertEquals(TimeControl.TEN, TimeControl.of(600))
        assertNull(TimeControl.of(120))
    }

    private fun assertFalse(value: Boolean, message: String) = org.junit.Assert.assertFalse(message, value)
}
