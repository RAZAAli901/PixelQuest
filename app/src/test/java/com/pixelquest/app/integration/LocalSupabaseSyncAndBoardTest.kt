package com.pixelquest.app.integration

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.auth.AuthRepositoryImpl
import com.pixelquest.app.auth.SupabaseAiAccess
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiProxyClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeGeminiCall
import com.pixelquest.app.data.repository.CloudProfileRepositoryImpl
import com.pixelquest.app.data.repository.LeaderboardRepositoryImpl
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.LocalSupabase
import com.pixelquest.app.worker.ProfileSyncWorker
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

/**
 * More of the app's cloud code against the local stack (opt-in: PIXELQUEST_LOCAL_SUPABASE=1): the
 * real ProfileSyncWorker, reporting a player, paging the leaderboard, and refreshing an expiring
 * session before an AI call.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalSupabaseSyncAndBoardTest {

    @Before
    fun onlyWithTheLocalStack() = assumeTrue("Set PIXELQUEST_LOCAL_SUPABASE=1 with the local stack running", LocalSupabase.enabled)

    private val context: Context = ApplicationProvider.getApplicationContext()

    private suspend fun signedIn(tag: String): Pair<SupabaseClient, String> {
        val supabase = LocalSupabase.client()
        val auth = AuthRepositoryImpl(supabase.auth, supabase.postgrest)
        val email = LocalSupabase.newEmail(tag)
        assertEquals(SupabaseResult.Success(Unit), auth.sendEmailCode(email))
        val user = auth.verifyEmailCode(email, LocalSupabase.latestCode(email)) as SupabaseResult.Success
        return supabase to user.data.id
    }

    private fun row(userId: String) =
        LocalSupabase.rows("/rest/v1/profiles?id=eq.$userId&select=level,total_xp,leaderboard_opt_in").singleOrNull() as JsonObject?

    private fun runSync(profiles: FakeUserProfileRepository, cloud: CloudProfileRepositoryImpl, streaks: FakeStreakRepository) = runBlocking {
        TestListenableWorkerBuilder<ProfileSyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    ProfileSyncWorker(appContext, workerParameters, profiles, cloud, streaks)
            })
            .build()
            .doWork()
    }

    @Test
    fun theSyncWorker_sendsProgress_holdsBackOlderNumbers_andOptsOutSpectators() = runBlocking {
        val (supabase, userId) = signedIn("worker")
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(
                username = "Rook", avatarId = "avatar_hero", level = 3, totalXp = 700, supabaseUserId = userId,
                leaderboardOptIn = true, leaderboardDisplayName = "Rook_${userId.take(5)}"
            )
        )
        val streaks = FakeStreakRepository(StreakEntity(currentStreak = 4, longestStreak = 9))
        val cloud = CloudProfileRepositoryImpl(supabase.postgrest, supabase.auth, profiles, streaks)

        // First sync makes the row.
        assertEquals(ListenableWorker.Result.success(), runSync(profiles, cloud, streaks))
        assertEquals("700", row(userId)!!["total_xp"]!!.jsonPrimitive.content)

        // More progress is sent.
        profiles.profile.value = profiles.profile.value!!.copy(totalXp = 900)
        assertEquals(ListenableWorker.Result.success(), runSync(profiles, cloud, streaks))
        assertEquals("900", row(userId)!!["total_xp"]!!.jsonPrimitive.content)

        // Another device got further: this one's numbers are held back.
        LocalSupabase.setProfileColumns(userId, """"level":50""")
        profiles.profile.value = profiles.profile.value!!.copy(totalXp = 950)
        assertEquals(ListenableWorker.Result.success(), runSync(profiles, cloud, streaks))
        assertEquals("900", row(userId)!!["total_xp"]!!.jsonPrimitive.content)
        assertEquals("50", row(userId)!!["level"]!!.jsonPrimitive.content)

        // Left the board on this device: the sync only clears the flag.
        profiles.profile.value = profiles.profile.value!!.copy(leaderboardOptIn = false)
        assertEquals(ListenableWorker.Result.success(), runSync(profiles, cloud, streaks))
        assertEquals("false", row(userId)!!["leaderboard_opt_in"]!!.jsonPrimitive.content)
        assertEquals("900", row(userId)!!["total_xp"]!!.jsonPrimitive.content)

        // A signed-in spectator who never joined: nothing is uploaded at all.
        val (spectatorClient, spectatorId) = signedIn("spectator")
        val spectatorProfiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Watcher", avatarId = "avatar_hero", level = 7, totalXp = 5000, supabaseUserId = spectatorId)
        )
        val spectatorCloud = CloudProfileRepositoryImpl(spectatorClient.postgrest, spectatorClient.auth, spectatorProfiles, streaks)
        assertEquals(ListenableWorker.Result.success(), runSync(spectatorProfiles, spectatorCloud, streaks))
        assertEquals(null, row(spectatorId))
    }

    @Test
    fun reportingAPlayer_filesItUnderTheReportersAccount() = runBlocking {
        val (_, reportedId) = signedIn("reported")
        LocalSupabase.setProfileColumns(reportedId, """"display_name":"Bad_${reportedId.take(5)}","leaderboard_opt_in":true""")
        val (reporter, reporterId) = signedIn("reporter")

        val result = LeaderboardRepositoryImpl(reporter.postgrest, reporter.auth).reportProfile(reportedId, "Flagged offensive display name: Bad")

        assertEquals(SupabaseResult.Success(Unit), result)
        val filed = LocalSupabase.rows("/rest/v1/reports?reporter_id=eq.$reporterId&select=reported_profile_id")
        assertEquals(reportedId, (filed.single() as JsonObject)["reported_profile_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun theLeaderboard_pagesPastTheFirst20_inOrder_withoutRepeats() = runBlocking {
        // 23 players at levels no other test uses, so the order is known.
        LocalSupabase.asService("/rest/v1/profiles?level=gte.500", method = "DELETE")
        val ids = (0 until 23).map { i ->
            val id = LocalSupabase.createPlayer(LocalSupabase.newEmail("page$i"))
            LocalSupabase.setProfileColumns(id, """"display_name":"Pager_$i","level":${600 - i},"leaderboard_opt_in":true""")
            id
        }
        val (viewer, _) = signedIn("viewer")
        val board = LeaderboardRepositoryImpl(viewer.postgrest, viewer.auth)

        val first = (board.getTopByLevel(limit = 20, offset = 0) as SupabaseResult.Success).data
        val second = (board.getTopByLevel(limit = 20, offset = 20) as SupabaseResult.Success).data
        val seen = (first + second).map { it.id }.filter { it in ids }

        assertEquals(ids, seen)
        assertEquals(seen.size, seen.toSet().size)
    }

    @Test
    fun anExpiringSession_isRefreshedBeforeAnAiCall() = runBlocking {
        val (supabase, _) = signedIn("refresh")
        val access = SupabaseAiAccess(supabase.auth)
        val session = supabase.auth.currentSessionOrNull()!!
        // As if the app had been in the background for an hour.
        supabase.auth.importSession(session.copy(expiresAt = Clock.System.now() - 10.seconds))

        val token = access.accessToken()

        assertNotEquals("A new token, not the expired one", session.accessToken, token)
        val proxy = GeminiProxyClient(LocalSupabase.status.apiUrl, LocalSupabase.status.publishableKey, { access.accessToken() }, HttpClient(Android))
        val result = safeGeminiCall { proxy.generateContent("How am I doing?") }
        assertTrue("The proxy accepted it (the fake Gemini key then fails): $result", result is GeminiResult.ApiError && result.statusCode == 502)
    }
}
