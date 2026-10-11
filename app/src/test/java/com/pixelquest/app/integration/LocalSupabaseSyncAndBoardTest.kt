package com.pixelquest.app.integration

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.auth.AuthRepositoryImpl
import com.pixelquest.app.auth.CloudAccountLink
import com.pixelquest.app.auth.SupabaseAiAccess
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiProxyClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeGeminiCall
import com.pixelquest.app.data.repository.CloudProfileRepositoryImpl
import com.pixelquest.app.data.repository.LeaderboardRepositoryImpl
import com.pixelquest.app.data.repository.PendingLeaderboardChoice
import com.pixelquest.app.testing.FakeSettingsRepository
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
 * real ProfileSyncWorker, reporting a player, paging the leaderboard, refreshing an expiring
 * session before an AI call, an account that is new to a phone keeping its place on the board, and
 * one account on two phones following the latest leaderboard choice.
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

    private fun runSync(
        profiles: FakeUserProfileRepository,
        cloud: CloudProfileRepositoryImpl,
        streaks: FakeStreakRepository,
        pending: PendingLeaderboardChoice? = null
    ) = runBlocking {
        TestListenableWorkerBuilder<ProfileSyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    ProfileSyncWorker(appContext, workerParameters, profiles, cloud, streaks, pending)
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

    @Test
    fun anAccountNewToThisPhone_keepsItsPlaceOnTheBoard_throughTheNextSync() = runBlocking {
        val (supabase, userId) = signedIn("returning")
        val name = "Back_${userId.take(5)}"
        LocalSupabase.setProfileColumns(userId, """"display_name":"$name","leaderboard_opt_in":true,"total_xp":300""")
        // A reinstalled app: nothing linked or chosen on this phone yet.
        val profiles = FakeUserProfileRepository(UserProfileEntity(username = "Rook", avatarId = "avatar_hero", totalXp = 400))
        val streaks = FakeStreakRepository(StreakEntity(currentStreak = 2, longestStreak = 2))
        val cloud = CloudProfileRepositoryImpl(supabase.postgrest, supabase.auth, profiles, streaks)
        val prefs = context.getSharedPreferences(CloudAccountLink.PREFS_NAME, Context.MODE_PRIVATE)

        CloudAccountLink(prefs, profiles, FakeSettingsRepository(), cloud).onSignedIn(userId)

        // Its own row, read through the row-level rules, gives back its choice and name.
        assertEquals(userId, profiles.profile.value!!.supabaseUserId)
        assertTrue(profiles.profile.value!!.leaderboardOptIn)
        assertEquals(name, profiles.profile.value!!.leaderboardDisplayName)

        // The next sync keeps it on the board (it used to clear the flag), with this phone's progress.
        assertEquals(ListenableWorker.Result.success(), runSync(profiles, cloud, streaks))
        assertEquals("true", row(userId)!!["leaderboard_opt_in"]!!.jsonPrimitive.content)
        assertEquals("400", row(userId)!!["total_xp"]!!.jsonPrimitive.content)
    }

    @Test
    fun oneAccountOnTwoPhones_followsTheLatestChoice() = runBlocking {
        val (phoneA, userId) = signedIn("twophones")
        // The second phone signed in to the same account (its session, without another email).
        val phoneB = LocalSupabase.client().also { it.auth.importSession(phoneA.auth.currentSessionOrNull()!!) }
        val name = "Duo_${userId.take(5)}"
        val streaks = FakeStreakRepository(StreakEntity(currentStreak = 2, longestStreak = 2))
        fun phone(tag: String, client: SupabaseClient): Triple<FakeUserProfileRepository, PendingLeaderboardChoice, CloudProfileRepositoryImpl> {
            val profiles = FakeUserProfileRepository(
                UserProfileEntity(username = "Duo", avatarId = "avatar_hero", level = 1, totalXp = 300, supabaseUserId = userId)
            )
            val pending = PendingLeaderboardChoice(context.getSharedPreferences("phone-$tag-${userId.take(5)}", Context.MODE_PRIVATE))
                .also { it.startTracking() }
            return Triple(profiles, pending, CloudProfileRepositoryImpl(client.postgrest, client.auth, profiles, streaks, pending))
        }
        val (profilesA, pendingA, cloudA) = phone("a", phoneA)
        val (profilesB, pendingB, cloudB) = phone("b", phoneB)
        fun onTheBoard() = row(userId)!!["leaderboard_opt_in"]!!.jsonPrimitive.content == "true"

        // Joined on phone A; phone B follows the account onto the board.
        assertEquals(SupabaseResult.Success(Unit), cloudA.updateOptInAndSync(optIn = true, displayName = name))
        runSync(profilesB, cloudB, streaks, pendingB)
        assertTrue(profilesB.profile.value!!.leaderboardOptIn)
        assertEquals(name, profilesB.profile.value!!.leaderboardDisplayName)

        // Left on phone A: phone B's next sync follows instead of putting the account back.
        assertEquals(SupabaseResult.Success(Unit), cloudA.optOutFromLeaderboard())
        assertEquals(ListenableWorker.Result.success(), runSync(profilesB, cloudB, streaks, pendingB))
        assertEquals(false, profilesB.profile.value!!.leaderboardOptIn)
        assertEquals(false, onTheBoard())

        // Joined again on phone B: phone A's next sync follows instead of taking it off again.
        assertEquals(SupabaseResult.Success(Unit), cloudB.updateOptInAndSync(optIn = true, displayName = name))
        assertEquals(ListenableWorker.Result.success(), runSync(profilesA, cloudA, streaks, pendingA))
        assertTrue(profilesA.profile.value!!.leaderboardOptIn)
        assertEquals(true, onTheBoard())
        assertEquals(null, pendingA.pendingFor(userId))
        assertEquals(null, pendingB.pendingFor(userId))
    }

    @Test
    fun aNameAnotherPlayerHas_isRefused_andThisPhoneStaysOffTheBoard() = runBlocking {
        val (first, firstId) = LocalSupabase.signedInPlayer("taken1")
        val (second, secondId) = LocalSupabase.signedInPlayer("taken2")
        val name = "Taken_${firstId.take(6)}"
        val streaks = FakeStreakRepository(StreakEntity())
        fun phone(id: String) = FakeUserProfileRepository(UserProfileEntity(username = "Hero", avatarId = "avatar_hero", supabaseUserId = id))

        assertEquals(
            SupabaseResult.Success(Unit),
            CloudProfileRepositoryImpl(first.postgrest, first.auth, phone(firstId), streaks).updateOptInAndSync(optIn = true, displayName = name)
        )

        val secondPhone = phone(secondId)
        val pending = PendingLeaderboardChoice(context.getSharedPreferences("taken-${secondId.take(5)}", Context.MODE_PRIVATE))
        val refused = CloudProfileRepositoryImpl(second.postgrest, second.auth, secondPhone, streaks, pending)
            .updateOptInAndSync(optIn = true, displayName = name.lowercase())

        assertEquals(SupabaseResult.ServerError(409, CloudProfileRepositoryImpl.NAME_TAKEN), refused)
        assertEquals(false, secondPhone.profile.value!!.leaderboardOptIn)
        assertEquals(null, pending.pendingFor(secondId))
        assertEquals("the second player has no row", null, row(secondId))
    }
}

