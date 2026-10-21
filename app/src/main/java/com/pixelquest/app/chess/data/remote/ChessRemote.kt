package com.pixelquest.app.chess.data.remote

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.domain.ai.AiAccess
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chess on the server (docs/chess-design.md): the referee's functions for everything that
 * changes a game or a challenge, and plain reads (under RLS) for what's shown. A refusal comes
 * back as a ServerError whose message is its token.
 */
interface ChessRemote {
    suspend fun createChallenge(partyId: String, toUser: String?, timeControlSec: Int, colour: String, rated: Boolean): SupabaseResult<ChallengeAnswerDto>
    suspend fun respondChallenge(challengeId: String, accept: Boolean): SupabaseResult<ChallengeAnswerDto>
    suspend fun move(gameId: String, ply: Int, uci: String): SupabaseResult<GameAnswerDto>
    suspend fun resign(gameId: String): SupabaseResult<GameAnswerDto>
    suspend fun draw(gameId: String, action: String): SupabaseResult<GameAnswerDto>
    suspend fun claimTimeout(gameId: String): SupabaseResult<GameAnswerDto>
    suspend fun liveGames(partyId: String): SupabaseResult<List<ChessGameDto>>

    suspend fun game(gameId: String): SupabaseResult<ChessGameDto?>
    suspend fun moves(gameId: String): SupabaseResult<List<ChessMoveDto>>
    suspend fun myLiveGame(me: String): SupabaseResult<ChessGameDto?>
    suspend fun openChallenges(partyId: String): SupabaseResult<List<ChallengeDto>>
    suspend fun ratings(userId: String): SupabaseResult<List<RatingDto>>
    suspend fun headToHead(me: String, other: String): SupabaseResult<List<HeadToHeadDto>>
    suspend fun leaderboard(partyId: String, timeControlSec: Int?): SupabaseResult<List<RatingDto>>
    suspend fun finishedGames(partyId: String, limit: Int): SupabaseResult<List<ChessGameDto>>

    /** Logs a watch, as the game's channel requires of a member who isn't playing. */
    suspend fun watch(gameId: String): SupabaseResult<Unit>
}

@Serializable
private data class FunctionError(val error: String = "refused")

