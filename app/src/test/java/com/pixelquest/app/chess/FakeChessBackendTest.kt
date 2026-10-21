package com.pixelquest.app.chess

import com.pixelquest.app.chess.data.fake.FakeChessBackend
import com.pixelquest.app.chess.domain.GameEvent
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The scripted backend the chess screens are built on (J134). */
class FakeChessBackendTest {
    @Test
    fun scholarsMate_playsToCheckmate_withTheOpponentAnswering(): Unit = runBlocking {
        val fake = FakeChessBackend(replyDelayMs = 1)
        val game = "fake-game-mate"
        val follower = launch { fake.follow(game).collect {} }
        for ((ply, uci) in listOf(1 to "e2e4", 3 to "f1c4", 5 to "d1h5", 7 to "h5f7")) {
            // Wait for the opponent's reply before the next scripted move.
            while ((fake.fetchGame(game) as SupabaseResult.Success).data.first.ply != ply - 1) kotlinx.coroutines.delay(2)
            assertTrue(fake.makeMove(game, ply, uci) is SupabaseResult.Success)
        }
        val (ended, moves) = (fake.fetchGame(game) as SupabaseResult.Success).data
        follower.cancel()
        assertEquals(GameStatus.FINISHED, ended.status)
        assertEquals(GameResult.WHITE to ResultReason.CHECKMATE, ended.result to ended.reason)
        assertEquals("Qxf7#", moves.last().san)
    }

    @Test
    fun offScript_isRefused_andAWatchedGameIsLive(): Unit = runBlocking {
        val fake = FakeChessBackend(replyDelayMs = 1)
        assertEquals("illegal_move", (fake.makeMove("fake-game-mate", 1, "a2a3") as SupabaseResult.ServerError).message)
        val watched = fake.watch("fake-game-watch").filterIsInstance<GameEvent.Updated>().first()
        assertEquals(4, watched.game.ply)
        assertEquals(2, (fake.liveGames(FakeChessBackend.PARTY) as SupabaseResult.Success).data.size)
        val accepted = (fake.respond("fake-challenge-1", accept = true) as SupabaseResult.Success).data!!
        assertEquals(GameStatus.WAITING, accepted.status)
    }
}
