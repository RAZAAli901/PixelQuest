package com.pixelquest.app.data.backup

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.Priority
import com.pixelquest.app.domain.model.RecurrenceType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

data class BackupPayload(
    val userProfile: UserProfileEntity?,
    val difficultySettings: DifficultySettingsEntity?,
    val streak: StreakEntity?,
    val tasks: List<TaskEntity>
)

object DataExportImport {

    fun exportToJson(payload: BackupPayload): String {
        val root = JSONObject()

        payload.userProfile?.let { profile ->
            val pObj = JSONObject().apply {
                put("id", profile.id)
                put("username", profile.username)
                put("avatarId", profile.avatarId)
                put("level", profile.level)
                put("totalXp", profile.totalXp)
                put("perfectDaysTowardNextLevel", profile.perfectDaysTowardNextLevel)
            }
            root.put("userProfile", pObj)
        }

        payload.difficultySettings?.let { diff ->
            val dObj = JSONObject().apply {
                put("id", diff.id)
                put("difficultyLevel", diff.difficultyLevel.name)
                put("perfectDayThreshold", diff.perfectDayThreshold.toDouble())
                put("daysRequiredPerLevel", diff.daysRequiredPerLevel)
            }
            root.put("difficultySettings", dObj)
        }

        payload.streak?.let { streak ->
            val sObj = JSONObject().apply {
                put("id", streak.id)
                put("currentStreak", streak.currentStreak)
                put("longestStreak", streak.longestStreak)
                put("lastCompletedDate", streak.lastCompletedDate.toString())
            }
            root.put("streak", sObj)
        }

        val tasksArray = JSONArray()
        payload.tasks.forEach { task ->
            val tObj = JSONObject().apply {
                put("id", task.id)
                put("name", task.name)
                put("description", task.description)
                put("scheduledTime", task.scheduledTime.toString())
                put("scheduledDay", task.scheduledDay.toString())
                put("recurrenceType", task.recurrenceType.name)
                put("priority", task.priority.name)
                put("difficulty", task.difficulty.name)
                put("isCompleted", task.isCompleted)
            }
            tasksArray.put(tObj)
        }
        root.put("tasks", tasksArray)

        return root.toString(2)
    }

    fun importFromJson(jsonString: String): BackupPayload {
        return try {
            val root = JSONObject(jsonString)

            val profile = if (root.has("userProfile") && !root.isNull("userProfile")) {
                try {
                    val pObj = root.getJSONObject("userProfile")
                    UserProfileEntity(
                        id = pObj.optLong("id", 1),
                        username = pObj.optString("username", "PixelHero"),
                        avatarId = pObj.optString("avatarId", "avatar_hero"),
                        level = pObj.optInt("level", 1),
                        totalXp = pObj.optInt("totalXp", 0),
                        perfectDaysTowardNextLevel = pObj.optInt("perfectDaysTowardNextLevel", 0)
                    )
                } catch (e: Exception) { null }
            } else null

            val difficulty = if (root.has("difficultySettings") && !root.isNull("difficultySettings")) {
                try {
                    val dObj = root.getJSONObject("difficultySettings")
                    val levelStr = dObj.optString("difficultyLevel", DifficultyLevel.MEDIUM.name)
                    val level = try { DifficultyLevel.valueOf(levelStr) } catch (e: Exception) { DifficultyLevel.MEDIUM }
                    DifficultySettingsEntity(
                        id = dObj.optLong("id", 1),
                        difficultyLevel = level,
                        perfectDayThreshold = dObj.optDouble("perfectDayThreshold", 0.7).toFloat(),
                        daysRequiredPerLevel = dObj.optInt("daysRequiredPerLevel", 7)
                    )
                } catch (e: Exception) { null }
            } else null

            val streak = if (root.has("streak") && !root.isNull("streak")) {
                try {
                    val sObj = root.getJSONObject("streak")
                    val dateStr = sObj.optString("lastCompletedDate", LocalDate.now().toString())
                    val parsedDate = try { LocalDate.parse(dateStr) } catch (e: Exception) { LocalDate.now() }
                    StreakEntity(
                        id = sObj.optLong("id", 1),
                        currentStreak = sObj.optInt("currentStreak", 0),
                        longestStreak = sObj.optInt("longestStreak", 0),
                        lastCompletedDate = parsedDate
                    )
                } catch (e: Exception) { null }
            } else null

            val tasks = mutableListOf<TaskEntity>()
            if (root.has("tasks") && !root.isNull("tasks")) {
                try {
                    val tArray = root.getJSONArray("tasks")
                    for (i in 0 until tArray.length()) {
                        try {
                            val tObj = tArray.getJSONObject(i)
                            val scheduledTimeStr = tObj.optString("scheduledTime", "09:00")
                            val scheduledDayStr = tObj.optString("scheduledDay", LocalDate.now().toString())
                            val recurrenceStr = tObj.optString("recurrenceType", RecurrenceType.DAILY.name)
                            val priorityStr = tObj.optString("priority", Priority.MEDIUM.name)
                            val difficultyStr = tObj.optString("difficulty", com.pixelquest.app.domain.model.TaskDifficulty.MEDIUM.name)

                            val time = try { LocalTime.parse(scheduledTimeStr) } catch (e: Exception) { LocalTime.of(9, 0) }
                            val day = try { LocalDate.parse(scheduledDayStr) } catch (e: Exception) { LocalDate.now() }
                            val rec = try { RecurrenceType.valueOf(recurrenceStr) } catch (e: Exception) { RecurrenceType.DAILY }
                            val prio = try { Priority.valueOf(priorityStr) } catch (e: Exception) { Priority.MEDIUM }
                            val diff = try { com.pixelquest.app.domain.model.TaskDifficulty.valueOf(difficultyStr) } catch (e: Exception) { com.pixelquest.app.domain.model.TaskDifficulty.MEDIUM }

                            tasks.add(
                                TaskEntity(
                                    id = tObj.optLong("id", 0),
                                    name = tObj.optString("name", "Quest"),
                                    description = tObj.optString("description", ""),
                                    scheduledTime = time,
                                    scheduledDay = day,
                                    recurrenceType = rec,
                                    priority = prio,
                                    difficulty = diff,
                                    isCompleted = tObj.optBoolean("isCompleted", false)
                                )
                            )
                        } catch (e: Exception) {
                            // Skip corrupted individual task item
                        }
                    }
                } catch (e: Exception) { }
            }

            BackupPayload(profile, difficulty, streak, tasks)
        } catch (e: Exception) {
            android.util.Log.e("DataExportImport", "Failed to parse backup JSON, returning empty payload", e)
            BackupPayload(null, null, null, emptyList())
        }
    }
}
