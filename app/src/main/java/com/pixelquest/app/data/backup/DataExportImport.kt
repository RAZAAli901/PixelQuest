package com.pixelquest.app.data.backup

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class BackupPayload(
    val userProfile: UserProfileEntity?,
    val difficultySettings: DifficultySettingsEntity?,
    val streak: StreakEntity?,
    val tasks: List<TaskEntity>,
    /** Completion history (Day 28). Null for older backups, which had none: a restore keeps the current history. */
    val logs: List<TaskCompletionLogEntity>? = null,
    /** Level-up timeline (Day 30). Null for older backups, which had none: a restore keeps the current timeline. */
    val levelHistory: List<LevelHistoryEntity>? = null
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
                streak.lastCompletedDate?.let { put("lastCompletedDate", it.toString()) }
                put("perfectDaysCount", streak.perfectDaysCount)
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
                put("category", task.category.name)
                put("isActive", task.isActive)
                put("reminderEnabled", task.reminderEnabled)
                put("reminderLeadMinutes", task.reminderLeadMinutes)
                put("reminderStyle", task.reminderStyle.name)
                put("weeklyDays", JSONArray(task.weeklyDays.sorted().map { it.name }))
            }
            tasksArray.put(tObj)
        }
        root.put("tasks", tasksArray)

        payload.logs?.let { logs ->
            val logsArray = JSONArray()
            logs.forEach { log ->
                logsArray.put(JSONObject().apply {
                    put("taskId", log.taskId)
                    put("completedDate", log.completedDate.toString())
                    put("wasCompleted", log.wasCompleted)
                    put("pointsAwarded", log.pointsAwarded)
                })
            }
            root.put("logs", logsArray)
        }

        payload.levelHistory?.let { history ->
            root.put("levelHistory", JSONArray().apply {
                history.forEach { entry ->
                    put(JSONObject().apply {
                        put("level", entry.level)
                        put("achievedDate", entry.achievedDate)
                        put("difficulty", entry.difficultyAtTimeOfLevelUp)
                    })
                }
            })
        }

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
                    // Missing (never evaluated) stays null rather than becoming today.
                    val parsedDate = try { LocalDate.parse(sObj.optString("lastCompletedDate")) } catch (e: Exception) { null }
                    StreakEntity(
                        id = sObj.optLong("id", 1),
                        currentStreak = sObj.optInt("currentStreak", 0),
                        longestStreak = sObj.optInt("longestStreak", 0),
                        lastCompletedDate = parsedDate,
                        perfectDaysCount = sObj.optInt("perfectDaysCount", 0)
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
                            val categoryStr = tObj.optString("category", TaskCategory.FITNESS.name)

                            val time = try { LocalTime.parse(scheduledTimeStr) } catch (e: Exception) { LocalTime.of(9, 0) }
                            val day = try { LocalDate.parse(scheduledDayStr) } catch (e: Exception) { LocalDate.now() }
                            val rec = try { RecurrenceType.valueOf(recurrenceStr) } catch (e: Exception) { RecurrenceType.DAILY }
                            val cat = try { TaskCategory.valueOf(categoryStr) } catch (e: Exception) { TaskCategory.FITNESS }

                            tasks.add(
                                TaskEntity(
                                    id = tObj.optLong("id", 0),
                                    name = tObj.optString("name", "Quest"),
                                    description = tObj.optString("description", ""),
                                    scheduledTime = time,
                                    scheduledDay = day,
                                    recurrenceType = rec,
                                    category = cat,
                                    isActive = tObj.optBoolean("isActive", true),
                                    // Backups made before Day 26 have no reminder fields; keep the defaults.
                                    reminderEnabled = tObj.optBoolean("reminderEnabled", true),
                                    reminderLeadMinutes = tObj.optInt("reminderLeadMinutes", 0).coerceIn(0, 120),
                                    reminderStyle = ReminderStyle.values()
                                        .firstOrNull { it.name == tObj.optString("reminderStyle") }
                                        ?: ReminderStyle.STANDARD,
                                    // Backups made before Day 27 have no weeklyDays; empty keeps the first date's weekday.
                                    weeklyDays = parseWeeklyDays(tObj.optJSONArray("weeklyDays"))
                                )
                            )
                        } catch (e: Exception) {
                            // Skip corrupted individual task item
                        }
                    }
                } catch (e: Exception) { }
            }

            val logs = root.optJSONArray("logs")?.let { array ->
                (0 until array.length()).mapNotNull { i ->
                    try {
                        val lObj = array.getJSONObject(i)
                        TaskCompletionLogEntity(
                            taskId = lObj.getLong("taskId"),
                            completedDate = LocalDate.parse(lObj.getString("completedDate")),
                            wasCompleted = lObj.optBoolean("wasCompleted", false),
                            pointsAwarded = lObj.optInt("pointsAwarded", 0)
                        )
                    } catch (e: Exception) { null } // Skip a corrupted entry
                }
            }

            val levelHistory = root.optJSONArray("levelHistory")?.let { array ->
                (0 until array.length()).mapNotNull { i ->
                    try {
                        val hObj = array.getJSONObject(i)
                        LevelHistoryEntity(
                            level = hObj.getInt("level"),
                            achievedDate = hObj.getLong("achievedDate"),
                            difficultyAtTimeOfLevelUp = hObj.optString("difficulty", DifficultyLevel.MEDIUM.name)
                        )
                    } catch (e: Exception) { null } // Skip a corrupted entry
                }
            }

            sanitize(BackupPayload(profile, difficulty, streak, tasks, logs, levelHistory))
        } catch (e: Exception) {
            android.util.Log.e("DataExportImport", "Failed to parse backup JSON, returning empty payload", e)
            BackupPayload(null, null, null, emptyList())
        }
    }

    // Plausible ranges for imported values. A backup is a file the player can edit (or be sent), and
    // out-of-range values used to crash or corrupt the app: a far-future quest date overflowed the
    // alarm time and crashed on every boot, a very old one hung Analytics, a huge streak overflowed
    // the XP bonus into negative XP, a future "last evaluated" date froze streaks, and a profile id
    // other than 1 restored nothing (the app only reads row 1).
    private val EARLIEST_DATE: LocalDate = LocalDate.of(2000, 1, 1)
    private const val MAX_LEVEL = 999
    private const val MAX_XP = 10_000_000
    private const val MAX_DAYS = 36_500
    private const val MAX_POINTS_PER_LOG = 10_000

    /** Brings every imported value into a range the app can handle; see the notes above. */
    internal fun sanitize(payload: BackupPayload, today: LocalDate = LocalDate.now()): BackupPayload {
        val latestTaskDate = today.plusYears(5)
        fun inRange(date: LocalDate, latest: LocalDate) = !date.isBefore(EARLIEST_DATE) && !date.isAfter(latest)

        val tasks = payload.tasks
            .filter { it.id > 0 }
            .distinctBy { it.id }
            .map { if (inRange(it.scheduledDay, latestTaskDate)) it else it.copy(scheduledDay = today) }
        return BackupPayload(
            userProfile = payload.userProfile?.let {
                it.copy(
                    id = 1,
                    level = it.level.coerceIn(1, MAX_LEVEL),
                    totalXp = it.totalXp.coerceIn(0, MAX_XP),
                    perfectDaysTowardNextLevel = it.perfectDaysTowardNextLevel.coerceIn(0, MAX_DAYS)
                )
            },
            difficultySettings = payload.difficultySettings?.copy(id = 1),
            streak = payload.streak?.let {
                it.copy(
                    id = 1,
                    currentStreak = it.currentStreak.coerceIn(0, MAX_DAYS),
                    longestStreak = it.longestStreak.coerceIn(0, MAX_DAYS),
                    perfectDaysCount = it.perfectDaysCount.coerceIn(0, MAX_DAYS),
                    // A day not yet over can't have been evaluated.
                    lastCompletedDate = it.lastCompletedDate?.takeIf { d -> inRange(d, today.minusDays(1)) }
                )
            },
            tasks = tasks,
            logs = payload.logs
                ?.filter { inRange(it.completedDate, today) }
                ?.map { it.copy(pointsAwarded = it.pointsAwarded.coerceIn(0, MAX_POINTS_PER_LOG)) },
            levelHistory = payload.levelHistory
                ?.filter { it.achievedDate in 0..(System.currentTimeMillis() + 86_400_000L) }
                ?.map { it.copy(level = it.level.coerceIn(1, MAX_LEVEL)) }
        )
    }

    /** Day names such as "MONDAY"; unknown entries are skipped. */
    private fun parseWeeklyDays(array: JSONArray?): Set<DayOfWeek> {
        if (array == null) return emptySet()
        return (0 until array.length())
            .mapNotNull { i -> DayOfWeek.values().firstOrNull { it.name == array.optString(i) } }
            .toSet()
    }
}
