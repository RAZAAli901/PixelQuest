package com.pixelquest.app.data.repository

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.account.AccountViewModel
import com.pixelquest.app.worker.RecordingSyncScheduler
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Joining under a name another player has (names are unique, ignoring case) says so on Account,
 * instead of "Opt-in saved locally. Cloud sync will retry." and a join that could never succeed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NameTakenTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pending = PendingLeaderboardChoice(context.getSharedPreferences("name-taken", Context.MODE_PRIVATE))
    private val profiles = FakeUserProfileRepository(
        UserProfileEntity(username = "Aria", avatarId = "avatar_hero", supabaseUserId = "user-1")
    )

    /** PostgREST's answer when the unique index refuses the name. */
    private fun repository(): CloudProfileRepositoryImpl {
        val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
            httpEngine = MockEngine {
                respond(
                    """{"code":"23505","details":null,"hint":null,"message":"duplicate key value violates unique constraint \"profiles_display_name_unique\""}""",
                    HttpStatusCode.Conflict,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            install(Postgrest)
        }
        val auth = mockk<Auth> { every { currentUserOrNull() } returns mockk<UserInfo> { every { id } returns "user-1" } }
        return CloudProfileRepositoryImpl(supabase.postgrest, auth, profiles, FakeStreakRepository(StreakEntity()), pending)
    }

    @Test
    fun theRepository_putsThePhoneBackOffTheBoard_keepsTheName_andDropsTheJoin() = runBlocking {
        val result = repository().updateOptInAndSync(optIn = true, displayName = "Titan_Slayer")

        assertEquals(SupabaseResult.ServerError(409, CloudProfileRepositoryImpl.NAME_TAKEN), result)
        assertFalse(profiles.profile.value!!.leaderboardOptIn)
        assertEquals("kept to edit", "Titan_Slayer", profiles.profile.value!!.leaderboardDisplayName)
        assertNull("nothing left to retry", pending.pendingFor("user-1"))
    }

    @Test
    fun account_saysTheNameIsTaken_andQueuesNothing() {
        val scheduler = RecordingSyncScheduler()
        val viewModel = AccountViewModel(profiles, repository(), syncScheduler = scheduler)
        viewModel.onDisplayNameChanged("Titan_Slayer")

        viewModel.confirmOptIn()

        val state = runBlocking { kotlinx.coroutines.withTimeout(5_000) { viewModel.uiState.first { !it.isSyncing } } }
        assertEquals(CloudProfileRepositoryImpl.NAME_TAKEN, state.displayNameError)
        assertFalse(state.isOptedIn)
        assertEquals(0, scheduler.callCount)
    }
}
