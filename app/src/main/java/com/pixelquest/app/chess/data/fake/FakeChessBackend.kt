package com.pixelquest.app.chess.data.fake

import com.pixelquest.app.chess.data.ChessChallengeRepository
import com.pixelquest.app.chess.data.ChessRatingRepository
import com.pixelquest.app.chess.data.ChessRepository
import com.pixelquest.app.chess.data.SpectateRepository
import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChallengeStatus
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameEvent
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

/**
 * Chess without a server (J134), for building and previewing the screens: scripted games whose
 * moves and positions are chess.js's own (supabase/functions/node_modules), and an opponent who
 * answers from the script. It referees nothing: a move off the script is refused as illegal.
 * Never wired into the real app.
 */
class FakeChessBackend(
    val me: String = ME,
    private val replyDelayMs: Long = 600,
    private val clock: () -> Instant = Instant::now
) : ChessRepository, ChessChallengeRepository, ChessRatingRepository, SpectateRepository {

    /** A scripted game: each ply's UCI, SAN and the FEN after it, and how it ends. */
    data class Script(val name: String, val plies: List<Triple<String, String, String>>, val ending: Pair<GameResult, ResultReason>?)

    private val games = linkedMapOf<String, MutableStateFlow<ChessGame>>()
    private val moves = mutableMapOf<String, MutableList<ChessMove>>()
    private val scripts = mutableMapOf<String, Script>()
    private val challenges = MutableStateFlow(
        listOf(
            Challenge("fake-challenge-1", PARTY, OPPONENT, ME, TimeControl.FIVE, ColourChoice.RANDOM, true, ChallengeStatus.OPEN, clock().plusSeconds(600), clock()),
            Challenge("fake-challenge-2", PARTY, WATCHED_WHITE, null, TimeControl.ONE, ColourChoice.WHITE, true, ChallengeStatus.OPEN, clock().plusSeconds(400), clock())
        )
    )

    init {
        start("fake-game-mate", SCHOLARS_MATE, white = ME, black = OPPONENT)
        start("fake-game-watch", QUEENS_GAMBIT, white = WATCHED_WHITE, black = WATCHED_BLACK, playedUpTo = 4)
    }

    /** Starts [script] as game [id]; the first [playedUpTo] plies already made. */
    fun start(id: String, script: Script, white: String, black: String, timeControl: TimeControl = TimeControl.FIVE, playedUpTo: Int = 0): ChessGame {
        scripts[id] = script
        val played = script.plies.take(playedUpTo).mapIndexed { i, (uci, san, fen) ->
            ChessMove(id, i + 1, san, uci, fen, timeControl.millis, timeControl.millis, clock())
        }
        moves[id] = played.toMutableList()
        val game = ChessGame(
            id, PARTY, white, black, timeControl, true,
            status = if (playedUpTo == 0) GameStatus.WAITING else GameStatus.ACTIVE, result = null, reason = null,
            fen = played.lastOrNull()?.fenAfter ?: START_FEN, turn = if (playedUpTo % 2 == 0) ChessColour.WHITE else ChessColour.BLACK,
            whiteMs = timeControl.millis, blackMs = timeControl.millis, ply = playedUpTo, lastMoveAt = clock(), createdAt = clock(), startedAt = clock(), endedAt = null
        )
        games[id] = MutableStateFlow(game)
        return game
    }

    private fun refuse(token: String) = SupabaseResult.ServerError(409, token)

    override suspend fun fetchGame(gameId: String): SupabaseResult<Pair<ChessGame, List<ChessMove>>> {
        val game = games[gameId]?.value ?: return SupabaseResult.ServerError(404, "not_found")
        return SupabaseResult.Success(game to moves[gameId].orEmpty().toList())
    }

    override suspend fun myLiveGame(): SupabaseResult<ChessGame?> =
        SupabaseResult.Success(games.values.map { it.value }.firstOrNull { it.status.live && it.colourOf(me) != null })

    override suspend fun makeMove(gameId: String, ply: Int, uci: String): SupabaseResult<ChessGame> {
        val state = games[gameId] ?: return refuse("not_a_player")
        val game = state.value
        if (!game.status.live) return refuse("game_over")
        if (ply != game.ply + 1) return refuse("wrong_ply")
        val script = scripts.getValue(gameId)
        if (script.plies.getOrNull(ply - 1)?.first != uci) return SupabaseResult.ServerError(422, "illegal_move")
        play(gameId, ply)
        return SupabaseResult.Success(state.value)
    }

    private fun play(gameId: String, ply: Int) {
        val state = games.getValue(gameId)
        val script = scripts.getValue(gameId)
        val (uci, san, fen) = script.plies[ply - 1]
        val game = state.value
        moves.getValue(gameId) += ChessMove(gameId, ply, san, uci, fen, game.whiteMs, game.blackMs, clock())
        val last = ply == script.plies.size
        val ending = script.ending.takeIf { last }
        state.value = game.copy(
            fen = fen, ply = ply, turn = if (ply % 2 == 0) ChessColour.WHITE else ChessColour.BLACK, lastMoveAt = clock(),
            status = if (ending != null) GameStatus.FINISHED else GameStatus.ACTIVE,
            result = ending?.first, reason = ending?.second, endedAt = if (ending != null) clock() else null,
            whiteChange = if (ending != null) 20 else null, blackChange = if (ending != null) -20 else null, ratingApplied = ending != null
        )
    }

    private fun end(gameId: String, result: GameResult?, reason: ResultReason): SupabaseResult<ChessGame> {
        val state = games[gameId] ?: return refuse("not_a_player")
        if (!state.value.status.live) return refuse("game_over")
        state.value = state.value.copy(
            status = if (reason == ResultReason.ABORTED) GameStatus.ABORTED else GameStatus.FINISHED,
            result = result, reason = reason, endedAt = clock(), drawOfferedBy = null
        )
        return SupabaseResult.Success(state.value)
    }

    override suspend fun resign(gameId: String): SupabaseResult<ChessGame> {
        val game = games[gameId]?.value ?: return refuse("not_a_player")
        return end(gameId, if (game.colourOf(me) == ChessColour.WHITE) GameResult.BLACK else GameResult.WHITE, ResultReason.RESIGN)
    }

    /** The scripted opponent always accepts a draw. */
    override suspend fun offerDraw(gameId: String) = end(gameId, GameResult.DRAW, ResultReason.DRAW_AGREED)

    override suspend fun answerDraw(gameId: String, accept: Boolean): SupabaseResult<ChessGame> {
        val state = games[gameId] ?: return refuse("not_a_player")
        if (accept) return end(gameId, GameResult.DRAW, ResultReason.DRAW_AGREED)
        state.value = state.value.copy(drawOfferedBy = null)
        return SupabaseResult.Success(state.value)
    }

    override suspend fun claimTimeout(gameId: String) = refuse("not_timed_out")

    /** Each change as it happens; after the player's move, the opponent answers from the script. */
    override fun follow(gameId: String): Flow<GameEvent> = flow {
        val state = games[gameId] ?: return@flow
        var seen = -1
        emit(GameEvent.Watchers(2))
        state.collect { game ->
            if (game.ply != seen || !game.status.live) {
                emit(GameEvent.Updated(game, moves[gameId]?.lastOrNull()?.takeIf { seen >= 0 && it.ply == game.ply }))
                seen = game.ply
            }
            val script = scripts.getValue(gameId)
            val opponentsTurn = game.status.live && game.colourOf(me) != null && game.turn != game.colourOf(me)
            if (opponentsTurn && game.ply < script.plies.size) {
                delay(replyDelayMs)
                play(gameId, game.ply + 1)
            }
        }
    }

    override suspend fun challenge(partyId: String, toUser: String?, timeControl: TimeControl, colour: ColourChoice, rated: Boolean): SupabaseResult<Challenge> {
        val made = Challenge("fake-challenge-${challenges.value.size + 1}", partyId, me, toUser, timeControl, colour, rated, ChallengeStatus.OPEN, clock().plusSeconds(600), clock())
        challenges.value = listOf(made) + challenges.value
        return SupabaseResult.Success(made)
    }

    override suspend fun respond(challengeId: String, accept: Boolean): SupabaseResult<ChessGame?> {
        val c = challenges.value.firstOrNull { it.id == challengeId } ?: return SupabaseResult.ServerError(404, "not_found")
        challenges.value = challenges.value.filter { it.id != challengeId }
        if (!accept) return SupabaseResult.Success(null)
        return SupabaseResult.Success(start("fake-game-${games.size + 1}", SCHOLARS_MATE, white = me, black = c.fromUser, timeControl = c.timeControl))
    }

    override suspend fun cancel(challengeId: String): SupabaseResult<Unit> {
        challenges.value = challenges.value.filter { it.id != challengeId }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun fetchChallenge(challengeId: String): SupabaseResult<Challenge?> =
        SupabaseResult.Success(challenges.value.firstOrNull { it.id == challengeId })

    override fun observeChallenges(partyId: String): Flow<List<Challenge>> = challenges
    override suspend fun refreshChallenges(partyId: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    /** Reports and blocks made, for tests. */
    val reported = mutableListOf<Pair<String, String>>()
    val blocked = mutableListOf<String>()

    override suspend fun report(challengeId: String, reason: String): SupabaseResult<Unit> {
        reported += challengeId to reason
        return SupabaseResult.Success(Unit)
    }

    override suspend fun block(userId: String): SupabaseResult<Unit> {
        blocked += userId
        return SupabaseResult.Success(Unit)
    }

    private val ratings = listOf(
        Rating(ME, TimeControl.FIVE, 1180, 14, 8, 2, 4, 1210),
        Rating(ME, TimeControl.ONE, 960, 6, 3, 0, 3, 990),
        Rating(OPPONENT, TimeControl.FIVE, 1320, 22, 13, 3, 6, 1340),
        Rating(WATCHED_WHITE, TimeControl.FIVE, 1540, 40, 26, 4, 10, 1600),
        Rating(WATCHED_BLACK, TimeControl.FIVE, 870, 3, 1, 0, 2, 880)
    )

    override fun observeRatings(userId: String): Flow<List<Rating>> = flowOf(ratings.filter { it.userId == userId })
    override suspend fun refreshRatings(userId: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    override suspend fun headToHead(other: String): SupabaseResult<List<HeadToHead>> =
        SupabaseResult.Success(listOf(HeadToHead(me, other, TimeControl.FIVE, 3, 2, 1), HeadToHead(me, other, null, 4, 3, 1)))
    override suspend fun leaderboard(partyId: String, timeControl: TimeControl?): SupabaseResult<List<Rating>> =
        SupabaseResult.Success(ratings.filter { timeControl == null || it.timeControl == timeControl }.sortedByDescending { it.points })

    override suspend fun liveGames(partyId: String): SupabaseResult<List<ChessGame>> =
        SupabaseResult.Success(games.values.map { it.value }.filter { it.status.live && it.partyId == partyId })

    override fun watch(gameId: String): Flow<GameEvent> = follow(gameId)

    companion object {
        const val ME = "fake-me"
        const val OPPONENT = "fake-bram"
        const val WATCHED_WHITE = "fake-cleo"
        const val WATCHED_BLACK = "fake-dara"
        const val PARTY = "fake-party"
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        /** 1.e4 e5 2.Bc4 Nc6 3.Qh5 Nf6 4.Qxf7#, as chess.js plays it. */
        val SCHOLARS_MATE = Script(
            "Scholar's mate",
            listOf(
                Triple("e2e4", "e4", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"),
                Triple("e7e5", "e5", "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"),
                Triple("f1c4", "Bc4", "rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR b KQkq - 1 2"),
                Triple("b8c6", "Nc6", "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR w KQkq - 2 3"),
                Triple("d1h5", "Qh5", "r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 3 3"),
                Triple("g8f6", "Nf6", "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"),
                Triple("h5f7", "Qxf7#", "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4")
            ),
            GameResult.WHITE to ResultReason.CHECKMATE
        )

        /** 1.d4 d5 2.c4 e6 3.Nc3 Nf6: a game to watch, still going. */
        val QUEENS_GAMBIT = Script(
            "Queen's Gambit Declined",
            listOf(
                Triple("d2d4", "d4", "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1"),
                Triple("d7d5", "d5", "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2"),
                Triple("c2c4", "c4", "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq - 0 2"),
                Triple("e7e6", "e6", "rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3"),
                Triple("b1c3", "Nc3", "rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR b KQkq - 1 3"),
                Triple("g8f6", "Nf6", "rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4")
            ),
            null
        )
    }
}
