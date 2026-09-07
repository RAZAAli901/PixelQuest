package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.account.AccountViewModel
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

class FakeCloudProfileRepository : CloudProfileRepository {
    var lastOptIn: Boolean? = null
    var lastDisplayName: String? = null
    var syncResult: SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        lastOptIn = optIn
        lastDisplayName = displayName
        return syncResult
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> = syncResult
}

class FakeUserProfileRepositoryForOptIn : UserProfileRepository {
    val profileFlow = MutableStateFlow(
        UserProfileEntity(id = 1, username = "LocalHero", avatarId = "avatar_1")
    )

    override fun getProfile(): Flow<UserProfileEntity?> = profileFlow
    override suspend fun insertProfile(profile: UserProfileEntity) { profileFlow.value = profile }
    override suspend fun updateProfile(profile: UserProfileEntity) { profileFlow.value = profile }
    override suspend fun performLevelUp(): UserProfileEntity? = null
    override suspend fun updateSupabaseUserId(userId: String?) {
        profileFlow.value = profileFlow.value.copy(supabaseUserId = userId)
    }
    override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) {
        profileFlow.value = profileFlow.value.copy(
            leaderboardOptIn = optIn,
            leaderboardDisplayName = displayName
        )
    }
    override suspend fun updateLeaderboardOptIn(optIn: Boolean) {
        profileFlow.value = profileFlow.value.copy(leaderboardOptIn = optIn)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardOptInValidationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeUserProfileRepo: FakeUserProfileRepositoryForOptIn
    private lateinit var fakeCloudProfileRepo: FakeCloudProfileRepository
    private lateinit var viewModel: AccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUserProfileRepo = FakeUserProfileRepositoryForOptIn()
        fakeCloudProfileRepo = FakeCloudProfileRepository()
        viewModel = AccountViewModel(fakeUserProfileRepo, fakeCloudProfileRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validateDisplayName_rejectsEmptyOrBlank() {
        assertNotNull(AccountViewModel.validateDisplayName(""))
        assertNotNull(AccountViewModel.validateDisplayName("   "))
    }

    @Test
    fun validateDisplayName_rejectsTooShortOrTooLong() {
        assertNotNull(AccountViewModel.validateDisplayName("ab"))
        assertNotNull(AccountViewModel.validateDisplayName("a_very_long_display_name_exceeding_twenty"))
    }

    @Test
    fun validateDisplayName_rejectsSpecialCharactersAndSpaces() {
        assertNotNull(AccountViewModel.validateDisplayName("Hero Space"))
        assertNotNull(AccountViewModel.validateDisplayName("Hero@123"))
        assertNotNull(AccountViewModel.validateDisplayName("Hero!"))
    }

    @Test
    fun validateDisplayName_acceptsValidAlphanumericAndUnderscores() {
        assertNull(AccountViewModel.validateDisplayName("PixelKnight"))
        assertNull(AccountViewModel.validateDisplayName("Quest_99"))
        assertNull(AccountViewModel.validateDisplayName("hero_123_abc"))
    }

    @Test
    fun optInToggle_whenNameInvalid_blocksConfirmDialog() = runTest {
        advanceUntilIdle()
        viewModel.onDisplayNameChanged("hi")
        viewModel.onOptInToggleClicked(true)
        advanceUntilIdle()

        assertFalse("Confirmation dialog should NOT show on invalid name", viewModel.uiState.value.showConfirmDialog)
        assertNotNull("Error message must be populated", viewModel.uiState.value.displayNameError)
    }

    @Test
    fun optInToggle_whenNameValid_opensConfirmDialog() = runTest {
        advanceUntilIdle()
        viewModel.onDisplayNameChanged("PixelHero_42")
        viewModel.onOptInToggleClicked(true)
        advanceUntilIdle()

        assertTrue("Confirmation dialog should show on valid name", viewModel.uiState.value.showConfirmDialog)
        assertNull("Error message should be null", viewModel.uiState.value.displayNameError)
    }

    @Test
    fun confirmOptIn_writesToCloudAndLocal_andUpdatesState() = runTest {
        advanceUntilIdle()
        viewModel.onDisplayNameChanged("PixelHero_42")
        viewModel.confirmOptIn()
        advanceUntilIdle()

        assertTrue("Opt-in flag should be true", viewModel.uiState.value.isOptedIn)
        assertEquals(true, fakeCloudProfileRepo.lastOptIn)
        assertEquals("PixelHero_42", fakeCloudProfileRepo.lastDisplayName)
    }

    @Test
    fun optOut_updatesCloudAndLocal_setsOptedInFalse() = runTest {
        advanceUntilIdle()
        viewModel.onDisplayNameChanged("PixelHero_42")
        viewModel.confirmOptIn()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isOptedIn)

        viewModel.optOut()
        advanceUntilIdle()

        assertFalse("Opt-in flag should be false after optOut", viewModel.uiState.value.isOptedIn)
        assertEquals(false, fakeCloudProfileRepo.lastOptIn)
    }
}
