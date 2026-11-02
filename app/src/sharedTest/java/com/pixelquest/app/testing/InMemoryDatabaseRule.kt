package com.pixelquest.app.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.LocalDate
import java.time.LocalTime

/**
 * JUnit test rule providing a clean, thread-safe in-memory [AppDatabase] for each test run.
 * Automatically closes the database upon test completion.
 */
class InMemoryDatabaseRule : TestWatcher() {

    private var internalDb: AppDatabase? = null
    val db: AppDatabase
        get() = internalDb ?: error("InMemoryDatabaseRule not initialized.")

    lateinit var seeder: DatabaseSeeder
        private set

    public override fun starting(description: Description) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        internalDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        seeder = DatabaseSeeder(db)
    }

    public override fun finished(description: Description) {
        internalDb?.close()
        internalDb = null
    }

    suspend fun seedNewUser(): UserProfileEntity = seeder.seedNewUser()
    suspend fun seed30DayStreakUser(): UserProfileEntity = seeder.seed30DayStreakUser()
    suspend fun seedExpiredSession(): UserProfileEntity = seeder.seedExpiredSession()
}

/**
 * Helper that seeds common realistic database states into [AppDatabase].
 */
class DatabaseSeeder(private val db: AppDatabase) {

    /**
     * Seeds a newly onboarded user with initial default difficulty and 0 streak.
     */
    suspend fun seedNewUser(): UserProfileEntity {
        val profile = UserProfileEntity(
            id = 1L,
            username = "NewAdventurer",
            avatarId = "avatar_pixel_01",
            level = 1,
            totalXp = 0,
            perfectDaysTowardNextLevel = 0,
            createdAt = 1_704_067_200_000L,
            supabaseUserId = null,
            leaderboardOptIn = false,
        )
        val streak = StreakEntity(
            id = 1L,
            currentStreak = 0,
            longestStreak = 0,
            lastCompletedDate = null,
            perfectDaysCount = 0,
        )
        val difficulty = DifficultySettingsEntity(id = 1L)

        db.userProfileDao().insertProfile(profile)
        db.streakDao().insertStreak(streak)
        db.difficultySettingsDao().insertSettings(difficulty)
        return profile
    }

    /**
     * Seeds a veteran user with 30-day streak, 30 completion logs, level 5, 2500 XP.
     */
    suspend fun seed30DayStreakUser(): UserProfileEntity {
        val baseDate = LocalDate.of(2026, 1, 30)
        val profile = UserProfileEntity(
            id = 1L,
            username = "MasterKnight",
            avatarId = "avatar_hero_gold",
            level = 5,
            totalXp = 2500,
            perfectDaysTowardNextLevel = 2,
            createdAt = 1_701_388_800_000L,
            supabaseUserId = "verified-supabase-uid-12345",
            leaderboardOptIn = true,
            leaderboardDisplayName = "MasterKnight",
        )
        val streak = StreakEntity(
            id = 1L,
            currentStreak = 30,
            longestStreak = 30,
            lastCompletedDate = baseDate,
            perfectDaysCount = 30,
        )
        val difficulty = DifficultySettingsEntity(id = 1L)

        db.userProfileDao().insertProfile(profile)
        db.streakDao().insertStreak(streak)
        db.difficultySettingsDao().insertSettings(difficulty)

        val task = TaskEntity(
            id = 1L,
            name = "Morning Quest",
            description = "Daily morning routine",
            scheduledDay = baseDate.minusDays(30),
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
            isActive = true,
            createdAt = 1_701_388_800_000L,
        )
        db.taskDao().insertTask(task)

        for (i in 0 until 30) {
            val date = baseDate.minusDays(i.toLong())
            db.taskCompletionLogDao().insertLog(
                TaskCompletionLogEntity(
                    taskId = task.id,
                    completedDate = date,
                    wasCompleted = true,
                    pointsAwarded = 50,
                ),
            )
        }
        return profile
    }

    /**
     * Seeds an expired session state: user has a linked supabaseUserId but session token is expired / invalidated.
     */
    suspend fun seedExpiredSession(): UserProfileEntity {
        val profile = UserProfileEntity(
            id = 1L,
            username = "ExpiredAdventurer",
            avatarId = "avatar_pixel_01",
            level = 2,
            totalXp = 350,
            perfectDaysTowardNextLevel = 1,
            createdAt = 1_700_000_000_000L,
            supabaseUserId = "expired-auth-token-user-999",
            leaderboardOptIn = false,
            leaderboardDisplayName = null,
        )
        val streak = StreakEntity(
            id = 1L,
            currentStreak = 3,
            longestStreak = 7,
            lastCompletedDate = LocalDate.of(2026, 1, 1),
            perfectDaysCount = 2,
        )
        val difficulty = DifficultySettingsEntity(id = 1L)

        db.userProfileDao().insertProfile(profile)
        db.streakDao().insertStreak(streak)
        db.difficultySettingsDao().insertSettings(difficulty)
        return profile
    }
}
