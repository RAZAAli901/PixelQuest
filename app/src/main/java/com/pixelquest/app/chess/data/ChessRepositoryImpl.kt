package com.pixelquest.app.chess.data

import com.pixelquest.app.chess.data.local.ChessDao
import com.pixelquest.app.chess.data.local.ChessFinishedGameEntity
import com.pixelquest.app.chess.data.local.ChessHeadToHeadEntity
import com.pixelquest.app.chess.data.local.ChessRatingEntity
import com.pixelquest.app.chess.data.remote.ChessGameDto
import com.pixelquest.app.chess.data.remote.ChessRemote
import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameEvent
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.sync.ChessLiveSource
import com.pixelquest.app.chess.sync.ChessSignal
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chess for the screens (docs/chess-design.md): every change goes to the server, which referees;
 * what's shown is what it says. Moves are never queued offline: a move that can't be sent is an
 * error for the screen to show. Ratings, records and finished games are cached on the phone;
 * a live game never is.
 */
@Singleton
class ChessRepositoryImpl(
    private val remote: ChessRemote,
    private val dao: ChessDao,
    private val account: DaroodAccount,
    private val live: ChessLiveSource,
    private val retryDelayMs: Long = RETRY_DELAY_MS
) : ChessRepository, ChessChallengeRepository, ChessRatingRepository, SpectateRepository {

    @Inject
    constructor(remote: ChessRemote, dao: ChessDao, account: DaroodAccount, live: ChessLiveSource) :
        this(remote, dao, account, live, RETRY_DELAY_MS)

    private fun signedOut() = SupabaseResult.AuthError(IllegalStateException("signed out"), "Sign in to play chess.")

    // Playing

    override suspend fun fetchGame(gameId: String): SupabaseResult<Pair<ChessGame, List<ChessMove>>> {
        val game = when (val read = remote.game(gameId)) {
            is SupabaseResult.Success -> read.data?.let { keepIfFinished(it) } ?: return SupabaseResult.ServerError(404, "not_found")
            else -> return read.failure()
        }
        return when (val moves = remote.moves(gameId)) {
            is SupabaseResult.Success -> SupabaseResult.Success(game to moves.data.map { it.toDomain() })
            else -> moves.failure()
        }
    }

    override suspend fun myLiveGame(): SupabaseResult<ChessGame?> {
        val me = account.id() ?: return signedOut()
        return remote.myLiveGame(me).mapData { it?.toDomain() }
    }

    override suspend fun makeMove(gameId: String, ply: Int, uci: String): SupabaseResult<ChessGame> =
        gameOf(remote.move(gameId, ply, uci).mapData { it.game })

    override suspend fun resign(gameId: String) = gameOf(remote.resign(gameId).mapData { it.game })

    override suspend fun offerDraw(gameId: String) = gameOf(remote.draw(gameId, "offer").mapData { it.game })

    override suspend fun answerDraw(gameId: String, accept: Boolean) =
        gameOf(remote.draw(gameId, if (accept) "accept" else "decline").mapData { it.game })

    override suspend fun claimTimeout(gameId: String) = gameOf(remote.claimTimeout(gameId).mapData { it.game })

    /**
     * The game's changes as they happen. On a drop it says so, waits, and follows again, reading
     * the game afresh each time, so nothing missed while away is lost.
     */
    override fun follow(gameId: String): Flow<GameEvent> = channelFlow {
        var lastPly = -1
        suspend fun refresh() {
            val read = fetchGame(gameId)
            if (read is SupabaseResult.Success) {
                val (game, moves) = read.data
                val move = moves.lastOrNull()?.takeIf { it.ply > lastPly && lastPly >= 0 }
                lastPly = game.ply
                send(GameEvent.Updated(game, move))
            }
        }
        while (true) {
            try {
                live.follow(gameId, account.id()).collect { signal ->
                    when (signal) {
                        ChessSignal.Subscribed, ChessSignal.Changed -> refresh()
                        is ChessSignal.Watching -> send(GameEvent.Watchers(signal.count))
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Dropped: said, then tried again.
            }
            send(GameEvent.Disconnected)
            delay(retryDelayMs)
        }
    }

    // Challenges

    private val challenges = mutableMapOf<String, MutableStateFlow<List<Challenge>>>()
    private fun challengesOf(partyId: String) = synchronized(challenges) { challenges.getOrPut(partyId) { MutableStateFlow(emptyList()) } }

    override suspend fun challenge(partyId: String, toUser: String?, timeControl: TimeControl, colour: ColourChoice, rated: Boolean): SupabaseResult<Challenge> {
        val made = remote.createChallenge(partyId, toUser, timeControl.seconds, colour.wire, rated)
        if (made !is SupabaseResult.Success) return made.failure()
        val challenge = made.data.challenge?.toDomain() ?: return SupabaseResult.ServerError(500, "server_error")
        challengesOf(partyId).value = listOf(challenge) + challengesOf(partyId).value.filter { it.id != challenge.id }
        return SupabaseResult.Success(challenge)
    }

    override suspend fun respond(challengeId: String, accept: Boolean): SupabaseResult<ChessGame?> {
        val answer = remote.respondChallenge(challengeId, accept)
        if (answer !is SupabaseResult.Success) return answer.failure()
        answer.data.challenge?.let { dto -> challengesOf(dto.partyId).value = challengesOf(dto.partyId).value.filter { it.id != dto.id } }
        return SupabaseResult.Success(answer.data.game?.toDomain())
    }

    /** The challenger's "no" cancels their own challenge. */
    override suspend fun cancel(challengeId: String): SupabaseResult<Unit> = respond(challengeId, accept = false).mapData { }

    override suspend fun fetchChallenge(challengeId: String): SupabaseResult<Challenge?> =
        remote.challenge(challengeId).mapData { it?.toDomain() }

    override fun observeChallenges(partyId: String): Flow<List<Challenge>> = challengesOf(partyId)

    override suspend fun refreshChallenges(partyId: String): SupabaseResult<Unit> =
        remote.openChallenges(partyId).mapData { rows -> challengesOf(partyId).value = rows.mapNotNull { it.toDomain() } }

    // Ratings and records

    override fun observeRatings(userId: String): Flow<List<Rating>> = dao.observeRatings(userId).map { rows ->
        rows.mapNotNull { row -> TimeControl.of(row.timeControl)?.let { Rating(row.userId, it, row.points, row.games, row.wins, row.draws, row.losses, row.peak) } }
    }

    override suspend fun refreshRatings(userId: String): SupabaseResult<Unit> = remote.ratings(userId).mapData { rows ->
        dao.replaceRatings(userId, rows.mapNotNull { it.toDomain() }.map { ChessRatingEntity(it.userId, it.timeControl.seconds, it.points, it.games, it.wins, it.draws, it.losses, it.peak) })
    }

    override suspend fun headToHead(other: String): SupabaseResult<List<HeadToHead>> {
        val me = account.id() ?: return signedOut()
        return when (val read = remote.headToHead(me, other)) {
            is SupabaseResult.Success -> {
                val records = read.data.mapNotNull { it.toDomain(me) }
                dao.upsertHeadToHead(records.map { ChessHeadToHeadEntity(me, other, it.timeControl?.seconds ?: 0, it.myWins, it.theirWins, it.draws) })
                SupabaseResult.Success(records)
            }
            // Offline: what the phone last knew.
            is SupabaseResult.NetworkError -> SupabaseResult.Success(
                dao.headToHead(me, other).map { HeadToHead(me, other, TimeControl.of(it.timeControl), it.myWins, it.theirWins, it.draws) }
            ).takeIf { it.data.isNotEmpty() } ?: read
            else -> read.failure()
        }
    }

    override suspend fun leaderboard(partyId: String, timeControl: TimeControl?): SupabaseResult<List<Rating>> =
        remote.leaderboard(partyId, timeControl?.seconds).mapData { rows -> rows.mapNotNull { it.toDomain(best = timeControl ?: TimeControl.FIVE) } }

    // Watching

    override suspend fun liveGames(partyId: String): SupabaseResult<List<ChessGame>> =
        remote.liveGames(partyId).mapData { rows -> rows.mapNotNull { it.toDomain() } }

    /** Logs the watch first (the game's channel lets in players and logged watchers), then follows. */
    override fun watch(gameId: String): Flow<GameEvent> = channelFlow {
        remote.watch(gameId)
        follow(gameId).collect { send(it) }
    }

    /** A finished game read from the server is kept for the party's recent games. */
    private suspend fun keepIfFinished(dto: ChessGameDto): ChessGame? {
        val game = dto.toDomain() ?: return null
        if (game.status == GameStatus.FINISHED) {
            dao.keepFinishedGame(
                ChessFinishedGameEntity(
                    game.id, game.partyId, game.whiteId, game.blackId, game.timeControl.seconds, game.rated, dto.result, dto.resultReason,
                    game.fen, game.ply, game.endedAt?.toEpochMilli() ?: 0L, game.whiteChange, game.blackChange
                )
            )
        }
        return game
    }

    private suspend fun gameOf(result: SupabaseResult<ChessGameDto?>): SupabaseResult<ChessGame> = when (result) {
        is SupabaseResult.Success -> result.data?.let { keepIfFinished(it) }?.let { SupabaseResult.Success(it) } ?: SupabaseResult.ServerError(500, "server_error")
        else -> result.failure()
    }

    companion object {
        const val RETRY_DELAY_MS = 3_000L
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> SupabaseResult<*>.failure(): SupabaseResult<T> = this as SupabaseResult<T>

internal inline fun <T, R> SupabaseResult<T>.mapData(transform: (T) -> R): SupabaseResult<R> = when (this) {
    is SupabaseResult.Success -> SupabaseResult.Success(transform(data))
    else -> failure()
}
