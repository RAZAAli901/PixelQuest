package com.pixelquest.app.ui

import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.ui.search.QuranSearchViewModel
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardTab
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardViewModel
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SavedStateHandlePersistenceTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun taskFormViewModel_survivesProcessDeath_restoringFields() {
        val handle = SavedStateHandle()
        val repo = mockk<TaskRepository>(relaxed = true)
        val scheduler = mockk<TaskAlarmScheduler>(relaxed = true)

        val vm1 = TaskFormViewModel(repo, scheduler, handle)
        vm1.onNameChanged("Dragon Slayer")
        vm1.onTimeSelected(LocalTime.of(14, 30))
        vm1.onRecurrenceSelected(RecurrenceType.DAILY)
        vm1.onCategorySelected(TaskCategory.LEARNING)
        vm1.onReminderEnabledChanged(false)

        // Simulate process death by creating a new VM instance with the same SavedStateHandle
        val vm2 = TaskFormViewModel(repo, scheduler, handle)
        val state = vm2.formState.value

        assertEquals("Dragon Slayer", state.name)
        assertEquals(LocalTime.of(14, 30), state.scheduledTime)
        assertEquals(RecurrenceType.DAILY, state.recurrenceType)
        assertEquals(TaskCategory.LEARNING, state.category)
        assertEquals(false, state.reminderEnabled)
    }

    @Test
    fun quranSearchViewModel_survivesProcessDeath_restoringQuery() {
        val handle = SavedStateHandle()
        val repo = mockk<QuranRepository>(relaxed = true)

        val vm1 = QuranSearchViewModel(repo, handle)
        vm1.search("Al-Fatihah")
        assertEquals("Al-Fatihah", vm1.query.value)

        // Simulate process death
        val vm2 = QuranSearchViewModel(repo, handle)
        assertEquals("Al-Fatihah", vm2.query.value)
    }

    @Test
    fun leaderboardViewModel_survivesProcessDeath_restoringSelectedTab() {
        val handle = SavedStateHandle()
        val board = object : LeaderboardRepository {
            override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> = SupabaseResult.Success(emptyList())
            override suspend fun getCurrentUserRank(sortMode: LeaderboardSortMode, userId: String?): SupabaseResult<UserLeaderboardRank?> =
                SupabaseResult.Success(null)
            override suspend fun reportProfile(reportedProfileId: String, reason: String): SupabaseResult<Unit> =
                SupabaseResult.Success(Unit)
        }
        val auth = object : com.pixelquest.app.auth.AuthRepository by FakeAuthRepository() {
            override val currentUser = MutableStateFlow<AuthUser?>(null)
        }
        val profiles = FakeUserProfileRepository()

        val vm1 = LeaderboardViewModel(board, auth, profiles, handle)
        vm1.selectTab(LeaderboardTab.TOP_LEVELS)
        assertEquals(LeaderboardTab.TOP_LEVELS, vm1.uiState.value.selectedTab)

        // Simulate process death
        val vm2 = LeaderboardViewModel(board, auth, profiles, handle)
        assertEquals(LeaderboardTab.TOP_LEVELS, vm2.uiState.value.selectedTab)
    }
}
