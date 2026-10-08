package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Leaving (or never joining) the leaderboard writes only the opt-in flag. It used to upsert a full row
 * with level, XP and streaks, and the sync worker calls it for every signed-in player who isn't opted
 * in, so a spectator uploaded their stats on each completion.
 */
class OptOutUploadsNoStatsTest {

    private val requests = mutableListOf<HttpRequestData>()

    private val supabase = createSupabaseClient("https://test.supabase.co", "anon-key") {
        httpEngine = MockEngine { request ->
            requests += request
            respond("[]", HttpStatusCode.OK)
        }
        install(Postgrest)
    }

    private fun bodyOf(request: HttpRequestData): String = when (val body = request.body) {
        is TextContent -> body.text
        is OutgoingContent.ByteArrayContent -> String(body.bytes())
        else -> body.toString()
    }

    @Test
    fun optingOut_patchesOnlyTheFlag_onTheSignedInPlayersRow() = runBlocking {
        val user = mockk<UserInfo> { every { id } returns "user-1" }
        val auth = mockk<Auth> { every { currentUserOrNull() } returns user }
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Aria", avatarId = "avatar_hero", level = 7, totalXp = 4200, supabaseUserId = "user-1", leaderboardOptIn = true)
        )
        val repository = CloudProfileRepositoryImpl(supabase.postgrest, auth, profiles, FakeStreakRepository())

        val result = repository.optOutFromLeaderboard()

        assertTrue(result is SupabaseResult.Success)
        assertFalse("The local flag is off", profiles.profile.value!!.leaderboardOptIn)
        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertTrue(request.url.toString().contains("/rest/v1/profiles"))
        assertEquals("eq.user-1", request.url.parameters["id"])
        val body = bodyOf(request)
        assertTrue(body.contains("\"leaderboard_opt_in\":false"))
        assertFalse("No stats leave the device", body.contains("total_xp") || body.contains("level") || body.contains("streak"))
    }

    @Test
    fun withNoSignedInPlayer_nothingIsSent() = runBlocking {
        val auth = mockk<Auth> { every { currentUserOrNull() } returns null }
        val repository = CloudProfileRepositoryImpl(supabase.postgrest, auth, FakeUserProfileRepository(), FakeStreakRepository())

        repository.optOutFromLeaderboard()

        assertTrue(requests.isEmpty())
    }
}
