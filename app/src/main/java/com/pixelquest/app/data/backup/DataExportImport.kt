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
        val root = JSONObject(jsonString)

        val profile = if (root.has("userProfile")) {
            val pObj = root.getJSONObject("userProfile")
            UserProfileEntity(
                id = pObj.optLong("id", 1),
                username = pObj.getString("username"),
                avatarId = pObj.getString("avatarId"),
                level = pObj.optInt("level", 1),
                totalXp = pObj.optInt("totalXp", 0),
                perfectDaysTowardNextLevel = pObj.optInt("perfectDaysTowardNextLevel", 0)
            )
        } else null

        val difficulty = if (root.has("difficultySettings")) {
            val dObj = root.getJSONObject("difficultySettings")
            DifficultySettingsEntity(
                id = dObj.optLong("id", 1),
                difficultyLevel = DifficultyLevel.valueOf(dObj.getString("difficultyLevel")),
                perfectDayThreshold = dObj.optDouble("perfectDayThreshold", 0.7).toFloat(),
                daysRequiredPerLevel = dObj.optInt("daysRequiredPerLevel", 7)
            )
        } else null

        val streak = if (root.has("streak")) {
            val sObj = root.getJSONObject("streak")
            StreakEntity(
                id = sObj.optLong("id", 1),
                currentStreak = sObj.optInt("currentStreak", 0),
                longestStreak = sObj.optInt("longestStreak", 0),
                lastCompletedDate = LocalDate.parse(sObj.getString("lastCompletedDate"))
            )
        } else null

        val tasks = mutableListOf<TaskEntity>()
        if (root.has("tasks")) {
            val tArray = root.getJSONArray("tasks")
            for (i in 0 until tArray.length()) {
                val tObj = tArray.getJSONObject(i)
                tasks.add(
                    TaskEntity(
                        id = tObj.optLong("id", 0),
                        name = tObj.getString("name"),
                        description = tObj.optString("description", ""),
                        scheduledTime = LocalTime.parse(tObj.getString("scheduledTime")),
                        scheduledDay = LocalDate.parse(tObj.getString("scheduledDay")),
                        recurrenceType = RecurrenceType.valueOf(tObj.getString("recurrenceType")),
                        priority = Priority.valueOf(tObj.optString("priority", Priority.MEDIUM.name)),
                        difficulty = com.pixelquest.app.domain.model.TaskDifficulty.valueOf(tObj.optString("difficulty", com.pixelquest.app.domain.model.TaskDifficulty.MEDIUM.name)),
                        isCompleted = tObj.optBoolean("isCompleted", false)
                    )
                )
            }
        }

        return BackupPayload(profile, difficulty, streak, tasks)
    }
}
