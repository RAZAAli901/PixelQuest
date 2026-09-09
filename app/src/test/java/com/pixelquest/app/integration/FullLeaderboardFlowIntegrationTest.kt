package com.pixelquest.app.integration

import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardAuthState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardTab
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test covering the full Leaderboard fetch, tab switching,
 * pagination, current user rank projection, and spectator/sign-in flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FullLeaderboardFlowIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private val seedProfiles = listOf(
        CloudProfileDto(id = "user-1", displayName = "DragonRider", currentStreak = 30, longestStreak = 45, level = 12, totalXp = 6000, leaderboardOptIn = true),
        CloudProfileDto(id = "user-2", displayName = "ShadowMage", currentStreak = 25, longestStreak = 30, level = 15, totalXp = 8000, leaderboardOptIn = true),
        CloudProfileDto(id = "user-3", displayName = "PixelKnight", currentStreak = 18, longestStreak = 20, level = 10, totalXp = 4500, leaderboardOptIn = true),
        CloudProfileDto(id = "user-4", displayName = "RogueArcher", currentStreak = 12, longestStreak = 15, level = 15, totalXp = 7500, leaderboardOptIn = true),
        CloudProfileDto(id = "user-5", displayName = "IronWarrior", currentStreak = 8, longestStreak = 10, level = 8, totalXp = 3000, leaderboardOptIn = true),
        CloudProfileDto(id = "user-6", displayName = "NoviceHero", currentStreak = 3, longestStreak = 5, level = 3, totalXp = 600, leaderboardOptIn = true)
    )

    private val authUserFlow = MutableStateFlow<AuthUser?>(
        AuthUser(id = "user-3", email = "pixelknight@test.com", displayName = "PixelKnight")
    )

    private val userProfileFlow = MutableStateFlow<UserProfileEntity?>(
        UserProfileEntity(
            id = 1,
            username = "PixelKnight",
            avatarId = "1",
            level = 10,
            totalXp = 4500,
            supabaseUserId = "user-3",
            leaderboardOptIn = true,
            leaderboardDisplayName = "PixelKnight"
        )
    )

    private lateinit var leaderboardRepository: LeaderboardRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var viewModel: LeaderboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        authRepository = object : AuthRepository {
            override val currentUser: Flow<AuthUser?> = authUserFlow
            override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?): SupabaseResult<AuthUser> =
                SupabaseResult.Success(authUserFlow.value!!)
            override suspend fun signOut(): SupabaseResult<Unit> {
                authUserFlow.value = null
                return SupabaseResult.Success(Unit)
            }
            override suspend fun getInitialUser(): AuthUser? = authUserFlow.value
        }

        userProfileRepository = object : UserProfileRepository {
            override fun getProfile(): Flow<UserProfileEntity?> = userProfileFlow
            override suspend fun insertProfile(profile: UserProfileEntity) { userProfileFlow.value = profile }
            override suspend fun updateProfile(profile: UserProfileEntity) { userProfileFlow.value = profile }
            override suspend fun performLevelUp(): UserProfileEntity? = userProfileFlow.value
            override suspend fun updateSupabaseUserId(userId: String?) {
                userProfileFlow.value = userProfileFlow.value?.copy(supabaseUserId = userId)
            }
            override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
                userProfileFlow.value = userProfileFlow.value?.copy(leaderboardOptIn = optIn, leaderboardDisplayName = displayName)
            }
            override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
                userProfileFlow.value = userProfileFlow.value?.copy(leaderboardOptIn = optIn)
            }
        }

        leaderboardRepository = object : LeaderboardRepository {
            override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> =
                SupabaseResult.Success(seedProfiles)

            override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
                val sorted = seedProfiles
                    .sortedWith(compareByDescending<CloudProfileDto> { it.currentStreak }.thenByDescending { it.longestStreak })
                    .drop(offset.toInt())
                    .take(limit.toInt())
                return SupabaseResult.Success(sorted)
            }

            override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
                val sorted = seedProfiles
                    .sortedWith(compareByDescending<CloudProfileDto> { it.level }.thenByDescending { it.totalXp })
                    .drop(offset.toInt())
                    .take(limit.toInt())
                return SupabaseResult.Success(sorted)
            }

            override suspend fun getCurrentUserRank(
                sortMode: LeaderboardSortMode,
                userId: String?
            ): SupabaseResult<UserLeaderboardRank?> {
                val target = seedProfiles.firstOrNull { it.id == userId } ?: return SupabaseResult.Success(null)
                val rank = when (sortMode) {
                    LeaderboardSortMode.STREAK -> seedProfiles.count {
                        it.currentStreak > target.currentStreak ||
                            (it.currentStreak == target.currentStreak && it.longestStreak > target.longestStreak)
                    } + 1
                    LeaderboardSortMode.LEVEL -> seedProfiles.count {
                        it.level > target.level ||
                            (it.level == target.level && it.totalXp > target.totalXp)
                    } + 1
                }
                return SupabaseResult.Success(UserLeaderboardRank(rank = rank, profile = target))
            }
        }

        viewModel = LeaderboardViewModel(
            leaderboardRepository = leaderboardRepository,
            authRepository = authRepository,
            userProfileRepository = userProfileRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun fullFlow_initialLoad_sortsTopStreaksAndCalculatesCurrentUserRank() = runTest(testDispatcher) {
        viewModel.loadInitialData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.lastUpdatedTimestamp)

        // 1. Verify Top Streaks ordering: DragonRider (30) > ShadowMage (25) > PixelKnight (18)
        assertEquals(6, state.streakEntries.size)
        assertEquals("user-1", state.streakEntries[0].id)
        assertEquals("DragonRider", state.streakEntries[0].displayName)
        assertEquals(30, state.streakEntries[0].currentStreak)

        assertEquals("user-2", state.streakEntries[1].id)
        assertEquals(25, state.streakEntries[1].currentStreak)

        assertEquals("user-3", state.streakEntries[2].id)
        assertEquals(18, state.streakEntries[2].currentStreak)

        // 2. Verify current user rank (user-3) is calculated correctly as Rank 3
        assertNotNull(state.currentUserRank)
        assertEquals(3, state.currentUserRank?.rank)
        assertEquals("user-3", state.currentUserRank?.profile?.id)
        assertEquals("PixelKnight", state.currentUserRank?.profile?.displayName)
    }

    @Test
    fun fullFlow_tabSwitchToTopLevels_loadsLevelRankingWithTiebreaker() = runTest(testDispatcher) {
        viewModel.loadInitialData()
        advanceUntilIdle()

        // Switch tab to TOP_LEVELS
        viewModel.selectTab(LeaderboardTab.TOP_LEVELS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LeaderboardTab.TOP_LEVELS, state.selectedTab)

        // ShadowMage (lvl 15, 8000 XP) > RogueArcher (lvl 15, 7500 XP) > DragonRider (lvl 12, 6000 XP) > PixelKnight (lvl 10)
        assertEquals(6, state.levelEntries.size)
        assertEquals("user-2", state.levelEntries[0].id)
        assertEquals("ShadowMage", state.levelEntries[0].displayName)
        assertEquals(15, state.levelEntries[0].level)
        assertEquals(8000, state.levelEntries[0].totalXp)

        assertEquals("user-4", state.levelEntries[1].id)
        assertEquals("RogueArcher", state.levelEntries[1].displayName)
        assertEquals(15, state.levelEntries[1].level)
        assertEquals(7500, state.levelEntries[1].totalXp)

        assertEquals("user-1", state.levelEntries[2].id)
        assertEquals(12, state.levelEntries[2].level)

        // PixelKnight is rank 4 on levels
        assertNotNull(state.currentUserRank)
        assertEquals(4, state.currentUserRank?.rank)
    }

    @Test
    fun fullFlow_spectatorMode_displaysLeaderboardWithoutPersonalRank() = runTest(testDispatcher) {
        // User opts out into spectator read-only mode
        userProfileFlow.value = userProfileFlow.value?.copy(leaderboardOptIn = false)
        advanceUntilIdle()

        val authState = viewModel.uiState.value.authState
        assertTrue(authState is LeaderboardAuthState.SignedInReadOnly)

        viewModel.loadInitialData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Leaderboard rows are still visible to spectator
        assertEquals(6, state.streakEntries.size)
        // But spectator has no personal ranking card
        assertNull(state.currentUserRank)
    }

    @Test
    fun fullFlow_signOut_resetsLeaderboardToLockedState() = runTest(testDispatcher) {
        viewModel.loadInitialData()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.streakEntries.isNotEmpty())

        // User signs out
        authUserFlow.value = null
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.authState is LeaderboardAuthState.NotSignedIn)
        assertTrue(state.streakEntries.isEmpty())
        assertTrue(state.levelEntries.isEmpty())
        assertNull(state.currentUserRank)
    }
}
