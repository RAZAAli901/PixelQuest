package com.pixelquest.app.data.backup

import androidx.room.withTransaction
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Makes and restores backups: profile, difficulty, streak, tasks, their completion history and the level-up timeline. */
@Singleton
class BackupRestorer @Inject constructor(
    private val db: AppDatabase,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val levelUpSignalManager: com.pixelquest.app.domain.LevelUpSignalManager
) {
    suspend fun snapshot(): BackupPayload = BackupPayload(
        userProfile = db.userProfileDao().getProfile().first(),
        difficultySettings = db.difficultySettingsDao().getCurrentDifficulty().first(),
        streak = db.streakDao().getCurrentStreak().first(),
        tasks = db.taskDao().getAllTasks().first(),
        logs = db.taskCompletionLogDao().getAllLogs().first(),
        levelHistory = db.levelHistoryDao().getAllHistory().first()
    )

    /**
     * Replaces progress with the backup's in one transaction. The cloud account link and
     * leaderboard choice belong to this install, not the backup, so they are kept; otherwise a
     * restore would quietly take an opted-in player off the leaderboard at the next sync.
     */
    suspend fun restore(payload: BackupPayload) {
        val currentTasks = db.taskDao().getAllTasks().first()
        val current = db.userProfileDao().getProfile().first()

        db.withTransaction {
            payload.userProfile?.let { imported ->
                db.userProfileDao().insertProfile(
                    current?.let {
                        imported.copy(
                            createdAt = it.createdAt,
                            supabaseUserId = it.supabaseUserId,
                            leaderboardOptIn = it.leaderboardOptIn,
                            leaderboardDisplayName = it.leaderboardDisplayName
                        )
                    } ?: imported
                )
            }
            payload.difficultySettings?.let { db.difficultySettingsDao().insertSettings(it) }
            payload.streak?.let { db.streakDao().insertStreak(it) }
            if (payload.tasks.isNotEmpty()) {
                db.taskDao().deleteAll()
                payload.tasks.forEach { db.taskDao().insertTask(it) }
                val logs = payload.logs
                if (logs != null) {
                    val taskIds = payload.tasks.map { it.id }.toSet()
                    db.taskCompletionLogDao().deleteAll()
                    logs.filter { it.taskId in taskIds }.forEach { db.taskCompletionLogDao().insertLog(it) }
                } else {
                    // Older backups (before Day 31, and all of v1.1.0's) carry no history. The current
                    // history stays only for quests the backup has too, by id and name: restoring this
                    // phone's own older backup keeps it, but after a reinstall the starter quests'
                    // results used to land on whichever backup quests had their ids.
                    val sameQuests = payload.tasks.filter { restored ->
                        currentTasks.any { it.id == restored.id && it.name == restored.name }
                    }.map { it.id }
                    db.taskCompletionLogDao().deleteAllExceptFor(sameQuests)
                }
            }
            // Older backups carry no level timeline; then the current one is left as it is.
            payload.levelHistory?.let { history ->
                db.levelHistoryDao().deleteAll()
                history.forEach { db.levelHistoryDao().insertLevelHistory(it.copy(id = 0)) }
            }
        }

        if (payload.tasks.isNotEmpty()) {
            taskAlarmScheduler.cancelAllAlarms(currentTasks)
            taskAlarmScheduler.rescheduleAllAlarms(payload.tasks.filter { it.isActive })
        }
        // A level-up still waiting to be celebrated belongs to the progress just replaced (restoring
        // a level-2 backup used to greet the player with "YOU ARE NOW LEVEL 5"), as on a reset.
        levelUpSignalManager.clearPendingLevelUp()
    }
}
