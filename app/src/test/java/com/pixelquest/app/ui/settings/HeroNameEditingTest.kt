package com.pixelquest.app.ui.settings

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.ProgressReset
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.HeroName
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Settings' hero name used to be written to the database on every keystroke, without validation.
 * Typing now edits a draft that SAVE NAME stores, with the same rules as onboarding.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HeroNameEditingTest {

    private lateinit var db: AppDatabase
    private lateinit var profiles: FakeUserProfileRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        profiles = FakeUserProfileRepository(UserProfileEntity(id = 1, username = "Aria", avatarId = "avatar_hero"))
        val scheduler = TaskAlarmScheduler(context)
        viewModel = SettingsViewModel(
            FakeSettingsRepository(),
            profiles,
            FakeDifficultySettingsRepository(),
            FakeTaskRepository(),
            scheduler,
            ProgressReset(db, scheduler, LevelUpSignalManager(context)),
            BackupRestorer(db, scheduler)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun typing_doesNotSave_untilSaveName() {
        viewModel.onNameDraftChanged("A")
        viewModel.onNameDraftChanged("Ar")
        viewModel.onNameDraftChanged("Arthur")
        assertEquals("Aria", profiles.profile.value?.username)

        assertTrue(viewModel.saveName())

        assertEquals("Arthur", profiles.profile.value?.username)
        assertNull("The field shows the saved name again", viewModel.nameDraft.value)
    }

    @Test
    fun aBlankName_isNotSaved_andTheDraftStays() {
        viewModel.onNameDraftChanged("   ")

        assertFalse(viewModel.saveName())
        assertEquals("Aria", profiles.profile.value?.username)
        assertEquals("   ", viewModel.nameDraft.value)
    }

    @Test
    fun typing_isLimitedTo20Characters_andTheSavedNameIsTrimmed() {
        viewModel.onNameDraftChanged("  " + "x".repeat(30))
        assertEquals(HeroName.MAX_LENGTH, viewModel.nameDraft.value!!.length)

        viewModel.onNameDraftChanged("  Sir Lance  ")
        viewModel.saveName()
        assertEquals("Sir Lance", profiles.profile.value?.username)
    }

    @Test
    fun theRule_matchesOnboarding() {
        assertEquals("Name cannot be blank", HeroName.error(""))
        assertEquals("Name cannot be blank", HeroName.error("  "))
        assertNull(HeroName.error("Aria"))
        assertEquals("Name must be 20 characters or less", HeroName.error("x".repeat(21)))
    }
}
