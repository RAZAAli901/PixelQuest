package com.pixelquest.app.integration

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.SupabaseAiAccess
import com.pixelquest.app.chess.data.ChessRepositoryImpl
import com.pixelquest.app.chess.data.remote.SupabaseChessRemote
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameEvent
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.sync.SupabaseChessLiveSource
import com.pixelquest.app.darood.data.SetUsernameResult
import com.pixelquest.app.darood.data.SupabaseUsernameRepository
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.testing.LocalSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Chess end to end (J169) through the app's own data layer on three phones against a local
 * Supabase with its Edge Functions served (opt-in: PIXELQUEST_LOCAL_SUPABASE=1; see
 * docs/LOCAL_SUPABASE.md): a challenge, accepting it, a game played to checkmate with each move
 * refereed by chess-move, the opponent and a watching party mate hearing every move live, and the
 * ratings and record the finished game leaves. The same run on two emulators is in
 * docs/QA_CHECKLIST.md.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalSupabaseChessTest {

    @Before
    fun onlyWithTheLocalStack() = assumeTrue("Set PIXELQUEST_LOCAL_SUPABASE=1 with the local stack and functions running", LocalSupabase.enabled)

    private val databases = mutableListOf<AppDatabase>()

    @After
    fun tearDown() {
        databases.forEach { it.close() }
    }

    /** A player's phone: their client, its own cache, and the app's chess repository on them. */
    private inner class Phone(val supabase: SupabaseClient, val userId: String, val username: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val chess = ChessRepositoryImpl(
            SupabaseChessRemote(
                supabase.postgrest, SupabaseAiAccess(supabase.auth), LocalSupabase.status.apiUrl, LocalSupabase.status.publishableKey, OkHttpClient()
            ),
            db.chessDao(), { userId }, SupabaseChessLiveSource(supabase)
        )

        suspend fun rpc(name: String, body: JsonObjectBuilder.() -> Unit): String =
            supabase.postgrest.rpc(name, buildJsonObject(body)).data.trim().trim('"')
    }

    private suspend fun phone(tag: String): Phone {
        val (supabase, id) = LocalSupabase.signedInPlayer(tag)
        val name = "${tag.take(6)}_${id.take(6)}"
        val saved = SupabaseUsernameRepository(supabase.postgrest).setUsername(Username.parse(name)!!)
        assertTrue(saved is SupabaseResult.Success && saved.data is SetUsernameResult.Saved)
        return Phone(supabase, id, name)
    }

    private suspend fun waitUntil(what: String, condition: suspend () -> Boolean) {
        repeat(150) {
            if (condition()) return
            delay(100)
        }
        throw AssertionError("still waiting after 15 s: $what")
    }

    private fun <T> SupabaseResult<T>.ok(what: String): T {
        assertTrue("$what: $this", this is SupabaseResult.Success)
        return (this as SupabaseResult.Success).data
    }

    private fun movesHeard(events: List<GameEvent>) = events.filterIsInstance<GameEvent.Updated>().mapNotNull { it.move?.uci }.distinct()

    @Test
    fun aChallenge_aGameToCheckmate_watchedLive() = runBlocking {
        val aria = phone("aria")
        val bram = phone("bram")
        val cleo = phone("cleo")

        // A study party of the three.
        val party = aria.rpc("create_party") { put("name", "Chess club"); put("max_members", 10); put("party_type", "study") }
        for (mate in listOf(bram, cleo)) {
            aria.rpc("invite_member") { put("party_id", party); put("username", mate.username) }
            mate.rpc("respond_invite") { put("party_id", party); put("accept", true) }
        }

        // Aria challenges Bram, as White; Bram accepts and the game is made.
        val challenge = aria.chess.challenge(party, bram.userId, TimeControl.FIVE, ColourChoice.WHITE, rated = true).ok("the challenge")
        bram.chess.refreshChallenges(party).ok("Bram's challenges")
        val game = bram.chess.respond(challenge.id, accept = true).ok("accepting")!!
        assertEquals(aria.userId, game.whiteId)
        assertEquals(bram.userId, game.blackId)

        // Bram follows the game; Cleo finds it on the Chess tab and watches.
        assertEquals(listOf(game.id), cleo.chess.liveGames(party).ok("the live games").map { it.id })
        val bramHeard = CopyOnWriteArrayList<GameEvent>()
        val cleoHeard = CopyOnWriteArrayList<GameEvent>()
        val following = launch(Dispatchers.IO) { bram.chess.follow(game.id).collect { bramHeard += it } }
        val watching = launch(Dispatchers.IO) { cleo.chess.watch(game.id).collect { cleoHeard += it } }
        waitUntil("Cleo counted as watching") { (bramHeard + cleoHeard).any { it is GameEvent.Watchers && it.count >= 1 } }
        // Realtime reports the channel joined a moment before the server listens; give it that moment.
        delay(3_000)

        // An illegal move is refused by the referee; then the fool's mate, Bram mating.
        assertEquals("illegal_move", (aria.chess.makeMove(game.id, 1, "e2e5") as SupabaseResult.ServerError).message)
        val plies = listOf(aria to "f2f3", bram to "e7e5", aria to "g2g4", bram to "d8h4")
        // Played like people play: each move is heard live on the other two phones before the next.
        for ((n, step) in plies.withIndex()) {
            step.first.chess.makeMove(game.id, n + 1, step.second).ok("move ${n + 1}")
            waitUntil("move ${n + 1} live on Bram's and Cleo's phones") { listOf(bramHeard, cleoHeard).all { step.second in movesHeard(it) } }
        }
        // Nothing more is taken after the end.
        assertTrue(aria.chess.makeMove(game.id, 5, "e2e4") is SupabaseResult.ServerError)

        waitUntil("the end heard by the watcher") { cleoHeard.any { it is GameEvent.Updated && it.game.status == GameStatus.FINISHED } }
        following.cancel(); watching.cancel()
        following.join(); watching.join()

        // The server's account of it, read on each phone.
        for (p in listOf(aria, bram, cleo)) {
            val (final, moves) = p.chess.fetchGame(game.id).ok("the game on ${p.username}'s phone")
            assertEquals(GameStatus.FINISHED, final.status)
            assertEquals(GameResult.BLACK, final.result)
            assertEquals(ResultReason.CHECKMATE, final.reason)
            assertEquals(listOf("f3", "e5", "g4", "Qh4#"), moves.map { it.san })
            assertTrue("rated", final.ratingApplied)
        }
        val ladder = aria.chess.leaderboard(party, TimeControl.FIVE).ok("the party's ladder")
        assertEquals(bram.userId, ladder.first().userId)
        assertTrue(ladder.first { it.userId == bram.userId }.points > ladder.first { it.userId == aria.userId }.points)
        val record = bram.chess.headToHead(aria.userId).ok("the record").first { it.timeControl == null }
        assertEquals(1 to 0, record.myWins to record.theirWins)
    }
}
