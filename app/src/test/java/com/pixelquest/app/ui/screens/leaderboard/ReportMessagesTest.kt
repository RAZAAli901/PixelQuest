package com.pixelquest.app.ui.screens.leaderboard

import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Reporting a player says what happened. The server takes one report per player a day, and a repeat
 * used to read as "check network connection", as did any answer from the server.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportMessagesTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun reportAnswered(answer: SupabaseResult<Unit>): LeaderboardUiState {
        val board = object : LeaderboardRepository {
            override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getCurrentUserRank(sortMode: LeaderboardSortMode, userId: String?): SupabaseResult<UserLeaderboardRank?> =
                SupabaseResult.Success(null)
            override suspend fun reportProfile(reportedProfileId: String, reason: String) = answer
        }
        val auth = object : com.pixelquest.app.auth.AuthRepository by FakeAuthRepository() {
            override val currentUser = kotlinx.coroutines.flow.MutableStateFlow<AuthUser?>(AuthUser("me", "me@pixelquest.test", null))
        }
        val viewModel = LeaderboardViewModel(board, auth, FakeUserProfileRepository())

        viewModel.reportProfile("someone-else")
        return viewModel.uiState.value
    }

    @Test
    fun filed_thanksThePlayer() {
        assertEquals("Report submitted. Thank you for keeping PixelQuest safe.", reportAnswered(SupabaseResult.Success(Unit)).reportMessage)
    }

    @Test
    fun alreadyFiledToday_saysSo_notANetworkProblem() {
        val state = reportAnswered(SupabaseResult.ServerError(409, "duplicate key value"))
        assertEquals(LeaderboardViewModel.REPORT_ALREADY_FILED, state.reportMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun anotherServerAnswer_isNotBlamedOnTheNetwork() {
        assertEquals(
            "Could not submit the report. Please try again later.",
            reportAnswered(SupabaseResult.ServerError(500, "boom")).errorMessage
        )
    }

    @Test
    fun offline_stillSaysCheckTheConnection() {
        assertEquals(
            "Could not submit report: check network connection.",
            reportAnswered(SupabaseResult.NetworkError(java.io.IOException("offline"))).errorMessage
        )
    }
}
