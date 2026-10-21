package com.pixelquest.app.chess.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.chess.data.ChessRepository
import com.pixelquest.app.chess.data.SpectateRepository
import com.pixelquest.app.chess.domain.ChessClock
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.chess.domain.ClockSync
import com.pixelquest.app.chess.domain.GameEvent
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.isTurnOf
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.data.remote.SupabaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where a game screen is: the states the UI draws (docs/chess-design.md, Game states). */
enum class SessionPhase { LOADING, WAITING, ACTIVE, FINISHED, DISCONNECTED }

data class ChessSessionState(
    val phase: SessionPhase = SessionPhase.LOADING,
    val game: ChessGame? = null,
    val moves: List<ChessMove> = emptyList(),
    val me: String? = null,
    /** Null for someone watching. */
    val myColour: ChessColour? = null,
    val watchers: Int = 0,
    /** A move, a resignation or an offer on its way: the board waits. */
    val sending: Boolean = false,
    /** The server's last refusal, if the screen should say it. */
    val problem: String? = null,
    val notFound: Boolean = false,
    /** The player's move, shown on the board at once, until the server takes it or says no (J144). */
    val pendingFen: String? = null,
    val pendingMove: String? = null
) {
    /** What the board shows: the player's move straight away, then the server's position. */
    val boardFen: String? get() = pendingFen ?: game?.fen

    /** The last move to highlight: the one on its way, or the last the server took. */
    val lastMove: String? get() = pendingMove ?: moves.lastOrNull()?.uci

    val myTurn: Boolean get() = game?.isTurnOf(me) == true && !sending
    val spectating: Boolean get() = game != null && myColour == null
    /** The other player has offered a draw. */
    val drawOffered: Boolean get() = game?.drawOfferedBy?.let { it != me && myColour != null } == true
    val iOfferedDraw: Boolean get() = game?.drawOfferedBy == me && me != null
}

/** One-off things for the screen: a sound, a toast, the end card. */
sealed interface ChessSessionEvent {
    data class Moved(val move: ChessMove, val mine: Boolean) : ChessSessionEvent
    data class Refused(val token: String) : ChessSessionEvent
    data class Ended(val game: ChessGame) : ChessSessionEvent
    data object DrawOffered : ChessSessionEvent
}

/**
 * A chess game on screen (J133): the server's game, live, as a small state machine — loading,
 * waiting for White's first move, active, finished (or aborted), and disconnected while the live
 * connection is coming back. Sends the player's moves and choices; never decides a move, a clock
 * or a result. When the other side's clock reaches zero on the phone, it asks the server once.
 */
