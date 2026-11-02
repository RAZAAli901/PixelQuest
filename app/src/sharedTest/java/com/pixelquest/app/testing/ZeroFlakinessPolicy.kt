package com.pixelquest.app.testing

import java.io.File

enum class FlakinessViolationType {
    THREAD_SLEEP,
    SYSTEM_CLOCK_SLEEP,
    TIME_UNIT_SLEEP,
    REAL_NETWORK,
}

data class FlakinessViolation(
    val file: File,
    val line: Int,
    val lineContent: String,
    val type: FlakinessViolationType,
    val message: String,
)

/**
 * Enforces zero-flakiness policy across unit and integration tests (V015).
 * Detects Thread.sleep, real delays, and real network calls.
 */
object ZeroFlakinessPolicy {

    private val FORBIDDEN_PATTERNS = listOf(
        Regex("""\bThread\.sleep\s*\(""") to FlakinessViolationType.THREAD_SLEEP,
        Regex("""\bSystemClock\.sleep\s*\(""") to FlakinessViolationType.SYSTEM_CLOCK_SLEEP,
        Regex("""TimeUnit\.[A-Z_]+\.sleep\s*\(""") to FlakinessViolationType.TIME_UNIT_SLEEP,
        Regex("""\bjava\.net\.Socket\s*\(""") to FlakinessViolationType.REAL_NETWORK,
        Regex("""\bjava\.net\.ServerSocket\s*\(""") to FlakinessViolationType.REAL_NETWORK,
        Regex("""\bjava\.net\.URL\s*\(""") to FlakinessViolationType.REAL_NETWORK,
        Regex("""\bHttpURLConnection\b""") to FlakinessViolationType.REAL_NETWORK,
    )

    // Legacy test files written prior to the Phase A test harness (V001-V020) being migrated in subsequent milestones
    val LEGACY_ALLOWLIST = setOf(
        "NamazReminderInstrumentedTest.kt",
        "StudyDueAlarmInstrumentedTest.kt",
        "ChessChallengeFlowTest.kt",
        "ChessGameFlowTest.kt",
        "ChessSessionViewModelTest.kt",
        "DaroodStatsClockTest.kt",
        "PartyFollowerTest.kt",
        "ChatListTest.kt",
        "MilestoneBannerTest.kt",
        "NumberInputBarTest.kt",
        "SendStateTest.kt",
        "TodayStripTest.kt",
        "CreatePartyLimitsUiTest.kt",
        "CreatePartyTest.kt",
        "PartyInfoTest.kt",
        "AddPeopleTest.kt",
        "RaceBoardTest.kt",
        "BackupFormatVersionTest.kt",
        "FirstLaunchSeedRaceTest.kt",
        "ResetProgressIntegrationTest.kt",
        "StudyEndToEndTest.kt",
        "PartyFlowForEveryKindTest.kt",
        "TypeAwareScreensTest.kt",
        "StudyReminderAlarmSyncTest.kt",
        "StudyChatFlowTest.kt",
        "StudyChatRepositoryTest.kt",
        "StudyChatViewModelTest.kt",
        "StudyTimerNotificationTest.kt",
        "HomeStudyTest.kt",
        "StudyCircleDoneItemTest.kt",
        "StudyPartyFlowTest.kt",
        "StudyPartyViewModelTest.kt",
        "StudyReminderFlowTest.kt",
        "StudyRemindersViewModelTest.kt",
        "LocalSupabase.kt",
        "TestViewModels.kt",
        "RestoreFromWelcomeTest.kt",
        "CloudExportTest.kt",
        "NamazEndToEndTest.kt",
        "NamazPersistenceTest.kt",
        "AutoBackupRestoreTest.kt",
        "BackupMessagesTest.kt",
        "BackupSavedCheckTest.kt",
        "UndoRestoreSettingsTest.kt",
    )

    fun scanContent(file: File, content: String): List<FlakinessViolation> {
        val violations = mutableListOf<FlakinessViolation>()
        var inBlockComment = false

        content.lines().forEachIndexed { index, line ->
            var current = line.trim()

            if (inBlockComment) {
                if (current.contains("*/")) {
                    current = current.substringAfter("*/").trim()
                    inBlockComment = false
                } else {
                    return@forEachIndexed
                }
            }

            if (current.startsWith("/*")) {
                if (current.contains("*/")) {
                    current = current.substringAfter("*/").trim()
                } else {
                    inBlockComment = true
                    return@forEachIndexed
                }
            }

            val code = current.substringBefore("//").trim()
            if (code.isEmpty() || code.startsWith("*")) return@forEachIndexed

            for ((pattern, type) in FORBIDDEN_PATTERNS) {
                if (pattern.containsMatchIn(code)) {
                    violations.add(
                        FlakinessViolation(
                            file = file,
                            line = index + 1,
                            lineContent = code,
                            type = type,
                            message = "Forbidden non-deterministic pattern '${pattern.pattern}' found at line ${index + 1}",
                        ),
                    )
                }
            }
        }
        return violations
    }

    fun scanDirectory(dir: File, excludeLegacyAllowlist: Boolean = true): List<FlakinessViolation> {
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .filter { !it.name.contains("ZeroFlakinessPolicy") }
            .filter { !excludeLegacyAllowlist || it.name !in LEGACY_ALLOWLIST }
            .flatMap { scanContent(it, it.readText()) }
            .toList()
    }
}
