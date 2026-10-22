package com.pixelquest.app.data.backup

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
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
    val levelHistory: List<LevelHistoryEntity>? = null,
    /** The file's format (DataExportImport.FORMAT_VERSION when it was written); 0 before formats were numbered. */
    val formatVersion: Int = DataExportImport.FORMAT_VERSION,
    /** App settings (format 4 on); null in older files, which leaves the phone's settings as they are. */
    val settings: BackupSettings? = null,
    /** The app version that wrote the file (format 3 on); null for older files and for a snapshot. */
    val appVersion: String? = null,
    /** The Namaz Tracker's prayer log (format 5 on). Null in older files: a restore keeps the phone's. */
    val namazLogs: List<NamazLogEntity>? = null,
    /** The Namaz Tracker's settings, with its place (format 5 on). Null when there were none to save. */
    val namazSettings: NamazSettingsEntity? = null
) {
    /** False for a file that isn't a PixelQuest backup (or couldn't be read): nothing to restore. */
    val isUsable: Boolean get() = userProfile != null || tasks.isNotEmpty()

    /**
     * Whether there's progress worth keeping a copy of: XP, a level, a completed quest or a prayed
     * prayer. A fresh install isn't, though it already has "missed" results for starter quests whose
     * time has passed.
     */
    val hasProgress: Boolean
        get() = com.pixelquest.app.domain.OnboardingGate.hasPlayed(userProfile) ||
            logs.orEmpty().any { it.wasCompleted } ||
            namazLogs.orEmpty().any { it.status == PrayerStatus.PRAYED }
}

/**
 * The app settings a backup carries. Each is optional: a value missing from the file isn't changed.
 * Not included: the AI Coach consent (given again, not restored from a file), the leaderboard and
 * account (they belong to the install), onboarding and one-time hints.
 */
data class BackupSettings(
    val sound: Boolean? = null,
    val crtEffect: Boolean? = null,
    val haptics: Boolean? = null,
    val reduceMotion: Boolean? = null,
    val notifications: Boolean? = null,
    val notificationSound: Boolean? = null,
    val notificationVibration: Boolean? = null,
    val theme: String? = null,
    val simpleMode: Boolean? = null
)

object DataExportImport {

    /**
     * The backup format this app writes. Files before Day 34 have no number (read as 0):
     *   v1.0.x: quests with priority, difficulty and isCompleted, no category or active flag.
     *   v1.1.0: quests with category and active flag.
     *   Days 26-31: reminder settings, weekdays, results (logs), level history, perfect days.
     *   3 (Day 34): numbered, with the app version that wrote it.
     *   4 (Day 34): app settings (BackupSettings). Before, a reinstall restored without them.
     *   5 (Namaz Tracker): the prayer log and the tracker's settings, with its place.
     * A newer file still restores what this version knows, and Settings says so.
     */
    const val FORMAT_VERSION = 5

    fun exportToJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("formatVersion", FORMAT_VERSION)
        root.put("appVersion", com.pixelquest.app.BuildConfig.VERSION_NAME)

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

