package com.pixelquest.app.chess

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.chess.data.ChessRepositoryImpl
import com.pixelquest.app.chess.data.remote.HeadToHeadDto
import com.pixelquest.app.chess.data.remote.RatingDto
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameEvent
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.sync.ChessLiveSource
import com.pixelquest.app.chess.sync.ChessSignal
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.testing.FakeChessRemote
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The chess repository (J131): the server decides, moves are never queued, a drop reconnects. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChessRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val remote = FakeChessRemote(me = "white")
    private var drops = 0
    private val live = ChessLiveSource { _, _ ->
        flow {
            emit(ChessSignal.Subscribed)
            emit(ChessSignal.Watching(2))
            // The first connection drops; the second stays.
            if (drops++ == 0) throw java.io.IOException("dropped")
            kotlinx.coroutines.awaitCancellation()
        }
    }
    private val repository = ChessRepositoryImpl(remote, db.chessDao(), { remote.me }, live, retryDelayMs = 1)

    @After
    fun tearDown() = db.closeAfterWrites()

    @Test
    fun aMove_goesToTheServer_orIsAnError_neverQueued(): Unit = runBlocking {
        val game = remote.seedGame()
        assertEquals(1, (repository.makeMove(game, 1, "e2e4") as SupabaseResult.Success).data.ply)
        remote.offline = true
        assertTrue(repository.makeMove(game, 3, "g1f3") is SupabaseResult.NetworkError)
        remote.offline = false
        assertEquals("nothing was kept to send later", 1, remote.moves[game]!!.size)
        val refused = repository.makeMove(game, 2, "e7e5") as SupabaseResult.ServerError
        assertEquals("not_your_turn", refused.message)
    }

    @Test
    fun following_readsTheGame_countsWatchers_andReconnects(): Unit = runBlocking {
        val game = remote.seedGame()
        val events = repository.follow(game).take(5).toList()
        assertTrue(events[0] is GameEvent.Updated)
        assertEquals(GameEvent.Watchers(2), events[1])
        assertEquals(GameEvent.Disconnected, events[2])
        assertTrue("read again after the drop", events[3] is GameEvent.Updated)
    }

    @Test
    fun challenges_ratings_andRecords(): Unit = runBlocking {
        val made = (repository.challenge("party", "black", TimeControl.FIVE, ColourChoice.WHITE, rated = true) as SupabaseResult.Success).data
        assertEquals(listOf(made.id), repository.observeChallenges("party").first().map { it.id })
        remote.me = "black"
        val game = (repository.respond(made.id, accept = true) as SupabaseResult.Success).data!!
        assertEquals("white", game.whiteId)
        assertEquals(GameStatus.WAITING, game.status)
        assertTrue(repository.observeChallenges("party").first().isEmpty())

        remote.ratings += RatingDto("black", 300, 820, games = 1, wins = 1, peak = 820)
        repository.refreshRatings("black")
        assertEquals(listOf(820), repository.observeRatings("black").first().map { it.points })
        remote.headToHead += HeadToHeadDto("black", "white", 300, 1, 0, 0)
        assertEquals(1, (repository.headToHead("white") as SupabaseResult.Success).data.single().myWins)
        remote.offline = true
        assertEquals("offline: what the phone last knew", 1, (repository.headToHead("white") as SupabaseResult.Success).data.single().myWins)
    }

    @Test
    fun aFinishedGame_isKept_aLiveOneNever(): Unit = runBlocking {
        db.daroodDao().upsertParties(listOf(com.pixelquest.app.party.data.local.PartyEntity("party", "Physics", "white", 10, null, null, null, 0L, 0L, 0, 2, null, com.pixelquest.app.party.domain.PartyRole.LEADER, com.pixelquest.app.party.domain.MemberStatus.ACTIVE, 0L, partyType = com.pixelquest.app.party.domain.PartyType.STUDY)))
        val game = remote.seedGame()
        repository.fetchGame(game)
        assertTrue(db.chessDao().observeFinishedGames("party", 10).first().isEmpty())
        repository.resign(game)
        assertEquals(listOf(game), db.chessDao().observeFinishedGames("party", 10).first().map { it.id })
    }
}
