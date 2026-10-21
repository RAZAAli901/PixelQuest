package com.pixelquest.app.chess.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.track
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** What a game's live connection says: something changed (fetch it), who's watching, or it's up. */
sealed interface ChessSignal {
    data object Changed : ChessSignal
    data class Watching(val count: Int) : ChessSignal
    data object Subscribed : ChessSignal
}

/**
 * A game's live changes (docs/chess-design.md, Realtime). Cold: collecting subscribes. A change is
 * only a sign to fetch: the game shown is always what the server's tables say.
 */
fun interface ChessLiveSource {
    fun follow(gameId: String, me: String?): Flow<ChessSignal>
}

@Singleton
class SupabaseChessLiveSource @Inject constructor(private val supabase: SupabaseClient) : ChessLiveSource {

    override fun follow(gameId: String, me: String?): Flow<ChessSignal> = channelFlow {
        // The game's row (moves, clocks, offers and its end), under the reader's own RLS.
        val changes = supabase.channel("chess-game-$gameId")
        val updates = changes.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "chess_games"
            filter("id", FilterOperator.EQ, gameId)
        }
        launch { updates.collect { send(ChessSignal.Changed) } }

        // Who's watching: presence on the game's private channel (members only, 20261019000093).
        val room = supabase.channel("chess:game:$gameId") { isPrivate = true }
        val present = mutableMapOf<String, Int>()
        launch {
            room.presenceChangeFlow().collect { action ->
                action.joins.keys.forEach { present[it] = 1 }
                action.leaves.keys.forEach { present.remove(it) }
                send(ChessSignal.Watching(present.size))
            }
        }
        try {
            changes.subscribe(blockUntilSubscribed = true)
            runCatching {
                room.subscribe(blockUntilSubscribed = true)
                room.track(buildJsonObject { put("user", me ?: "") })
            }
            send(ChessSignal.Subscribed)
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                listOf<RealtimeChannel>(changes, room).forEach { runCatching { supabase.realtime.removeChannel(it) } }
            }
        }
    }
}
