package com.pixelquest.app.chess

import com.pixelquest.app.chess.data.remote.GameAnswerDto
import com.pixelquest.app.chess.data.remote.HeadToHeadDto
import com.pixelquest.app.chess.data.remote.RatingDto
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.TimeControl
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The server's chess rows, as the app reads them (J129). */
class ChessDtosTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aMoveAnswer_withItsGame() {
        val answer = json.decodeFromString<GameAnswerDto>(
            """{"game":{"id":"g1","party_id":"p1","white_id":"w","black_id":"b","time_control_sec":180,"rated":true,"status":"finished",
            "result":"white","result_reason":"checkmate","fen":"8/8","turn":"b","ply":7,"white_ms":170000,"black_ms":171500,
            "last_move_at":"2026-10-22T10:00:00+00:00","draw_offered_by":null,"created_at":"2026-10-22T09:55:00+00:00",
            "started_at":"2026-10-22T09:56:00+00:00","ended_at":"2026-10-22T10:00:00+00:00","white_change":20,"black_change":-20,
            "rating_applied":true,"something_new":1},
            "move":{"game_id":"g1","ply":7,"san":"Qxf7#","uci":"h5f7","fen_after":"8/8","white_ms_after":170000,"black_ms_after":171500,"created_at":"2026-10-22T10:00:00+00:00"}}"""
        )
        val game = answer.game!!.toDomain()!!
        assertEquals(TimeControl.THREE, game.timeControl)
        assertEquals(GameStatus.FINISHED, game.status)
        assertEquals(GameResult.WHITE, game.result)
        assertEquals(ResultReason.CHECKMATE, game.reason)
        assertEquals(ChessColour.BLACK, game.turn)
        assertEquals(20, game.whiteChange)
        assertEquals("Qxf7#", answer.move!!.toDomain().san)
    }

    @Test
    fun anUnknownTimeControl_isLeftOut_andTheRecordIsFromMySide() {
        val odd = RatingDto("u", timeControlSec = 120, intPoints = 900)
        assertNull(odd.toDomain())
        val pair = HeadToHeadDto("a", "b", 300, lowWins = 3, highWins = 1, draws = 2)
        assertEquals(1, pair.toDomain("b")!!.myWins)
        assertEquals(3, pair.toDomain("b")!!.theirWins)
        assertEquals("a", pair.toDomain("b")!!.them)
        assertNull(pair.toDomain("c"))
        assertNull(HeadToHeadDto("a", "b", null, 1, 1, 1).toDomain("a")!!.timeControl)
    }
}