        payload.settings?.let { s ->
            root.put("settings", JSONObject().apply {
                s.sound?.let { put("sound", it) }
                s.crtEffect?.let { put("crtEffect", it) }
                s.haptics?.let { put("haptics", it) }
                s.reduceMotion?.let { put("reduceMotion", it) }
                s.notifications?.let { put("notifications", it) }
                s.notificationSound?.let { put("notificationSound", it) }
                s.notificationVibration?.let { put("notificationVibration", it) }
                s.theme?.let { put("theme", it) }
                s.simpleMode?.let { put("simpleMode", it) }
            })
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

        payload.namazSettings?.let { n ->
            root.put("namazSettings", JSONObject().apply {
                put("enabled", n.enabled)
                put("method", n.method)
                put("asrMadhab", n.asrMadhab)
                n.highLatitudeRule?.let { put("highLatitudeRule", it) }
                n.latitude?.let { put("latitude", it) }
                n.longitude?.let { put("longitude", it) }
                n.city?.let { put("city", it) }
                put("reminderOffsetMin", n.reminderOffsetMin)
                put("remindFajr", n.remindFajr)
                put("remindDhuhr", n.remindDhuhr)
                put("remindAsr", n.remindAsr)
                put("remindMaghrib", n.remindMaghrib)
                put("remindIsha", n.remindIsha)
                put("adjustFajr", n.adjustFajr)
                put("adjustDhuhr", n.adjustDhuhr)
                put("adjustAsr", n.adjustAsr)
                put("adjustMaghrib", n.adjustMaghrib)
                put("adjustIsha", n.adjustIsha)
                put("xpEnabled", n.xpEnabled)
            })
        }

        payload.namazLogs?.let { namazLogs ->
            root.put("namazLog", JSONArray().apply {
                namazLogs.forEach { log ->
                    put(JSONObject().apply {
                        put("date", log.date.toString())
                        put("prayer", log.prayer.name)
                        put("status", log.status.name)
                        log.prayedAt?.let { put("prayedAt", it) }
                        put("xpAwarded", log.xpAwarded)
                        put("bonusXp", log.bonusXp)
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

            val settings = root.optJSONObject("settings")?.let { s ->
                fun flag(key: String): Boolean? = if (s.has(key)) s.optBoolean(key) else null
                BackupSettings(
                    sound = flag("sound"),
                    crtEffect = flag("crtEffect"),
                    haptics = flag("haptics"),
                    reduceMotion = flag("reduceMotion"),
                    notifications = flag("notifications"),
                    notificationSound = flag("notificationSound"),
                    notificationVibration = flag("notificationVibration"),
                    theme = s.optString("theme").ifBlank { null },
                    simpleMode = flag("simpleMode")
                )
            }

            val namazSettings = root.optJSONObject("namazSettings")?.let { n ->
                val defaults = NamazSettingsEntity()
                fun number(key: String): Double? = if (n.has(key) && !n.isNull(key)) n.optDouble(key).takeIf { !it.isNaN() } else null
                NamazSettingsEntity(
                    enabled = n.optBoolean("enabled", defaults.enabled),
                    method = n.optString("method").ifBlank { defaults.method },
                    asrMadhab = n.optString("asrMadhab").ifBlank { defaults.asrMadhab },
                    highLatitudeRule = n.optString("highLatitudeRule").ifBlank { null },
                    latitude = number("latitude"),
                    longitude = number("longitude"),
                    city = n.optString("city").ifBlank { null },
                    reminderOffsetMin = n.optInt("reminderOffsetMin", defaults.reminderOffsetMin),
                    remindFajr = n.optBoolean("remindFajr", defaults.remindFajr),
                    remindDhuhr = n.optBoolean("remindDhuhr", defaults.remindDhuhr),
                    remindAsr = n.optBoolean("remindAsr", defaults.remindAsr),
                    remindMaghrib = n.optBoolean("remindMaghrib", defaults.remindMaghrib),
                    remindIsha = n.optBoolean("remindIsha", defaults.remindIsha),
                    adjustFajr = n.optInt("adjustFajr", 0),
                    adjustDhuhr = n.optInt("adjustDhuhr", 0),
                    adjustAsr = n.optInt("adjustAsr", 0),
                    adjustMaghrib = n.optInt("adjustMaghrib", 0),
                    adjustIsha = n.optInt("adjustIsha", 0),
                    xpEnabled = n.optBoolean("xpEnabled", defaults.xpEnabled)
                )
            }

            val namazLogs = root.optJSONArray("namazLog")?.let { array ->
                (0 until array.length()).mapNotNull { i ->
                    try {
                        val nObj = array.getJSONObject(i)
                        NamazLogEntity(
                            date = LocalDate.parse(nObj.getString("date")),
                            prayer = Prayer.valueOf(nObj.getString("prayer")),
                            status = PrayerStatus.valueOf(nObj.getString("status")),
                            prayedAt = if (nObj.has("prayedAt") && !nObj.isNull("prayedAt")) nObj.getLong("prayedAt") else null,
                            xpAwarded = nObj.optInt("xpAwarded", 0),
                            // Files from before Namaz XP have none.
                            bonusXp = nObj.optInt("bonusXp", 0)
                        )
                    } catch (e: Exception) { null } // Skip a corrupted entry
                }
            }

            sanitize(
                BackupPayload(
                    profile, difficulty, streak, tasks, logs, levelHistory,
                    formatVersion = root.optInt("formatVersion", 0),
                    settings = settings,
                    appVersion = root.optString("appVersion").ifBlank { null },
                    namazLogs = namazLogs,
                    namazSettings = namazSettings
                )
            )
        } catch (e: Exception) {
            com.pixelquest.app.util.AppLog.e("DataExportImport", "Failed to parse backup JSON, returning empty payload", e)
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
    private const val MAX_REMINDER_OFFSET_MIN = 120
    private const val MAX_PRAYER_ADJUST_MIN = 60
    private const val MAX_CITY_LENGTH = 80

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
                ?.map { it.copy(level = it.level.coerceIn(1, MAX_LEVEL)) },
            formatVersion = payload.formatVersion,
            settings = payload.settings,
            appVersion = payload.appVersion,
            // One row per prayer per day, as the table's unique index needs; none for a day ahead.
            namazLogs = payload.namazLogs
                ?.filter { inRange(it.date, today) }
                ?.distinctBy { it.date to it.prayer }
                ?.map {
                    it.copy(
                        id = 0,
                        prayedAt = it.prayedAt?.takeIf { t -> t >= 0 },
                        xpAwarded = it.xpAwarded.coerceIn(0, MAX_POINTS_PER_LOG),
                        bonusXp = it.bonusXp.coerceIn(0, MAX_POINTS_PER_LOG)
                    )
                },
            namazSettings = payload.namazSettings?.let { n ->
                fun adjust(minutes: Int) = minutes.coerceIn(-MAX_PRAYER_ADJUST_MIN, MAX_PRAYER_ADJUST_MIN)
                // A place off the map is dropped (the times fall back to Lahore), not half kept.
                val placeOk = n.latitude != null && n.longitude != null && n.latitude in -90.0..90.0 && n.longitude in -180.0..180.0
                n.copy(
                    id = NamazSettingsEntity.SINGLE_ROW_ID,
                    latitude = n.latitude.takeIf { placeOk },
                    longitude = n.longitude.takeIf { placeOk },
                    city = n.city?.take(MAX_CITY_LENGTH),
                    reminderOffsetMin = n.reminderOffsetMin.coerceIn(0, MAX_REMINDER_OFFSET_MIN),
                    adjustFajr = adjust(n.adjustFajr),
                    adjustDhuhr = adjust(n.adjustDhuhr),
                    adjustAsr = adjust(n.adjustAsr),
                    adjustMaghrib = adjust(n.adjustMaghrib),
                    adjustIsha = adjust(n.adjustIsha)
                )
            }
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