@HiltViewModel
class ChessSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chess: ChessRepository,
    private val spectate: SpectateRepository,
    account: DaroodAccount
) : ViewModel() {
    val gameId: String = savedStateHandle[ARG_GAME] ?: ""
    private val me: String? = account.id()

    private val _state = MutableStateFlow(ChessSessionState(me = me))
    val state: StateFlow<ChessSessionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChessSessionEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<ChessSessionEvent> = _events

    /** The phone's clock against the server's, for the clocks the screen shows. */
    val clockSync = ClockSync()
    var phoneClock: () -> Long = System::currentTimeMillis

    private var following: Job? = null
    private var claimedAtPly = -1

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        when (val read = chess.fetchGame(gameId)) {
            is SupabaseResult.Success -> {
                val (game, moves) = read.data
                apply(game, moves)
                follow(spectator = game.colourOf(me) == null)
            }
            is SupabaseResult.ServerError -> _state.update { it.copy(notFound = true, phase = SessionPhase.FINISHED) }
            else -> {
                _state.update { it.copy(phase = SessionPhase.DISCONNECTED) }
                delay(RETRY_MS)
                load()
            }
        }
    }

    private fun follow(spectator: Boolean) {
        following?.cancel()
        following = viewModelScope.launch {
            val stream = if (spectator) spectate.watch(gameId) else chess.follow(gameId)
            stream.collect { event ->
                when (event) {
                    is GameEvent.Updated -> {
                        val moves = event.move?.let { move -> (_state.value.moves.filter { it.ply < move.ply } + move) } ?: _state.value.moves
                        apply(event.game, if (event.game.ply == moves.size) moves else refreshMoves(moves, event.game))
                        event.move?.let { _events.tryEmit(ChessSessionEvent.Moved(it, mine = event.game.colourOf(me) == colourOfPly(it.ply))) }
                    }
                    is GameEvent.Watchers -> _state.update { it.copy(watchers = event.count) }
                    GameEvent.Disconnected -> _state.update { it.copy(phase = SessionPhase.DISCONNECTED) }
                }
            }
        }
        viewModelScope.launch { watchTheFlag() }
    }

    private suspend fun refreshMoves(known: List<ChessMove>, game: ChessGame): List<ChessMove> =
        (chess.fetchGame(game.id) as? SupabaseResult.Success)?.data?.second ?: known

    private fun colourOfPly(ply: Int) = if (ply % 2 == 1) ChessColour.WHITE else ChessColour.BLACK

    /** The server's game, as it now stands. */
    internal fun apply(game: ChessGame, moves: List<ChessMove>) {
        val before = _state.value.game
        game.lastMoveAt?.let { if (game.ply != before?.ply) clockSync.sample(it, phoneClock()) }
        val phase = when (game.status) {
            GameStatus.WAITING -> SessionPhase.WAITING
            GameStatus.ACTIVE -> SessionPhase.ACTIVE
            GameStatus.FINISHED, GameStatus.ABORTED -> SessionPhase.FINISHED
        }
        _state.update { it.copy(phase = phase, game = game, moves = moves, myColour = game.colourOf(me), sending = false, pendingFen = null, pendingMove = null) }
        if (phase == SessionPhase.FINISHED && before?.status?.live != false) _events.tryEmit(ChessSessionEvent.Ended(game))
        if (game.drawOfferedBy != null && game.drawOfferedBy != me && before?.drawOfferedBy != game.drawOfferedBy && game.colourOf(me) != null) {
            _events.tryEmit(ChessSessionEvent.DrawOffered)
        }
    }

    /** The player's move ("e2e4", "e7e8q"): sent as the next ply; the board moves when the server says. */
    fun move(uci: String) {
        val current = _state.value
        val game = current.game ?: return
        if (!current.myTurn) return
        // Shown at once; the server's answer replaces it, or takes it back (send's refusal path).
        val after = com.pixelquest.app.chess.rules.Position.of(game.fen)?.let { position ->
            com.pixelquest.app.chess.rules.Move.ofUci(uci)?.takeIf { it in position.legalMoves() }?.let { position.apply(it).fen() }
        }
        _state.update { it.copy(pendingFen = after, pendingMove = after?.let { uci }) }
        send { chess.makeMove(game.id, game.ply + 1, uci) }
    }

    fun resign() = _state.value.game?.let { game -> send { chess.resign(game.id) } }
    fun offerDraw() = _state.value.game?.let { game -> send { chess.offerDraw(game.id) } }
    fun answerDraw(accept: Boolean) = _state.value.game?.let { game -> send { chess.answerDraw(game.id, accept) } }

    private fun send(call: suspend () -> SupabaseResult<ChessGame>) {
        if (_state.value.sending) return
        _state.update { it.copy(sending = true, problem = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is SupabaseResult.Success -> apply(result.data, (chess.fetchGame(result.data.id) as? SupabaseResult.Success)?.data?.second ?: _state.value.moves)
                is SupabaseResult.ServerError -> {
                    _state.update { it.copy(sending = false, problem = result.message, pendingFen = null, pendingMove = null) }
                    _events.tryEmit(ChessSessionEvent.Refused(result.message))
                    // The game may have moved on (ended on time, aborted): read it again.
                    (chess.fetchGame(gameId) as? SupabaseResult.Success)?.data?.let { (g, m) -> apply(g, m) }
                }
                else -> {
                    _state.update { it.copy(sending = false, problem = OFFLINE, pendingFen = null, pendingMove = null) }
                    _events.tryEmit(ChessSessionEvent.Refused(OFFLINE))
                }
            }
        }
    }

    /** When the other side's clock shows zero, the server is asked to end the game on time, once per ply. */
    private suspend fun watchTheFlag() {
        while (true) {
            delay(TICK_MS)
            val s = _state.value
            val game = s.game ?: continue
            val mine = s.myColour ?: continue
            if (game.status != GameStatus.ACTIVE || game.turn == mine || claimedAtPly == game.ply) continue
            if (ChessClock.remaining(game, game.turn, phoneClock(), clockSync) > 0) continue
            claimedAtPly = game.ply
            when (val claimed = chess.claimTimeout(game.id)) {
                is SupabaseResult.Success -> apply(claimed.data, s.moves)
                else -> Unit
            }
        }
    }

    companion object {
        const val ARG_GAME = "gameId"
        const val OFFLINE = "offline"
        private const val RETRY_MS = 3_000L
        private const val TICK_MS = 500L
    }
}
