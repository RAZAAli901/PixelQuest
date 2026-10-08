package com.pixelquest.app.ui.screens.leaderboard

import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.PlayersAroundYou
import com.pixelquest.app.data.repository.RankedProfile
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Leaderboard pages, refreshes and tab switches can finish in any order. A page that repeated a hero
 * already shown crashed the list (two rows with the same key), a page from before a refresh was
 * appended to the new list, and a load or "around you" result for the tab the player had just left
 * was applied to the one on screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardPagingRaceTest {

    private val dispatcher = StandardTestDispatcher()

    private class Call(val tab: LeaderboardTab, val offset: Long) {
        val reply = CompletableDeferred<SupabaseResult<List<CloudProfileDto>>>()
    }

    private class AroundCall(val sortMode: LeaderboardSortMode) {
        val reply = CompletableDeferred<SupabaseResult<PlayersAroundYou?>>()
    }

    /** Every request waits until the test answers it, so the test decides the order. */
    private class ControlledLeaderboard : LeaderboardRepository {
        val calls = mutableListOf<Call>()
        val aroundCalls = mutableListOf<AroundCall>()

        override suspend fun getProfiles() = SupabaseResult.Success(emptyList<CloudProfileDto>())
        override suspend fun getTopByStreak(limit: Long, offset: Long) =
            Call(LeaderboardTab.TOP_STREAKS, offset).also { calls += it }.reply.await()
        override suspend fun getTopByLevel(limit: Long, offset: Long) =
            Call(LeaderboardTab.TOP_LEVELS, offset).also { calls += it }.reply.await()
        override suspend fun getCurrentUserRank(sortMode: LeaderboardSortMode, userId: String?) =
            SupabaseResult.Success<UserLeaderboardRank?>(null)
        override suspend fun getPlayersAroundYou(sortMode: LeaderboardSortMode, userId: String?, radius: Int) =
            AroundCall(sortMode).also { aroundCalls += it }.reply.await()

        fun pending(tab: LeaderboardTab, offset: Long) = calls.last { it.tab == tab && it.offset == offset && !it.reply.isCompleted }
    }

    private class SignedIn : AuthRepository {
        override val currentUser: Flow<AuthUser?> = MutableStateFlow(AuthUser(id = "me", email = "me@pixelquest.test", displayName = "Me"))
        override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?) = SupabaseResult.Success(AuthUser("me", null, null))
        override suspend fun signOut() = SupabaseResult.Success(Unit)
        override suspend fun getInitialUser(): AuthUser? = null
    }

    private val repo = ControlledLeaderboard()
    private lateinit var viewModel: LeaderboardViewModel

    private fun heroes(range: IntRange, prefix: String = "hero") =
        range.map { CloudProfileDto(id = "$prefix-$it", displayName = "Hero $it") }

    private fun ok(list: List<CloudProfileDto>) = SupabaseResult.Success(list)

    private fun around(you: String) = SupabaseResult.Success<PlayersAroundYou?>(
        PlayersAroundYou(
            you = UserLeaderboardRank(rank = 4, profile = CloudProfileDto(id = you, displayName = you)),
            entries = listOf(RankedProfile(4, CloudProfileDto(id = you, displayName = you)))
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Me", avatarId = "avatar_hero", supabaseUserId = "me", leaderboardOptIn = true)
        )
        viewModel = LeaderboardViewModel(repo, SignedIn(), profiles)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Signs in (which loads the streak tab) and answers the first page with heroes 1..20. */
    private fun kotlinx.coroutines.test.TestScope.firstPageLoaded() {
        advanceUntilIdle()
        repo.pending(LeaderboardTab.TOP_STREAKS, 0).reply.complete(ok(heroes(1..20)))
        advanceUntilIdle()
    }

    @Test
    fun aNextPageThatRepeatsAHero_isShownOnce() = runTest(dispatcher) {
        firstPageLoaded()

        viewModel.loadMore()
        advanceUntilIdle()
        // Someone climbed into the top 20 meanwhile, so hero 20 slid to the start of page two.
        repo.pending(LeaderboardTab.TOP_STREAKS, 20).reply.complete(ok(heroes(20..39)))
        advanceUntilIdle()

        val ids = viewModel.uiState.value.streakEntries.map { it.id }
        assertEquals(39, ids.size)
        assertEquals("Every row key is unique", ids.size, ids.toSet().size)
    }

    @Test
    fun aPageFromBeforeARefresh_isNotAppendedToTheNewList() = runTest(dispatcher) {
        firstPageLoaded()

        viewModel.loadMore()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()
        repo.pending(LeaderboardTab.TOP_STREAKS, 0).reply.complete(ok(heroes(1..20, "fresh")))
        advanceUntilIdle()
        repo.pending(LeaderboardTab.TOP_STREAKS, 20).reply.complete(ok(heroes(21..40)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(heroes(1..20, "fresh").map { it.id }, state.streakEntries.map { it.id })
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun anOlderRefreshFinishingLast_doesNotReplaceTheNewerOne() = runTest(dispatcher) {
        firstPageLoaded()

        viewModel.refresh()
        advanceUntilIdle()
        val older = repo.pending(LeaderboardTab.TOP_STREAKS, 0)
        viewModel.refresh()
        advanceUntilIdle()
        val newer = repo.pending(LeaderboardTab.TOP_STREAKS, 0)

        newer.reply.complete(ok(heroes(1..20, "newer")))
        advanceUntilIdle()
        older.reply.complete(ok(heroes(1..20, "older")))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.streakEntries.all { it.id.startsWith("newer") })
    }

    @Test
    fun theOtherTabsLoadFinishing_leavesThisTabsSpinnerAndLoadMoreAlone() = runTest(dispatcher) {
        firstPageLoaded()
        repo.aroundCalls.forEach { it.reply.complete(around("me")) }

        viewModel.selectTab(LeaderboardTab.TOP_LEVELS) // empty, so it loads
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.selectTab(LeaderboardTab.TOP_STREAKS) // back before levels arrive
        advanceUntilIdle()
        assertFalse("Streaks are loaded, so no spinner", viewModel.uiState.value.isLoading)

        // Only 5 heroes on the level board: no more pages there. Streaks can still load more.
        repo.pending(LeaderboardTab.TOP_LEVELS, 0).reply.complete(ok(heroes(1..5, "lvl")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LeaderboardTab.TOP_STREAKS, state.selectedTab)
        assertFalse(state.isLoading)
        assertTrue("LOAD MORE stays on the streak tab", state.canLoadMore)
        assertEquals(5, state.levelEntries.size)

        viewModel.selectTab(LeaderboardTab.TOP_LEVELS)
        assertFalse("but not on the level tab", viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun aroundYouForTheTabJustLeft_isNotShownOnTheNewOne() = runTest(dispatcher) {
        firstPageLoaded()
        val streakAround = repo.aroundCalls.single()
        assertEquals(LeaderboardSortMode.STREAK, streakAround.sortMode)

        viewModel.selectTab(LeaderboardTab.TOP_LEVELS)
        advanceUntilIdle()
        repo.pending(LeaderboardTab.TOP_LEVELS, 0).reply.complete(ok(heroes(1..20, "lvl")))
        advanceUntilIdle()
        val levelAround = repo.aroundCalls.last()
        assertEquals(LeaderboardSortMode.LEVEL, levelAround.sortMode)

        levelAround.reply.complete(around("me-by-level"))
        advanceUntilIdle()
        streakAround.reply.complete(around("me-by-streak")) // the slow one arrives last
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("me-by-level", state.currentUserRank?.profile?.id)
        assertEquals(listOf("me-by-level"), state.aroundYouEntries.map { it.profile.id })
        assertFalse(state.isLoadingAroundYou)
    }
}
