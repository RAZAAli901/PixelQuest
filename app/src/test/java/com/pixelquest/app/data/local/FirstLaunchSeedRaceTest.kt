package com.pixelquest.app.data.local

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.di.DatabaseModule
import com.pixelquest.app.domain.model.DifficultyLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * On a fresh install with no network, onboarding's save is the first thing to open the database.
 * The default data is seeded in the background when the database is created, and used to REPLACE
 * the profile and difficulty the player had just chosen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FirstLaunchSeedRaceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context.deleteDatabase("pixelquest.db")
        db = DatabaseModule.provideAppDatabase(context) { db }
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase("pixelquest.db")
    }

    /** Waits until the background seed has added the starter quests. */
    private suspend fun awaitSeed() {
        repeat(100) {
            if (db.taskDao().getAllTasks().first().size >= 3) return
            Thread.sleep(50)
        }
        error("The seed never ran")
    }

    @Test
    fun onboardingsChoices_surviveTheFirstLaunchSeed() = runBlocking {
        // What OnboardingViewModel.completeOnboarding saves; here it is the first database access.
        db.userProfileDao().insertProfile(UserProfileEntity(id = 1, username = "Aria", avatarId = "avatar_mage"))
        db.difficultySettingsDao().insertSettings(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.HARD, perfectDayThreshold = 0.9f, daysRequiredPerLevel = 14)
        )

        awaitSeed()
        Thread.sleep(200) // let anything still queued finish

        val profile = db.userProfileDao().getProfile().first()!!
        assertEquals("Aria", profile.username)
        assertEquals("avatar_mage", profile.avatarId)
        assertEquals(DifficultyLevel.HARD, db.difficultySettingsDao().getCurrentDifficulty().first()!!.difficultyLevel)
    }

    @Test
    fun aFreshDatabase_isStillSeeded() = runBlocking {
        db.taskDao().getAllTasks().first() // any read opens (and creates) the database
        awaitSeed()

        assertEquals("PixelHero", db.userProfileDao().getProfile().first()!!.username)
        assertEquals(DifficultyLevel.MEDIUM, db.difficultySettingsDao().getCurrentDifficulty().first()!!.difficultyLevel)
        assertEquals(0, db.streakDao().getCurrentStreak().first()!!.currentStreak)
    }
}
