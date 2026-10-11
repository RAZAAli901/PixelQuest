package com.pixelquest.app.data.remote

import android.app.Application
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.CloudProfileRepositoryImpl
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A streak of 0, level 1 and 0 XP are sent like any other numbers. supabase-kt's JSON leaves out
 * values equal to a field's default, and an upsert keeps the columns it isn't sent: a broken streak
 * never reached the leaderboard, which went on showing the old one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DefaultNumbersSentTest {

    @Test
    fun aBrokenStreak_andAFreshPlayersNumbers_areInTheUpload() = runBlocking {
        var sent: String? = null
        val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
            httpEngine = MockEngine { request ->
                if (request.method == HttpMethod.Post) sent = when (val body = request.body) {
                    is io.ktor.http.content.TextContent -> body.text
                    is io.ktor.http.content.OutgoingContent.ByteArrayContent -> String(body.bytes())
                    else -> error("unexpected body $body")
                }
                // The board still holds yesterday's streak.
                val row = """[{"id":"user-1","display_name":"Aria","current_streak":5,"longest_streak":5,"level":1,"total_xp":0,"leaderboard_opt_in":true}]"""
                respond(if (request.method == HttpMethod.Get) row else "", HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
            }
            install(Postgrest)
        }
        val auth = mockk<Auth> { every { currentUserOrNull() } returns mockk<UserInfo> { every { id } returns "user-1" } }
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Aria", avatarId = "avatar_hero", level = 1, totalXp = 0, supabaseUserId = "user-1",
                leaderboardOptIn = true, leaderboardDisplayName = "Aria")
        )
        val repository = CloudProfileRepositoryImpl(supabase.postgrest, auth, profiles, FakeStreakRepository(StreakEntity(currentStreak = 0, longestStreak = 5)))

        assertEquals(SupabaseResult.Success(Unit), repository.updateOptInAndSync(optIn = true, displayName = "Aria"))

        val row = Json.parseToJsonElement(sent!!).let { if (it is kotlinx.serialization.json.JsonArray) it.jsonArray.first() else it }.jsonObject
        assertEquals("0", row["current_streak"]?.jsonPrimitive?.content)
        assertEquals("1", row["level"]?.jsonPrimitive?.content)
        assertEquals("0", row["total_xp"]?.jsonPrimitive?.content)
        assertEquals("5", row["longest_streak"]?.jsonPrimitive?.content)
    }
}
