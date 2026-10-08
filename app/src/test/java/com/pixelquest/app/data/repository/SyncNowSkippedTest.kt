package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.account.AccountViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * "Sync now" says what happened. When the leaderboard already had more progress for the account
 * (from another device), nothing was sent but the button reported "Cloud sync successful!".
 */
class SyncNowSkippedTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun repositoryWithServerRow(serverXp: Int, sent: MutableList<HttpMethod>): CloudProfileRepositoryImpl {
        val supabase = createSupabaseClient("https://test.supabase.co", "anon-key") {
            httpEngine = MockEngine { request ->
                sent += request.method
                val row = """[{"id":"user-1","display_name":"Aria","current_streak":3,"longest_streak":3,"level":2,"total_xp":$serverXp,"leaderboard_opt_in":true}]"""
                respond(if (request.method == HttpMethod.Get) row else "[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
            install(Postgrest)
        }
        val auth = mockk<Auth> { every { currentUserOrNull() } returns mockk<UserInfo> { every { id } returns "user-1" } }
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Aria", avatarId = "avatar_hero", level = 2, totalXp = 500, supabaseUserId = "user-1", leaderboardOptIn = true)
        )
        return CloudProfileRepositoryImpl(supabase.postgrest, auth, profiles, FakeStreakRepository(StreakEntity(currentStreak = 3, longestStreak = 3)))
    }

    @Test
    fun whenTheServerIsAhead_nothingIsSent_andThePushSaysSo() = runBlocking {
        val sent = mutableListOf<HttpMethod>()

        val result = repositoryWithServerRow(serverXp = 9000, sent = sent).pushProfileToCloud()

        assertEquals(SupabaseResult.Success(false), result)
        assertEquals(listOf(HttpMethod.Get), sent)
    }

    @Test
    fun whenThisDeviceIsAhead_theRowIsSent() = runBlocking {
        val sent = mutableListOf<HttpMethod>()

        val result = repositoryWithServerRow(serverXp = 100, sent = sent).pushProfileToCloud()

        assertEquals(SupabaseResult.Success(true), result)
        assertEquals(listOf(HttpMethod.Get, HttpMethod.Post), sent)
    }

    private class Pushing(private val sent: Boolean) : CloudProfileRepository {
        override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String) = SupabaseResult.Success(Unit)
        override suspend fun syncProfileToCloud() = SupabaseResult.Success(Unit)
        override suspend fun pushProfileToCloud() = SupabaseResult.Success(sent)
    }

    private fun accountAfterSyncNow(sent: Boolean): AccountViewModel {
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Aria", avatarId = "avatar_hero", supabaseUserId = "user-1", leaderboardOptIn = true)
        )
        return AccountViewModel(profiles, Pushing(sent)).also { it.syncNow() }
    }

    @Test
    fun syncNow_whenNothingWasSent_doesNotClaimSuccess() {
        val state = accountAfterSyncNow(sent = false).uiState.value

        assertEquals(AccountViewModel.SYNC_SKIPPED_SERVER_AHEAD, state.syncMessage)
        assertFalse("Not shown as successful", state.syncMessage!!.contains("successful", ignoreCase = true))
        assertFalse(state.isSyncFailed)
        assertNull("No new last-synced time", state.lastSyncTime)
    }

    @Test
    fun syncNow_whenTheRowWasSent_reportsSuccess() {
        val state = accountAfterSyncNow(sent = true).uiState.value

        assertEquals(AccountViewModel.SYNC_SUCCESSFUL, state.syncMessage)
        assertTrue(state.lastSyncTime != null)
    }
}