@Singleton
class SupabaseChessRemote(
    private val postgrest: Postgrest,
    private val access: AiAccess,
    private val supabaseUrl: String,
    private val anonKey: String,
    private val http: OkHttpClient
) : ChessRemote {
    @Inject
    constructor(postgrest: Postgrest, access: AiAccess) : this(
        postgrest, access, BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY,
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    )

    private val json = Json { ignoreUnknownKeys = true }

    private suspend inline fun <reified R> function(name: String, crossinline body: JsonObjectBuilder.() -> Unit): SupabaseResult<R> = withContext(Dispatchers.IO) {
        val token = access.accessToken() ?: return@withContext SupabaseResult.AuthError(IllegalStateException("signed out"), "Sign in to play chess.")
        val request = Request.Builder()
            .url("${supabaseUrl.trim().trimEnd('/')}/functions/v1/$name")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $token")
            .post(buildJsonObject(body).toString().toRequestBody("application/json".toMediaType()))
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> SupabaseResult.Success(json.decodeFromString<R>(text))
                    response.code == 401 -> SupabaseResult.AuthError(IllegalStateException("401"), "Sign in to play chess.")
                    response.code >= 500 -> SupabaseResult.ServerError(response.code, "server_error")
                    else -> SupabaseResult.ServerError(response.code, runCatching { json.decodeFromString<FunctionError>(text).error }.getOrDefault("refused"))
                }
            }
        } catch (e: IOException) {
            SupabaseResult.NetworkError(e)
        }
    }

    override suspend fun createChallenge(partyId: String, toUser: String?, timeControlSec: Int, colour: String, rated: Boolean) =
        function<ChallengeAnswerDto>("chess-create-challenge") {
            put("party_id", partyId)
            if (toUser != null) put("to_user", toUser)
            put("time_control", timeControlSec)
            put("colour", colour)
            put("rated", rated)
        }

    override suspend fun respondChallenge(challengeId: String, accept: Boolean) =
        function<ChallengeAnswerDto>("chess-respond-challenge") {
            put("challenge_id", challengeId)
            put("accept", accept)
        }

    override suspend fun move(gameId: String, ply: Int, uci: String) = function<GameAnswerDto>("chess-move") {
        put("game_id", gameId)
        put("ply", ply)
        put("uci", uci)
    }

    override suspend fun resign(gameId: String) = function<GameAnswerDto>("chess-resign") { put("game_id", gameId) }

    override suspend fun draw(gameId: String, action: String) = function<GameAnswerDto>("chess-draw") {
        put("game_id", gameId)
        put("action", action)
    }

    override suspend fun claimTimeout(gameId: String) = function<GameAnswerDto>("chess-claim-timeout") { put("game_id", gameId) }

    override suspend fun liveGames(partyId: String): SupabaseResult<List<ChessGameDto>> =
        when (val answer = function<LiveGamesDto>("chess-live-games") { put("party_id", partyId) }) {
            is SupabaseResult.Success -> SupabaseResult.Success(answer.data.games)
            is SupabaseResult.NetworkError -> answer
            is SupabaseResult.AuthError -> answer
            is SupabaseResult.ServerError -> answer
            is SupabaseResult.UnknownError -> answer
        }

    override suspend fun game(gameId: String) = safeSupabaseCall {
        postgrest[GAMES].select { filter { eq("id", gameId) } }.decodeList<ChessGameDto>().firstOrNull()
    }

    override suspend fun moves(gameId: String) = safeSupabaseCall {
        postgrest[MOVES].select {
            filter { eq("game_id", gameId) }
            order("ply", Order.ASCENDING)
        }.decodeList<ChessMoveDto>()
    }

    override suspend fun myLiveGame(me: String) = safeSupabaseCall {
        postgrest[GAMES].select {
            filter {
                or {
                    eq("white_id", me)
                    eq("black_id", me)
                }
                isIn("status", listOf("waiting", "active"))
            }
        }.decodeList<ChessGameDto>().firstOrNull()
    }

    override suspend fun openChallenges(partyId: String) = safeSupabaseCall {
        postgrest[CHALLENGES].select {
            filter {
                eq("party_id", partyId)
                eq("status", "open")
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<ChallengeDto>()
    }

    override suspend fun ratings(userId: String) = safeSupabaseCall {
        postgrest[RATINGS].select { filter { eq("user_id", userId) } }.decodeList<RatingDto>()
    }

    override suspend fun headToHead(me: String, other: String) = safeSupabaseCall {
        val (low, high) = if (me < other) me to other else other to me
        postgrest[HEAD_TO_HEAD].select {
            filter {
                eq("user_low", low)
                eq("user_high", high)
            }
        }.decodeList<HeadToHeadDto>()
    }

    override suspend fun leaderboard(partyId: String, timeControlSec: Int?) = safeSupabaseCall {
        postgrest[LEADERBOARD].select(Columns.list("user_id", "time_control_sec", "int_points", "games", "wins", "draws", "losses", "place")) {
            filter {
                eq("party_id", partyId)
                if (timeControlSec == null) exact("time_control_sec", null) else eq("time_control_sec", timeControlSec)
            }
            order("place", Order.ASCENDING)
        }.decodeList<RatingDto>()
    }

    override suspend fun finishedGames(partyId: String, limit: Int) = safeSupabaseCall {
        postgrest[GAMES].select {
            filter {
                eq("party_id", partyId)
                eq("status", "finished")
            }
            order("ended_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<ChessGameDto>()
    }

    override suspend fun watch(gameId: String) = safeSupabaseCall {
        postgrest.rpc("chess_watch", buildJsonObject { put("game_id", gameId) })
        Unit
    }

    internal companion object {
        const val GAMES = "chess_games"
        const val MOVES = "chess_moves"
        const val CHALLENGES = "chess_challenges"
        const val RATINGS = "chess_ratings"
        const val HEAD_TO_HEAD = "chess_head_to_head_totals"
        const val LEADERBOARD = "chess_party_leaderboard"
    }
}

