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
    private val levelUpSignalManager: com.pixelquest.app.domain.LevelUpSignalManager,
    private val settingsRepository: com.pixelquest.app.domain.repository.SettingsRepository? = null,
    private val safetyCopy: RestoreSafetyCopy? = null
) {
    /** Whether the last restore can be undone (see RestoreSafetyCopy). */
    fun canUndoLastRestore(): Boolean = safetyCopy?.exists() == true

    /** Puts back what the phone had before the last restore. False when there's nothing to put back. */
    suspend fun undoLastRestore(): Boolean {
        val json = safetyCopy?.read() ?: return false
        val before = DataExportImport.importFromJson(json)
        if (!before.isUsable) return false
        restore(before, keepSafetyCopy = false)
        safetyCopy.clear()
        return true
    }

    suspend fun snapshot(): BackupPayload = BackupPayload(
        userProfile = db.userProfileDao().getProfile().first(),
        difficultySettings = db.difficultySettingsDao().getCurrentDifficulty().first(),
        streak = db.streakDao().getCurrentStreak().first(),
        tasks = db.taskDao().getAllTasks().first(),
        logs = db.taskCompletionLogDao().getAllLogs().first(),
        levelHistory = db.levelHistoryDao().getAllHistory().first(),
        settings = settingsRepository?.let { s ->
            BackupSettings(
                sound = s.isSoundEnabled.first(),
                crtEffect = s.isCrtEnabled.first(),
                haptics = s.isHapticsEnabled.first(),
                reduceMotion = s.isReduceMotionEnabled.first(),
                notifications = s.isNotificationsEnabled.first(),
                notificationSound = s.isNotificationSoundEnabled.first(),
                notificationVibration = s.isNotificationVibrationEnabled.first(),
                theme = s.themeMode.first().name,
                simpleMode = s.simpleModeEnabled.first()
            )
        }
    )

    /**
     * Replaces progress with the backup's in one transaction. The cloud account link and
     * leaderboard choice belong to this install, not the backup, so they are kept; otherwise a
     * restore would quietly take an opted-in player off the leaderboard at the next sync.
     */
    suspend fun restore(payload: BackupPayload) = restore(payload, keepSafetyCopy = true)

    private suspend fun restore(payload: BackupPayload, keepSafetyCopy: Boolean) {
        // What the phone has now, so this restore can be undone.
        if (keepSafetyCopy) safetyCopy?.save(DataExportImport.exportToJson(snapshot()))
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
        payload.settings?.let { applySettings(it) }

        // A level-up still waiting to be celebrated belongs to the progress just replaced (restoring
        // a level-2 backup used to greet the player with "YOU ARE NOW LEVEL 5"), as on a reset.
        levelUpSignalManager.clearPendingLevelUp()
    }

    /** The settings the backup has; one it doesn't have, or a theme this version lacks, is left alone. */
    private suspend fun applySettings(settings: BackupSettings) {
        val repo = settingsRepository ?: return
        settings.sound?.let { repo.setSoundEnabled(it) }
        settings.crtEffect?.let { repo.setCrtEnabled(it) }
        settings.haptics?.let { repo.setHapticsEnabled(it) }
        settings.reduceMotion?.let { repo.setReduceMotionEnabled(it) }
        settings.notifications?.let { repo.setNotificationsEnabled(it) }
        settings.notificationSound?.let { repo.setNotificationSoundEnabled(it) }
        settings.notificationVibration?.let { repo.setNotificationVibrationEnabled(it) }
        settings.theme
            ?.let { name -> com.pixelquest.app.ui.theme.ThemeMode.entries.firstOrNull { it.name == name } }
            ?.let { repo.setThemeMode(it) }
        settings.simpleMode?.let { repo.setSimpleModeEnabled(it) }
    }
}

