package com.pixelquest.app.data.local

import androidx.room.withTransaction
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.AvatarCatalog
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RESET ALL PROGRESS. In one transaction it deletes every task, completion log, level-history
 * entry and cached AI insight, and resets the streak, XP, level and difficulty to a new player's.
 * Reminders and a pending level-up celebration are cleared too. Kept: the cloud account link and
 * leaderboard choice, and app settings such as theme, sound and notifications.
 */
@Singleton
class ProgressReset @Inject constructor(
    private val db: AppDatabase,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val levelUpSignalManager: LevelUpSignalManager
) {
    suspend fun resetAll() {
        val tasks = db.taskDao().getAllTasks().first()
        taskAlarmScheduler.cancelAllAlarms(tasks)
        tasks.forEach { taskAlarmScheduler.clearReminder(it.id) }
        val profile = db.userProfileDao().getProfile().first()

        db.withTransaction {
            db.taskDao().deleteAll()
            db.taskCompletionLogDao().deleteAll()
            db.levelHistoryDao().deleteAll()
            db.insightCacheDao().clearCache()
            db.streakDao().insertStreak(StreakEntity())
            db.userProfileDao().insertProfile(
                UserProfileEntity(
                    username = "PixelHero",
                    avatarId = AvatarCatalog.DEFAULT_AVATAR_ID,
                    supabaseUserId = profile?.supabaseUserId,
                    leaderboardOptIn = profile?.leaderboardOptIn ?: false,
                    leaderboardDisplayName = profile?.leaderboardDisplayName
                )
            )
            db.difficultySettingsDao().insertSettings(DifficultySettingsEntity())
        }
        levelUpSignalManager.clearPendingLevelUp()
    }
}
