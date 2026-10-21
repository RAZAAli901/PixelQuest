package com.pixelquest.app.chess

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.chess.data.ChessRepositoryImpl
import com.pixelquest.app.chess.sync.ChessLiveSource
import com.pixelquest.app.chess.sync.ChessSignal
import com.pixelquest.app.chess.ui.ChessSessionViewModel
import com.pixelquest.app.chess.ui.SessionPhase
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.testing.FakeChessRemote
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper
import java.time.Duration

/** The game screen's state machine (J133): waiting, active, finished, disconnected. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChessSessionViewModelTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val remote = FakeChessRemote(me = "white")
    private val signals = MutableSharedFlow<ChessSignal>(extraBufferCapacity = 8)
    private val live = ChessLiveSource { _, _ -> signals }
    private val repository = ChessRepositoryImpl(remote, db.chessDao(), { remote.me }, live, retryDelayMs = 10)
    private val viewModels = TestViewModels()

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(300) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
        throw AssertionError("still waiting: $what")
    }

    private fun session(game: String) = viewModels.keep(ChessSessionViewModel::class.java) {
        ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to game)), repository, repository, { "white" })
    }

    @Test
    fun waiting_thenActive_myMove_theirMove_thenResignFinishes() {
        val game = remote.seedGame(status = "waiting")
        val vm = session(game)
        waitFor("loaded") { vm.state.value.phase == SessionPhase.WAITING }
        assertTrue(vm.state.value.myTurn)
        vm.move("e2e4")
        waitFor("active") { vm.state.value.phase == SessionPhase.ACTIVE && vm.state.value.game!!.ply == 1 }
        assertFalse("Black's turn", vm.state.value.myTurn)
        vm.move("d2d4")
        assertEquals("not sent off-turn", 1, remote.moves[game]!!.size)

        // Black moves on the server; the live connection says so.
        runBlocking {
            remote.me = "black"
            remote.move(game, 2, "e7e5")
            remote.me = "white"
        }
        signals.tryEmit(ChessSignal.Changed)
        waitFor("their move") { vm.state.value.game!!.ply == 2 }
        assertTrue(vm.state.value.myTurn)

        vm.resign()
        waitFor("finished") { vm.state.value.phase == SessionPhase.FINISHED }
        assertEquals("black", remote.games[game]!!.result)
    }

    @Test
    fun aRefusal_isSaid_andADropShowsDisconnected() {
        val game = remote.seedGame()
        val vm = session(game)
        waitFor("loaded") { vm.state.value.phase == SessionPhase.ACTIVE }
        remote.offline = true
        vm.move("e2e4")
        waitFor("said") { vm.state.value.problem == ChessSessionViewModel.OFFLINE }
        assertFalse(vm.state.value.sending)
        remote.offline = false
        vm.offerDraw()
        waitFor("offered") { vm.state.value.iOfferedDraw }
    }

    @Test
    fun someoneNotPlaying_watches() {
        val game = remote.seedGame(white = "aria", black = "bram")
        val vm = session(game)
        waitFor("loaded") { vm.state.value.game != null }
        assertTrue(vm.state.value.spectating)
        assertFalse(vm.state.value.myTurn)
        signals.tryEmit(ChessSignal.Watching(3))
        waitFor("count") { vm.state.value.watchers == 3 }
        assertTrue("watching is logged first", "watch" in remote.calls)
    }

    @Test
    fun aMove_showsAtOnce_andIsTakenBackIfRefused() {
        val game = remote.seedGame()
        remote.games[game] = remote.games.getValue(game).copy(fen = com.pixelquest.app.chess.rules.Position.START)
        val vm = session(game)
        waitFor("loaded") { vm.state.value.phase == SessionPhase.ACTIVE }
        remote.offline = true
        vm.move("e2e4")
        // Before the answer the board already shows it... and once refused, it's taken back.
        waitFor("taken back") { vm.state.value.problem == ChessSessionViewModel.OFFLINE }
        assertEquals(com.pixelquest.app.chess.rules.Position.START, vm.state.value.boardFen)
        assertEquals(null, vm.state.value.pendingMove)
    }

    @Test
    fun aDrop_showsDisconnected_andTheGameResumes() {
        var drops = 0
        val flaky = ChessLiveSource { _, _ ->
            kotlinx.coroutines.flow.flow {
                emit(ChessSignal.Subscribed)
                if (drops++ == 0) {
                    kotlinx.coroutines.delay(50)
                    throw java.io.IOException("dropped")
                }
                kotlinx.coroutines.awaitCancellation()
            }
        }
        val repo = ChessRepositoryImpl(remote, db.chessDao(), { remote.me }, flaky, retryDelayMs = 300)
        val game = remote.seedGame()
        val seen = mutableListOf<SessionPhase>()
        val vm = viewModels.keep(ChessSessionViewModel::class.java) {
            ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to game)), repo, repo, { "white" })
        }
        waitFor("disconnected") { vm.state.value.phase.also { seen += it } == SessionPhase.DISCONNECTED }
        waitFor("back") { vm.state.value.phase == SessionPhase.ACTIVE }
    }
}
