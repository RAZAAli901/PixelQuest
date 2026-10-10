package com.pixelquest.app.integration

import android.app.Application
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
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.LocalSupabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The app's own cloud code against a local Supabase (opt-in: PIXELQUEST_LOCAL_SUPABASE=1 with the
 * stack running; see docs/LOCAL_SUPABASE.md). Sign-in by emailed code, joining the leaderboard,
 * ranks, sync decisions, leaving, deleting the account, and the AI proxy with a real session.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalSupabaseAppTest {

    @Before
    fun onlyWithTheLocalStack() = assumeTrue("Set PIXELQUEST_LOCAL_SUPABASE=1 with the local stack running", LocalSupabase.enabled)

    private suspend fun signedIn(tag: String): Pair<io.github.jan.supabase.SupabaseClient, String> {
        val supabase = LocalSupabase.client()
        val auth = AuthRepositoryImpl(supabase.auth, supabase.postgrest)
        val email = LocalSupabase.newEmail(tag)
        assertEquals(SupabaseResult.Success(Unit), auth.sendEmailCode(email))
        val user = auth.verifyEmailCode(email, LocalSupabase.latestCode(email))
        assertTrue("$user", user is SupabaseResult.Success)
        return supabase to (user as SupabaseResult.Success).data.id
    }

    @Test
    fun aPlayersWholeCloudLife_signIn_join_rank_sync_leave_delete() = runBlocking {
        // Streaks no other test uses (and within the stored limits), cleared from earlier runs.
        LocalSupabase.asService("/rest/v1/profiles?current_streak=gte.30000", method = "DELETE")
        val streak = (30_000..36_000).random()

        val (supabase, userId) = signedIn("whole")
        val suffix = userId.take(6)
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Aria", avatarId = "avatar_hero", level = 4, totalXp = 1234, supabaseUserId = userId)
        )
        val cloud = CloudProfileRepositoryImpl(
            supabase.postgrest, supabase.auth, profiles, FakeStreakRepository(StreakEntity(currentStreak = streak, longestStreak = streak))
        )

        // Joining the board uploads the row.
        assertEquals(SupabaseResult.Success(Unit), cloud.updateOptInAndSync(optIn = true, displayName = "Aria_$suffix"))
        val stored = LocalSupabase.rows("/rest/v1/profiles?id=eq.$userId&select=display_name,level,total_xp,current_streak,leaderboard_opt_in").single()
        assertEquals("Aria_$suffix", stored.jsonObjectField("display_name"))
        assertEquals("1234", stored.jsonObjectField("total_xp"))
        assertEquals("true", stored.jsonObjectField("leaderboard_opt_in"))

        // The leaderboard: top of the streak board, rank 1, and "around you".
        val board = LeaderboardRepositoryImpl(supabase.postgrest, supabase.auth)
        val top = (board.getTopByStreak(limit = 20, offset = 0) as SupabaseResult.Success).data
        assertEquals(userId, top.first().id)
        val rank = (board.getCurrentUserRank(LeaderboardSortMode.STREAK, userId) as SupabaseResult.Success).data!!
        assertEquals(1, rank.rank)
        val around = (board.getPlayersAroundYou(LeaderboardSortMode.STREAK, userId) as SupabaseResult.Success).data!!
        assertEquals(1, around.you.rank)
        assertEquals(userId, around.entries.first().profile.id)

        // More progress on this device is sent; less than the server has (another device) is not.
        profiles.profile.value = profiles.profile.value!!.copy(totalXp = 2000)
        assertEquals(SupabaseResult.Success(true), cloud.pushProfileToCloud())
        LocalSupabase.setProfileColumns(userId, """"total_xp":900000""")
        assertEquals(SupabaseResult.Success(false), cloud.pushProfileToCloud())

        // Leaving the board: another player no longer sees the row.
        assertEquals(SupabaseResult.Success(Unit), cloud.optOutFromLeaderboard())
        val (other, _) = signedIn("other")
        val seenByOther = (LeaderboardRepositoryImpl(other.postgrest, other.auth).getTopByStreak(limit = 50, offset = 0) as SupabaseResult.Success).data
        assertTrue(seenByOther.none { it.id == userId })

        // Deleting the account removes the row and the sign-in.
        val auth = AuthRepositoryImpl(supabase.auth, supabase.postgrest)
        val deleted = auth.deleteAccount()
        assertEquals("$deleted", SupabaseResult.Success(Unit), deleted)
        assertTrue(LocalSupabase.rows("/rest/v1/profiles?id=eq.$userId").isEmpty())
        assertEquals(404, LocalSupabase.asService("/auth/v1/admin/users/$userId").status)
    }

    @Test
    fun theAiProxy_takesASignedInSession_andCountsItOnTheAccount() = runBlocking {
        val (supabase, userId) = signedIn("coach")
        val access = SupabaseAiAccess(supabase.auth)
        val proxy = GeminiProxyClient(LocalSupabase.status.apiUrl, LocalSupabase.status.publishableKey, { access.accessToken() }, HttpClient(Android))

        // The local function has a fake Gemini key, so a call that gets through comes back as
        // Gemini's refusal (502) after being counted.
        val result = safeGeminiCall { proxy.generateContent("How am I doing?") }
        assertTrue("$result", result is GeminiResult.ApiError && result.statusCode == 502)
        val counted = LocalSupabase.rows("/rest/v1/ai_proxy_usage?device_id=eq.user:$userId&select=calls").single()
        assertEquals("1", counted.jsonObjectField("calls"))

        AuthRepositoryImpl(supabase.auth, supabase.postgrest).signOut()
        assertTrue(safeGeminiCall { proxy.generateContent("hi") } is GeminiResult.SignInRequired)
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectField(name: String) =
        (this as kotlinx.serialization.json.JsonObject)[name]!!.jsonPrimitive.content
}
