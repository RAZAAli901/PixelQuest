package com.pixelquest.app.ui.screens.account

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * SYNC NOW says when the leaderboard stored less than the phone sent. The server adds at most one
 * streak day a day and one level per three days, so a catch-up after nights the nightly check missed
 * arrives over a few syncs; "Cloud sync successful!" next to the lower numbers read like a bug.
 */
class SyncNowHeldBackTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun syncNowWith(storedStreak: Int, storedLevel: Int, pushed: Boolean = true, leftElsewhere: Boolean = false): String? {
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Aria", avatarId = "avatar_hero", level = 4, totalXp = 3000, supabaseUserId = "acct",
                leaderboardOptIn = true, leaderboardDisplayName = "Titan_Slayer")
        )
        val cloud = object : CloudProfileRepository {
            override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String) = SupabaseResult.Success(Unit)
            override suspend fun syncProfileToCloud() = SupabaseResult.Success(Unit)
            override suspend fun pushProfileToCloud(): SupabaseResult<Boolean> {
                // What the repository does when the account left on another phone: follow, send nothing.
                if (leftElsewhere) profiles.updateLeaderboardOptIn(false)
                return SupabaseResult.Success(pushed)
            }
            override suspend fun fetchCloudProfile(userId: String): SupabaseResult<CloudProfileDto?> = SupabaseResult.Success(
                CloudProfileDto(id = "acct", displayName = "Titan_Slayer", currentStreak = storedStreak, longestStreak = storedStreak,
                    level = storedLevel, totalXp = 3000, leaderboardOptIn = true)
            )
        }
        val viewModel = AccountViewModel(
            profiles, cloud,
            streakRepository = FakeStreakRepository(StreakEntity(currentStreak = 25, longestStreak = 25))
        )

        viewModel.syncNow()
        return viewModel.uiState.value.syncMessage
    }

    @Test
    fun everythingStored_saysSuccessful() {
        assertEquals(AccountViewModel.SYNC_SUCCESSFUL, syncNowWith(storedStreak = 25, storedLevel = 4))
    }

    @Test
    fun aStreakHeldBack_saysWhatTheBoardShows_andWhy() {
        assertEquals(
            "Synced. The leaderboard shows a 19-day streak and level 4 for now: it adds at most one streak day a day " +
                "and one level per three days, so the rest follows over the next few days.",
            syncNowWith(storedStreak = 19, storedLevel = 4)
        )
    }

    @Test
    fun aLevelHeldBack_saysSoToo() {
        assertEquals(true, syncNowWith(storedStreak = 25, storedLevel = 3)?.contains("level 3 for now"))
    }

    @Test
    fun leftOnAnotherPhone_saysSo() {
        assertEquals(
            AccountViewModel.SYNC_LEFT_ELSEWHERE,
            syncNowWith(storedStreak = 25, storedLevel = 4, pushed = false, leftElsewhere = true)
        )
    }

    @Test
    fun theServerAhead_stillSaysSo() {
        assertEquals(AccountViewModel.SYNC_SKIPPED_SERVER_AHEAD, syncNowWith(storedStreak = 25, storedLevel = 4, pushed = false))
    }
}

