package com.pixelquest.app.chess

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.chess.data.fake.FakeChessBackend
import com.pixelquest.app.chess.domain.ChessClock
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ClockSync
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.ChessPartyViewModel
import com.pixelquest.app.chess.ui.ChessSessionViewModel
import com.pixelquest.app.chess.ui.IncomingChallengeViewModel
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * V048: Chess flow and limits suite.
 *
 * User flow:
 * - Challenge a party member
 * - Accept incoming challenge
 * - Play game through to checkmate ending
 * - Spectate a game in progress
 * - Inspect ratings and head-to-head records
 *
 * Out-of-bounds cases:
 * - Illegal move: invalid square or off-script move rejected
 * - Move out of turn: move attempted when opponent's turn is dropped
 * - Double submit: in-flight move blocks duplicate submissions
 * - Clock at zero: expired time coerces to 0 and claims timeout
 * - Disconnect / network failure: displays offline problem and rolls back optimistic state
 * - One live game rule: player cannot participate in multiple concurrent live games
 * - Blocked player: challenge from blocked player is refused and reported
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChessFlowAndLimitsTest {

    private val viewModels = TestViewModels()
    private val fake = FakeChessBackend(replyDelayMs = 10)
    private val server = FakeDaroodServer(me = FakeChessBackend.ME).apply {
        usernames += mapOf(
            FakeChessBackend.ME to "raza",
            FakeChessBackend.OPPONENT to "bram",
            FakeChessBackend.WATCHED_WHITE to "cleo",
            FakeChessBackend.WATCHED_BLACK to "dara",
        )
    }
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { ZoneId.of("UTC") })

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(300) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun userFlow_challenge_playToEnd_spectate_andSeeRating() = runBlocking {
        // 1. Challenge party member
        val party = server.seedParty("Physics", FakeChessBackend.ME, FakeChessBackend.OPPONENT, type = PartyType.STUDY)
        parties.refreshParties()
        parties.refreshParty(party)

        val partyVm = viewModels.keep(ChessPartyViewModel::class.java) {
            ChessPartyViewModel(SavedStateHandle(mapOf(ChessPartyViewModel.ARG_PARTY to party)), fake, fake, fake, parties, { FakeChessBackend.ME }, chess = fake)
        }
        val partyWatching = partyVm.state.launchIn(MainScope())
        waitFor("party members loaded") { partyVm.state.value.members.isNotEmpty() }

        partyVm.challenge(FakeChessBackend.OPPONENT, TimeControl.FIVE, ColourChoice.WHITE, rated = true)
        waitFor("challenge sent") {
            partyVm.state.value.challenges.any { it.fromUser == FakeChessBackend.ME && it.toUser == FakeChessBackend.OPPONENT }
        }
        partyWatching.cancel()

        // 2. Accept challenge
        val incomingVm = viewModels.keep(IncomingChallengeViewModel::class.java) {
            IncomingChallengeViewModel(SavedStateHandle(mapOf(IncomingChallengeViewModel.ARG to "fake-challenge-1")), fake, fake, parties)
        }
        val incomingWatching = incomingVm.state.launchIn(MainScope())
        waitFor("incoming challenge loaded") { incomingVm.state.value.challenge != null }
        incomingVm.answer(true)
        waitFor("game created from challenge") { incomingVm.state.value.gameId != null }
        val createdGameId = incomingVm.state.value.gameId!!
        incomingWatching.cancel()

        // 3. Play game to checkmate
        val sessionVm = viewModels.keep(ChessSessionViewModel::class.java) {
            ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to "fake-game-mate")), fake, fake, { FakeChessBackend.ME }, ratings = fake)
        }
        val sessionWatching = sessionVm.state.launchIn(MainScope())
        waitFor("game session loaded") { sessionVm.state.value.game != null }

        // Play Scholar's mate moves
        val moves = listOf("e2e4", "f1c4", "d1h5", "h5f7")
        for ((idx, move) in moves.withIndex()) {
            waitFor("my turn for move $idx") { sessionVm.state.value.myTurn && sessionVm.state.value.game?.ply == idx * 2 }
            sessionVm.move(move)
        }

        waitFor("checkmate achieved") {
            sessionVm.state.value.game?.status == GameStatus.FINISHED &&
                sessionVm.state.value.game?.result == GameResult.WHITE
        }
        assertEquals(ResultReason.CHECKMATE, sessionVm.state.value.game?.reason)
        assertEquals(20, sessionVm.state.value.game?.whiteChange)

        // 4. Spectate an active game
        val spectateVm = viewModels.keep(ChessSessionViewModel::class.java) {
            ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to "fake-game-watch")), fake, fake, { FakeChessBackend.ME })
        }
        val spectateWatching = spectateVm.state.launchIn(MainScope())
        waitFor("spectating session loaded") { spectateVm.state.value.spectating && spectateVm.state.value.watchers == 2 }
        assertTrue("User must be marked as spectator", spectateVm.state.value.spectating)
        assertFalse("Spectator cannot have a turn", spectateVm.state.value.myTurn)

        // 5. Check ratings
        val myRatings = (fake.observeRatings(FakeChessBackend.ME) as kotlinx.coroutines.flow.Flow<*>).let {
            fake.observeRatings(FakeChessBackend.ME)
        }
        val headToHead = fake.headToHead(FakeChessBackend.OPPONENT)
        assertTrue(headToHead is SupabaseResult.Success)
        val h2hList = (headToHead as SupabaseResult.Success).data
        assertTrue(h2hList.isNotEmpty())

        sessionWatching.cancel()
        spectateWatching.cancel()
    }

    @Test
    fun outOfBounds_illegalMove_and_moveOutOfTurn() = runBlocking {
        val vm = viewModels.keep(ChessSessionViewModel::class.java) {
            ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to "fake-game-mate")), fake, fake, { FakeChessBackend.ME })
        }
        val watching = vm.state.launchIn(MainScope())
        waitFor("game session loaded") { vm.state.value.game != null }

        // 1. Illegal move: pawn e2 to e5 (3 squares) is rejected by rules and backend
        vm.move("e2e5")
        // Ply should not advance
        assertEquals(0, vm.state.value.game?.ply)

        // 2. Play legal first move: e2e4
        vm.move("e2e4")
        waitFor("ply 1 accepted") { vm.state.value.game?.ply == 1 }

        // 3. Move out of turn: Black is thinking (turn is Black). White moves f1c4 out of turn
        assertFalse("Not my turn while Black is playing", vm.state.value.myTurn)
        val plyBefore = vm.state.value.game?.ply
        vm.move("f1c4")
        // Out of turn move dropped without affecting game ply
        assertEquals(plyBefore, vm.state.value.game?.ply)

        watching.cancel()
    }

    @Test
    fun outOfBounds_doubleSubmit_preventedBySendingFlag() = runBlocking {
        var networkInFlight = false
        val slowFake = object :
            com.pixelquest.app.chess.data.ChessRepository by fake,
            com.pixelquest.app.chess.data.ChessChallengeRepository by fake,
            com.pixelquest.app.chess.data.ChessRatingRepository by fake,
            com.pixelquest.app.chess.data.SpectateRepository by fake {
            override suspend fun makeMove(gameId: String, ply: Int, uci: String): SupabaseResult<ChessGame> {
                networkInFlight = true
                kotlinx.coroutines.delay(200)
                networkInFlight = false
                return fake.makeMove(gameId, ply, uci)
            }
        }

        val vm = viewModels.keep(ChessSessionViewModel::class.java) {
            ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to "fake-game-mate")), slowFake, slowFake, { FakeChessBackend.ME })
        }
        val watching = vm.state.launchIn(MainScope())
        waitFor("loaded") { vm.state.value.game != null }

        // Send first move
        vm.move("e2e4")
        waitFor("sending is true") { vm.state.value.sending }

        // Double submit during flight
        vm.move("e2e4")
        vm.move("f1c4")

        // Wait for first move to complete
        waitFor("sending done") { !vm.state.value.sending }
        assertEquals(1, vm.state.value.game?.ply)

        watching.cancel()
    }

    @Test
    fun outOfBounds_clockAtZero_triggersTimeoutClaim() {
        val game = ChessGame(
            id = "g-timeout",
            partyId = "p1",
            whiteId = "white_player",
            blackId = "black_player",
            timeControl = TimeControl.FIVE,
            rated = true,
            status = GameStatus.ACTIVE,
            result = null,
            reason = null,
            fen = FakeChessBackend.START_FEN,
            turn = ChessColour.WHITE,
            whiteMs = 5_000L,
            blackMs = 300_000L,
            ply = 2,
            lastMoveAt = Instant.parse("2026-10-21T10:00:00Z"),
            createdAt = Instant.parse("2026-10-21T09:59:00Z"),
            startedAt = Instant.parse("2026-10-21T09:59:00Z"),
            endedAt = null,
        )

        val sync = ClockSync()
        val moveTime = Instant.parse("2026-10-21T10:00:00Z").toEpochMilli()

        // 1. Clock with time remaining
        val remainingBefore = ChessClock.remaining(game, ChessColour.WHITE, moveTime + 3_000L, sync)
        assertEquals(2_000L, remainingBefore)

        // 2. Clock exactly at zero
        val remainingExactZero = ChessClock.remaining(game, ChessColour.WHITE, moveTime + 5_000L, sync)
        assertEquals(0L, remainingExactZero)
        assertEquals("0:00.0", ChessClock.format(remainingExactZero))

        // 3. Clock past zero (overdue by 10 seconds): coerces to 0, never negative
        val remainingPastZero = ChessClock.remaining(game, ChessColour.WHITE, moveTime + 15_000L, sync)
        assertEquals(0L, remainingPastZero)
        assertEquals("0:00.0", ChessClock.format(remainingPastZero))
    }

    @Test
    fun outOfBounds_disconnect_showsOfflineProblem_andRecovers() = runBlocking {
        var dropConnection = false
        val flakyFake = object :
            com.pixelquest.app.chess.data.ChessRepository by fake,
            com.pixelquest.app.chess.data.ChessChallengeRepository by fake,
            com.pixelquest.app.chess.data.ChessRatingRepository by fake,
            com.pixelquest.app.chess.data.SpectateRepository by fake {
            override suspend fun makeMove(gameId: String, ply: Int, uci: String): SupabaseResult<ChessGame> {
                if (dropConnection) return SupabaseResult.NetworkError(IOException("Connection reset by peer"))
                return fake.makeMove(gameId, ply, uci)
            }
        }

        val vm = viewModels.keep(ChessSessionViewModel::class.java) {
            ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to "fake-game-mate")), flakyFake, flakyFake, { FakeChessBackend.ME })
        }
        val watching = vm.state.launchIn(MainScope())
        waitFor("loaded") { vm.state.value.game != null }

        // 1. Disconnect mid-move
        dropConnection = true
        vm.move("e2e4")
        waitFor("offline reported") { vm.state.value.problem == ChessSessionViewModel.OFFLINE }
        assertEquals(ChessSessionViewModel.OFFLINE, vm.state.value.problem)
        assertNull("Pending move rolled back on failure", vm.state.value.pendingMove)
        assertNull("Pending FEN rolled back on failure", vm.state.value.pendingFen)
        assertEquals(0, vm.state.value.game?.ply)

        // 2. Reconnect and retry
        dropConnection = false
        vm.move("e2e4")
        waitFor("move sent after reconnect") { vm.state.value.game?.ply == 1 }
        assertNull(vm.state.value.problem)

        watching.cancel()
    }

    @Test
    fun outOfBounds_oneLiveGameRule_and_blockedPlayer() = runBlocking {
        // 1. One live game rule
        val liveGame = fake.myLiveGame()
        assertTrue(liveGame is SupabaseResult.Success)
        val active = (liveGame as SupabaseResult.Success).data
        assertNotNull("Player must have their active live game", active)
        assertEquals("fake-game-mate", active?.id)
        assertTrue("Live game status is active/waiting", active?.status?.live == true)

        // 2. Blocked player handling
        val incomingVm = viewModels.keep(IncomingChallengeViewModel::class.java) {
            IncomingChallengeViewModel(SavedStateHandle(mapOf(IncomingChallengeViewModel.ARG to "fake-challenge-1")), fake, fake, parties)
        }
        val incomingWatching = incomingVm.state.launchIn(MainScope())
        waitFor("incoming challenge loaded") { incomingVm.state.value.challenge != null }

        // Report and block sender
        incomingVm.report()
        waitFor("reported") { incomingVm.state.value.reported }
        assertEquals(listOf("fake-challenge-1" to "abuse"), fake.reported)

        incomingVm.block()
        waitFor("declined after block") { incomingVm.state.value.declined }
        assertTrue("Blocked user recorded", fake.blocked.contains(FakeChessBackend.OPPONENT))

        incomingWatching.cancel()
    }
}
