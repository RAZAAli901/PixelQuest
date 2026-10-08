package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.SupabaseResult
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A rank is counted on the server. Counting fetched ids, and the API returns at most 1000 rows per
 * request, so a hero with 2500 others ahead was told they were about #1000.
 */
class LeaderboardRankCountTest {

    private val requests = mutableListOf<HttpRequestData>()

    /** The hero's own row, then a server-side count for each of the three "ahead of you" queries. */
    private fun repository(vararg counts: Int): LeaderboardRepositoryImpl {
        val pending = counts.toMutableList()
        val supabase = createSupabaseClient("https://test.supabase.co", "anon-key") {
            httpEngine = MockEngine { request ->
                requests += request
                if (request.method == HttpMethod.Head) {
                    respond("", HttpStatusCode.OK, headersOf(HttpHeaders.ContentRange, "*/${pending.removeAt(0)}"))
                } else {
                    val me = """[{"id":"me","display_name":"Me","current_streak":4,"longest_streak":9,"level":3,"total_xp":800,"leaderboard_opt_in":true}]"""
                    respond(me, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(Postgrest)
        }
        return LeaderboardRepositoryImpl(supabase.postgrest)
    }

    @Test
    fun aRankPastTheFirstThousand_isCountedExactly() = runBlocking {
        val result = repository(2500, 40, 3).getCurrentUserRank(LeaderboardSortMode.STREAK, userId = "me")

        assertEquals(2544, (result as SupabaseResult.Success).data!!.rank)
        val counts = requests.filter { it.method == HttpMethod.Head }
        assertEquals("Three counts, no rows downloaded", 3, counts.size)
        assertTrue(counts.all { it.headers["Prefer"].orEmpty().contains("count=exact") })
        assertEquals("gt.4", counts[0].url.parameters["current_streak"])
        assertEquals("eq.true", counts[0].url.parameters["leaderboard_opt_in"])
    }

    @Test
    fun theTopHero_isRankOne() = runBlocking {
        val result = repository(0, 0, 0).getCurrentUserRank(LeaderboardSortMode.LEVEL, userId = "me")

        assertEquals(1, (result as SupabaseResult.Success).data!!.rank)
        assertEquals("gt.3", requests.first { it.method == HttpMethod.Head }.url.parameters["level"])
    }
}
