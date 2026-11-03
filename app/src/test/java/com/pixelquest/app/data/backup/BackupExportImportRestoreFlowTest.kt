package com.pixelquest.app.data.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * End-to-end backup flow and extreme limit cases:
 * - Full user flow: Export state -> Wipe database -> Restore -> Verify exact state
 * - Empty and whitespace-only files
 * - Corrupt, truncated, and non-JSON payloads
 * - Older formats and future format versions with unknown fields
 * - Oversized backups with thousands of tasks and history logs
 * - Hostile payloads: SQL injection, XSS markup, extreme overflows, invalid dates, prototype pollution
 * - Restore undo safety copy flow
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupExportImportRestoreFlowTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databases = mutableListOf<AppDatabase>()
    private lateinit var safetyCopyFile: File
    private lateinit var safetyCopy: RestoreSafetyCopy
    private lateinit var fakeSettingsRepo: FakeSettingsRepository

    private val testDate: LocalDate = LocalDate.of(2026, 10, 15)

    private fun createDb(): AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
        .also { databases += it }

    @Before
    fun setUp() {
        safetyCopyFile = File(context.filesDir, "test-safety-copy.json")
        if (safetyCopyFile.exists()) safetyCopyFile.delete()
        safetyCopy = RestoreSafetyCopy(safetyCopyFile)
        fakeSettingsRepo = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        databases.forEach { it.close() }
        if (safetyCopyFile.exists()) safetyCopyFile.delete()
    }

    private fun createRestorer(db: AppDatabase): BackupRestorer = BackupRestorer(
        db = db,
        taskAlarmScheduler = TaskAlarmScheduler(context),
        levelUpSignalManager = LevelUpSignalManager(context),
        settingsRepository = fakeSettingsRepo,
        safetyCopy = safetyCopy,
    )

    @Test
    fun flow_exportWipeAndRestore_restoresAllEntitiesAndSettingsCleanly() = runBlocking {
        val db = createDb()
        val restorer = createRestorer(db)

        // 1. Seed complete user state
        val initialProfile = UserProfileEntity(
            id = 1,
            username = "QuestMaster",
            avatarId = "avatar_mage",
            level = 12,
            totalXp = 5400,
            perfectDaysTowardNextLevel = 4,
        )
        val initialDifficulty = DifficultySettingsEntity(
            id = 1,
            difficultyLevel = DifficultyLevel.HARD,
            perfectDayThreshold = 0.90f,
            daysRequiredPerLevel = 14,
        )
        val initialStreak = StreakEntity(
            id = 1,
            currentStreak = 18,
            longestStreak = 25,
            lastCompletedDate = testDate.minusDays(1),
            perfectDaysCount = 18,
        )
        val initialTasks = listOf(
            TaskEntity(
                id = 101,
                name = "Morning Meditation",
                description = "15 minutes mindfulness",
                scheduledDay = testDate,
                scheduledTime = LocalTime.of(7, 30),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.HEALTH,
                isActive = true,
                reminderEnabled = true,
                reminderLeadMinutes = 10,
                reminderStyle = ReminderStyle.PROMPT,
                weeklyDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            ),
            TaskEntity(
                id = 102,
                name = "Deep Coding",
                description = "Write robust tests",
                scheduledDay = testDate,
                scheduledTime = LocalTime.of(14, 0),
                recurrenceType = RecurrenceType.WEEKLY,
                category = TaskCategory.LEARNING,
                isActive = true,
            ),
        )
        val initialLogs = listOf(
            TaskCompletionLogEntity(
                taskId = 101,
                completedDate = testDate.minusDays(1),
                wasCompleted = true,
                pointsAwarded = 25,
            ),
        )
        val initialHistory = listOf(
            LevelHistoryEntity(level = 2, achievedDate = 1759000000000L, difficultyAtTimeOfLevelUp = "MEDIUM"),
            LevelHistoryEntity(level = 3, achievedDate = 1759500000000L, difficultyAtTimeOfLevelUp = "HARD"),
        )
        val initialNamazSettings = NamazSettingsEntity(
            enabled = true,
            method = "ISNA",
            asrMadhab = "Hanafi",
            latitude = 31.5204,
            longitude = 74.3587,
            city = "Lahore",
            reminderOffsetMin = 15,
            xpEnabled = true,
        )
        val initialNamazLogs = listOf(
            NamazLogEntity(
                date = testDate.minusDays(1),
                prayer = Prayer.FAJR,
                status = PrayerStatus.PRAYED,
                prayedAt = 1759800000000L,
                xpAwarded = 50,
                bonusXp = 10,
            ),
        )

        db.userProfileDao().insertProfile(initialProfile)
        db.difficultySettingsDao().insertSettings(initialDifficulty)
        db.streakDao().insertStreak(initialStreak)
        initialTasks.forEach { db.taskDao().insertTask(it) }
        initialLogs.forEach { db.taskCompletionLogDao().insertLog(it) }
        initialHistory.forEach { db.levelHistoryDao().insertLevelHistory(it) }
        db.namazSettingsDao().save(initialNamazSettings)
        initialNamazLogs.forEach { db.namazLogDao().upsert(it) }

        // 2. Snapshot & Export
        val snapshot = restorer.snapshot()
        val jsonExport = DataExportImport.exportToJson(snapshot)
        assertTrue("Exported JSON must contain formatVersion", jsonExport.contains("\"formatVersion\": 5"))
        assertTrue("Exported JSON must contain task name", jsonExport.contains("Morning Meditation"))

        // 3. Wipe database
        db.clearAllTables()
        assertNull(db.userProfileDao().getProfile().first())
        assertTrue(db.taskDao().getAllTasks().first().isEmpty())

        // 4. Import & Restore
        val parsedPayload = DataExportImport.importFromJson(jsonExport)
        assertTrue("Imported payload should be usable", parsedPayload.isUsable)
        restorer.restore(parsedPayload)

        // 5. Verify restored state
        val restoredProfile = db.userProfileDao().getProfile().first()
        assertNotNull(restoredProfile)
        assertEquals("QuestMaster", restoredProfile?.username)
        assertEquals(12, restoredProfile?.level)
        assertEquals(5400, restoredProfile?.totalXp)

        val restoredStreak = db.streakDao().getCurrentStreak().first()
        assertEquals(18, restoredStreak?.currentStreak)
        assertEquals(25, restoredStreak?.longestStreak)

        val restoredTasks = db.taskDao().getAllTasks().first()
        assertEquals(2, restoredTasks.size)
        val meditation = restoredTasks.first { it.id == 101L }
        assertEquals("Morning Meditation", meditation.name)
        assertEquals(ReminderStyle.PROMPT, meditation.reminderStyle)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), meditation.weeklyDays)

        val restoredLogs = db.taskCompletionLogDao().getAllLogs().first()
        assertEquals(1, restoredLogs.size)
        assertEquals(101L, restoredLogs[0].taskId)
        assertTrue(restoredLogs[0].wasCompleted)

        val restoredHistory = db.levelHistoryDao().getAllHistory().first()
        assertEquals(2, restoredHistory.size)

        val restoredNamaz = db.namazSettingsDao().get()
        assertNotNull(restoredNamaz)
        assertEquals("Lahore", restoredNamaz?.city)
        assertEquals(31.5204, restoredNamaz?.latitude ?: 0.0, 0.0001)

        val restoredNamazLogs = db.namazLogDao().getAll()
        assertEquals(1, restoredNamazLogs.size)
        assertEquals(Prayer.FAJR, restoredNamazLogs[0].prayer)
    }

    @Test
    fun limits_emptyOrWhitespaceFile_returnsNonUsablePayloadSafely() {
        val emptyInputs = listOf("", "   ", "\n\t\r", "   \n   ")
        for (input in emptyInputs) {
            val payload = DataExportImport.importFromJson(input)
            assertFalse("Empty input should not produce usable payload", payload.isUsable)
            assertNull(payload.userProfile)
            assertTrue(payload.tasks.isEmpty())
        }
    }

    @Test
    fun limits_corruptJsonPayloads_returnsEmptyPayloadWithoutCrashing() {
        val corruptInputs = listOf(
            "{",
            "{\"userProfile\": {\"username\": \"Unterminated",
            "{\"tasks\": [{\"id\": 1, \"name\": \"Broken\",}",
            "not a json object at all",
            "null",
            "42",
            "{\"tasks\": \"not-an-array\"}",
            "<<<XML>><header>not json</header></XML>",
        )
        for (input in corruptInputs) {
            val payload = DataExportImport.importFromJson(input)
            assertFalse("Corrupt payload should not be usable", payload.isUsable)
            assertNull(payload.userProfile)
            assertTrue(payload.tasks.isEmpty())
        }
    }

    @Test
    fun limits_futureVersionWithUnknownFields_restoresKnownDataGracefully() = runBlocking {
        val db = createDb()
        val restorer = createRestorer(db)

        val futureJson = """
            {
              "formatVersion": 999,
              "appVersion": "99.0.0",
              "futureNewFeatureData": {
                "quantumQuests": true,
                "multiplayerGalaxy": ["Alpha", "Beta"]
              },
              "userProfile": {
                "id": 1,
                "username": "TimeTraveler",
                "avatarId": "avatar_hero",
                "level": 5,
                "totalXp": 2500,
                "perfectDaysTowardNextLevel": 2,
                "cyberneticsEquipped": 4
              },
              "tasks": [
                {
                  "id": 42,
                  "name": "Warp Drive Check",
                  "description": "Routine hyperspace inspection",
                  "scheduledDay": "2026-10-15",
                  "scheduledTime": "10:00",
                  "recurrenceType": "DAILY",
                  "category": "FITNESS",
                  "tachyonFrequency": 12.5
                }
              ]
            }
        """.trimIndent()

        val payload = DataExportImport.importFromJson(futureJson)
        assertTrue("Future payload with standard data should be usable", payload.isUsable)
        assertEquals(999, payload.formatVersion)
        assertEquals("TimeTraveler", payload.userProfile?.username)

        restorer.restore(payload)
        val profile = db.userProfileDao().getProfile().first()
        assertEquals("TimeTraveler", profile?.username)
        val task = db.taskDao().getAllTasks().first().single()
        assertEquals("Warp Drive Check", task.name)
    }

    @Test
    fun limits_oversizedPayloadThousandsOfTasks_processesSafelyWithoutOom() = runBlocking {
        val db = createDb()
        val restorer = createRestorer(db)

        val root = JSONObject()
        root.put("formatVersion", 5)
        root.put("appVersion", "1.2.0")

        val pObj = JSONObject().apply {
            put("id", 1)
            put("username", "StressTester")
            put("avatarId", "avatar_hero")
            put("level", 50)
            put("totalXp", 100000)
            put("perfectDaysTowardNextLevel", 10)
        }
        root.put("userProfile", pObj)

        val tasksArray = JSONArray()
        val logsArray = JSONArray()
        val taskCount = 1000
        for (i in 1..taskCount) {
            val tObj = JSONObject().apply {
                put("id", i.toLong())
                put("name", "Stress Task #$i with extra padding text to verify large payload handling")
                put("description", "Description detail for test task $i")
                put("scheduledTime", "10:00")
                put("scheduledDay", "2026-10-15")
                put("recurrenceType", "DAILY")
                put("category", "FITNESS")
                put("isActive", true)
            }
            tasksArray.put(tObj)

            val lObj = JSONObject().apply {
                put("taskId", i.toLong())
                put("completedDate", "2026-10-15")
                put("wasCompleted", i % 2 == 0)
                put("pointsAwarded", 50)
            }
            logsArray.put(lObj)
        }
        root.put("tasks", tasksArray)
        root.put("logs", logsArray)

        val largeJson = root.toString()
        assertTrue("Payload should be substantially sized", largeJson.length > 200_000)

        val imported = DataExportImport.importFromJson(largeJson)
        assertEquals(taskCount, imported.tasks.size)
        assertEquals(taskCount, imported.logs?.size)

        restorer.restore(imported)
        val restoredTaskCount = db.taskDao().getAllTasks().first().size
        assertEquals(taskCount, restoredTaskCount)
    }

    @Test
    fun limits_hostileSqlInjectionXssAndNullBytes_sanitizedSafelyIntoDatabase() = runBlocking {
        val db = createDb()
        val restorer = createRestorer(db)

        val maliciousSqlName = "'; DROP TABLE tasks; DROP TABLE user_profile; --"
        val xssDescription = "<script>alert('xss')</script><img src='x' onerror='eval(atob(\"...\"))'>"
        val nullByteTitle = "CleanName\u0000\u0001\u001FHidden"

        val hostileJson = JSONObject().apply {
            put("formatVersion", 5)
            put(
                "userProfile",
                JSONObject().apply {
                    put("id", 999) // Invalid row id (must be sanitized to 1)
                    put("username", maliciousSqlName)
                    put("avatarId", "avatar_hero' OR '1'='1")
                    put("level", 999999) // Overflows maximum level (must be clamped to 999)
                    put("totalXp", 2147483647) // Overflows max xp (must be clamped to 10_000_000)
                    put("perfectDaysTowardNextLevel", -50) // Negative days (clamped to 0)
                },
            )
            put(
                "streak",
                JSONObject().apply {
                    put("id", 44)
                    put("currentStreak", 9999999) // Clamped to MAX_DAYS
                    put("longestStreak", -10) // Clamped to 0
                    put("lastCompletedDate", "2999-01-01") // Future evaluation date (dropped)
                },
            )
            put(
                "tasks",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("id", 1L)
                            put("name", maliciousSqlName)
                            put("description", xssDescription)
                            put("scheduledDay", "2026-10-15")
                            put("scheduledTime", "08:00")
                            put("recurrenceType", "DAILY")
                            put("category", "FITNESS")
                        },
                    )
                    put(
                        JSONObject().apply {
                            put("id", 2L)
                            put("name", nullByteTitle)
                            put("description", "Safe text")
                            put("scheduledDay", "+999999999-12-31") // Far future date (clamped to today)
                            put("scheduledTime", "09:00")
                            put("recurrenceType", "DAILY")
                            put("category", "FITNESS")
                        },
                    )
                    put(
                        JSONObject().apply {
                            put("id", -99L) // Non-positive id (must be dropped)
                            put("name", "Dropped task")
                            put("scheduledDay", "2026-10-15")
                            put("scheduledTime", "10:00")
                        },
                    )
                },
            )
            put(
                "__proto__",
                JSONObject().apply {
                    put("isAdmin", true)
                },
            )
            put("drop_database", true)
        }.toString()

        val imported = DataExportImport.importFromJson(hostileJson)
        val profile = imported.userProfile
        assertNotNull(profile)
        assertEquals("Profile row ID must be sanitized to 1", 1L, profile?.id)
        assertEquals("Level must be clamped to 999", 999, profile?.level)
        assertEquals("XP must be clamped to 10_000_000", 10_000_000, profile?.totalXp)
        assertEquals("Negative days clamped to 0", 0, profile?.perfectDaysTowardNextLevel)

        val streak = imported.streak
        assertNotNull(streak)
        assertEquals(1L, streak?.id)
        assertEquals(36_500, streak?.currentStreak)
        assertEquals(0, streak?.longestStreak)
        assertNull("Future completed date should be dropped", streak?.lastCompletedDate)

        // Only positive tasks should survive
        assertEquals(listOf(1L, 2L), imported.tasks.map { it.id })

        // Restore into database - Room parameterization prevents SQL injection
        restorer.restore(imported)

        // Verify database tables are completely intact
        val dbTasks = db.taskDao().getAllTasks().first()
        assertEquals(2, dbTasks.size)
        val task1 = dbTasks.first { it.id == 1L }
        assertEquals(maliciousSqlName, task1.name)
        assertEquals(xssDescription, task1.description)

        val dbProfile = db.userProfileDao().getProfile().first()
        assertNotNull(dbProfile)
        assertEquals(maliciousSqlName, dbProfile?.username)
    }

    @Test
    fun flow_safetyCopyAndUndoRestore_restoresPreviousGameState() = runBlocking {
        val db = createDb()
        val restorer = createRestorer(db)

        // State A: initial active game
        val profileA = UserProfileEntity(id = 1, username = "OriginalHero", avatarId = "avatar_hero", level = 5, totalXp = 1000)
        val taskA = TaskEntity(
            id = 1,
            name = "Original Task",
            description = "Original Desc",
            scheduledDay = testDate,
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
        )
        db.userProfileDao().insertProfile(profileA)
        db.taskDao().insertTask(taskA)

        assertFalse("No undo should exist initially", restorer.canUndoLastRestore())

        // State B: payload to restore
        val profileB = UserProfileEntity(id = 1, username = "NewRestoredHero", avatarId = "avatar_mage", level = 10, totalXp = 5000)
        val taskB = TaskEntity(
            id = 2,
            name = "Restored Task",
            description = "Restored Desc",
            scheduledDay = testDate,
            scheduledTime = LocalTime.of(9, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.LEARNING,
        )
        val payloadB = BackupPayload(
            userProfile = profileB,
            difficultySettings = null,
            streak = null,
            tasks = listOf(taskB),
        )

        // Restore State B
        restorer.restore(payloadB)
        assertEquals("NewRestoredHero", db.userProfileDao().getProfile().first()?.username)
        assertEquals(listOf("Restored Task"), db.taskDao().getAllTasks().first().map { it.name })
        assertTrue("Undo should be available after restore", restorer.canUndoLastRestore())

        // Execute Undo
        val undoResult = restorer.undoLastRestore()
        assertTrue("Undo operation should succeed", undoResult)
        assertEquals("OriginalHero", db.userProfileDao().getProfile().first()?.username)
        assertEquals(listOf("Original Task"), db.taskDao().getAllTasks().first().map { it.name })
        assertFalse("Safety copy should be cleared after undo", restorer.canUndoLastRestore())
    }
}
