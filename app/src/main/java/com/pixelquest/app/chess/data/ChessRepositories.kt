package com.pixelquest.app.chess.data

import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameEvent
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.flow.Flow

// The calls the chess screens make (docs/chess-design.md). Every one goes to the server, which
// referees: a refusal comes back as a ServerError whose message is its token (not_your_turn,
// illegal_move, game_over, ...). Moves are never queued offline.

/** Playing a game. */
interface ChessRepository {
    /** The game and its moves so far, oldest first. */
    suspend fun fetchGame(gameId: String): SupabaseResult<Pair<ChessGame, List<ChessMove>>>

    /** The player's one live game (waiting or active), if they have one. */
    suspend fun myLiveGame(): SupabaseResult<ChessGame?>

    /**
     * Sends a move ([uci], e.g. "e2e4" or "e7e8q") as move number [ply] (the moves played so far,
     * plus one). Sent again after a lost answer, it's taken once.
     */
    suspend fun makeMove(gameId: String, ply: Int, uci: String): SupabaseResult<ChessGame>

    suspend fun resign(gameId: String): SupabaseResult<ChessGame>

    suspend fun offerDraw(gameId: String): SupabaseResult<ChessGame>

    suspend fun answerDraw(gameId: String, accept: Boolean): SupabaseResult<ChessGame>

    /** Ends the game on time if the opponent's clock has run out by the server's clock. */
    suspend fun claimTimeout(gameId: String): SupabaseResult<ChessGame>

    /** The game's live channel: each change as it happens, reconnecting on its own. */
    fun follow(gameId: String): Flow<GameEvent>
}

/** Challenges in a study party. */
interface ChessChallengeRepository {
    /** A challenge to [toUser], or open to the room when null. */
    suspend fun challenge(partyId: String, toUser: String?, timeControl: TimeControl, colour: ColourChoice, rated: Boolean): SupabaseResult<Challenge>

    /** Accepting makes the game (returned); declining returns null. */
    suspend fun respond(challengeId: String, accept: Boolean): SupabaseResult<ChessGame?>

    suspend fun cancel(challengeId: String): SupabaseResult<Unit>

    /** The party's open challenges, the player's own and those to them or the room. */
    fun observeChallenges(partyId: String): Flow<List<Challenge>>

    suspend fun refreshChallenges(partyId: String): SupabaseResult<Unit>
}

/** Int points, records and leaderboards. */
interface ChessRatingRepository {
    /** A player's ratings, one per time control they've played (cached on the phone). */
    fun observeRatings(userId: String): Flow<List<Rating>>

    suspend fun refreshRatings(userId: String): SupabaseResult<Unit>

    /** The player's record against [other]: per time control, and all together (timeControl null). */
    suspend fun headToHead(other: String): SupabaseResult<List<HeadToHead>>

    /** A party's members by Int points: in [timeControl], or by each member's best when null. */
    suspend fun leaderboard(partyId: String, timeControl: TimeControl?): SupabaseResult<List<Rating>>
}

/** Watching a party's games. */
interface SpectateRepository {
    /** The party's games now being played. */
    suspend fun liveGames(partyId: String): SupabaseResult<List<ChessGame>>

    /** A game's moves, clocks and watcher count, live; joining counts the player as watching. */
    fun watch(gameId: String): Flow<GameEvent>
}
