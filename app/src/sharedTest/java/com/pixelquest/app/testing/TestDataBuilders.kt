package com.pixelquest.app.testing

import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.darood.data.local.DaroodEntryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/**
 * Deterministic test data generator and builders using a seeded [Random].
 * Produces fake test data for Quests, Tasks, Streaks, UserProfiles, Parties,
 * StudyItems, DaroodEntries, and ChessGames.
 */
class TestDataGenerator(val seed: Long = 42L) {
    private val random = Random(seed)

    fun nextLong(from: Long = 1L, until: Long = 100_000L): Long = random.nextLong(from, until)
    fun nextInt(from: Int = 1, until: Int = 100): Int = random.nextInt(from, until)
    fun nextBoolean(): Boolean = random.nextBoolean()

    fun buildTask(
        id: Long = nextLong(),
        name: String = "Fake Task ${nextInt()}",
        description: String = "Description for fake task",
        scheduledDay: LocalDate = LocalDate.of(2026, 1, 1).plusDays(nextInt(0, 30).toLong()),
        scheduledTime: LocalTime = LocalTime.of(random.nextInt(0, 24), random.nextInt(0, 60)),
        recurrenceType: RecurrenceType = RecurrenceType.DAILY,
        category: TaskCategory = TaskCategory.FITNESS,
        isActive: Boolean = true,
        createdAt: Long = 1_704_067_200_000L + nextInt(0, 10_000) * 1000L,
        reminderEnabled: Boolean = true,
        reminderLeadMinutes: Int = 10,
        reminderStyle: ReminderStyle = ReminderStyle.STANDARD,
    ): TaskEntity = TaskEntity(
        id = id,
        name = name,
        description = description,
        scheduledDay = scheduledDay,
        scheduledTime = scheduledTime,
        recurrenceType = recurrenceType,
        category = category,
        isActive = isActive,
        createdAt = createdAt,
        reminderEnabled = reminderEnabled,
        reminderLeadMinutes = reminderLeadMinutes,
        reminderStyle = reminderStyle,
        weeklyDays = emptySet(),
    )

    fun buildQuest(
        id: Long = nextLong(),
        title: String = "Quest Title ${nextInt()}",
        time: String? = "10:00",
        category: QuestCategory = QuestCategory.Fitness,
        repeatLabel: String? = "Daily",
        done: Boolean = false,
        overdue: Boolean = false,
        xp: Int = 50,
        missed: Boolean = false,
    ): QuestUi = QuestUi(
        id = id,
        title = title,
        time = time,
        category = category,
        repeatLabel = repeatLabel,
        done = done,
        overdue = overdue,
        xp = xp,
        missed = missed,
    )

    fun buildStreak(
        id: Long = 1L,
        currentStreak: Int = nextInt(1, 30),
        longestStreak: Int = nextInt(30, 100),
        lastCompletedDate: LocalDate? = LocalDate.of(2026, 1, 15),
        perfectDaysCount: Int = nextInt(5, 50),
    ): StreakEntity = StreakEntity(
        id = id,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastCompletedDate = lastCompletedDate,
        perfectDaysCount = perfectDaysCount,
    )

    fun buildUserProfile(
        id: Long = 1L,
        username: String = "FakeHero_${nextInt(100, 999)}",
        avatarId: String = "avatar_pixel_01",
        level: Int = nextInt(1, 10),
        totalXp: Int = nextInt(100, 5000),
        perfectDaysTowardNextLevel: Int = nextInt(0, 5),
        createdAt: Long = 1_704_067_200_000L,
        supabaseUserId: String? = "fake-user-uuid-${nextLong()}",
        leaderboardOptIn: Boolean = false,
        leaderboardDisplayName: String? = null,
    ): UserProfileEntity = UserProfileEntity(
        id = id,
        username = username,
        avatarId = avatarId,
        level = level,
        totalXp = totalXp,
        perfectDaysTowardNextLevel = perfectDaysTowardNextLevel,
        createdAt = createdAt,
        supabaseUserId = supabaseUserId,
        leaderboardOptIn = leaderboardOptIn,
        leaderboardDisplayName = leaderboardDisplayName,
    )

    fun buildParty(
        id: String = "party-uuid-${nextLong()}",
        name: String = "Study Circle ${nextInt()}",
        leaderId: String? = "leader-uuid-${nextLong()}",
        maxMembers: Int = 10,
        milestone: Long? = 1000L,
        milestoneSetAt: Instant? = Instant.parse("2026-01-01T00:00:00Z"),
        completedAt: Instant? = null,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        total: Long = nextInt(10, 500).toLong(),
        memberCount: Int = nextInt(2, 8),
        myStatus: MemberStatus? = MemberStatus.ACTIVE,
        isPublicRace: Boolean = false,
        type: PartyType = PartyType.DAROOD,
    ): Party = Party(
        id = id,
        name = name,
        leaderId = leaderId,
        maxMembers = maxMembers,
        milestone = milestone,
        milestoneSetAt = milestoneSetAt,
        completedAt = completedAt,
        createdAt = createdAt,
        total = total,
        memberCount = memberCount,
        myStatus = myStatus,
        isPublicRace = isPublicRace,
        type = type,
    )

    fun buildStudyItem(
        id: String = "study-item-uuid-${nextLong()}",
        partyId: String = "study-party-uuid-1",
        kind: StudyItemKind = StudyItemKind.ASSIGNMENT,
        title: String = "Assignment ${nextInt()}",
        subject: String? = "Math",
        notes: String? = "Chapter 4 Exercises",
        dueAt: Instant? = Instant.parse("2026-01-10T18:00:00Z"),
        pinned: Boolean = false,
        createdBy: String? = "user-uuid-1",
        createdAt: Instant = Instant.parse("2026-01-01T12:00:00Z"),
    ): StudyItem = StudyItem(
        id = id,
        partyId = partyId,
        kind = kind,
        title = title,
        subject = subject,
        notes = notes,
        dueAt = dueAt,
        pinned = pinned,
        createdBy = createdBy,
        createdAt = createdAt,
    )

    fun buildDaroodEntry(
        id: Long = nextLong(),
        partyId: String = "darood-party-uuid-1",
        userId: String? = "user-uuid-1",
        amount: Int = nextInt(10, 100),
        localDate: LocalDate = LocalDate.of(2026, 1, 10),
        tz: String = "UTC",
        createdAt: Long = 1_704_067_200_000L,
        clientId: String? = "client-uuid-${nextLong()}",
    ): DaroodEntryEntity = DaroodEntryEntity(
        id = id,
        partyId = partyId,
        userId = userId,
        amount = amount,
        localDate = localDate,
        tz = tz,
        createdAt = createdAt,
        clientId = clientId,
    )

    fun buildChessGame(
        id: String = "chess-game-uuid-${nextLong()}",
        partyId: String = "study-party-uuid-1",
        whiteId: String = "white-player-uuid-1",
        blackId: String = "black-player-uuid-2",
        timeControl: TimeControl = TimeControl.FIVE,
        rated: Boolean = false,
        status: GameStatus = GameStatus.ACTIVE,
        result: GameResult? = null,
        reason: ResultReason? = null,
        fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        turn: ChessColour = ChessColour.WHITE,
        whiteMs: Long = 300_000L,
        blackMs: Long = 300_000L,
        ply: Int = 0,
        lastMoveAt: Instant? = null,
        createdAt: Instant = Instant.parse("2026-01-01T10:00:00Z"),
        startedAt: Instant? = Instant.parse("2026-01-01T10:01:00Z"),
        endedAt: Instant? = null,
    ): ChessGame = ChessGame(
        id = id,
        partyId = partyId,
        whiteId = whiteId,
        blackId = blackId,
        timeControl = timeControl,
        rated = rated,
        status = status,
        result = result,
        reason = reason,
        fen = fen,
        turn = turn,
        whiteMs = whiteMs,
        blackMs = blackMs,
        ply = ply,
        lastMoveAt = lastMoveAt,
        createdAt = createdAt,
        startedAt = startedAt,
        endedAt = endedAt,
    )
}

/** Global default test builders sharing a deterministic fixed seed (42L). */
object TestDataBuilders {
    private val defaultGenerator = TestDataGenerator(42L)

    fun createSeeded(seed: Long): TestDataGenerator = TestDataGenerator(seed)

    fun buildTask(
        id: Long = defaultGenerator.nextLong(),
        name: String = "Fake Task ${defaultGenerator.nextInt()}",
        description: String = "Description for fake task",
        scheduledDay: LocalDate = LocalDate.of(2026, 1, 1),
        scheduledTime: LocalTime = LocalTime.of(12, 0),
        recurrenceType: RecurrenceType = RecurrenceType.DAILY,
        category: TaskCategory = TaskCategory.FITNESS,
        isActive: Boolean = true,
        createdAt: Long = 1_704_067_200_000L,
        reminderEnabled: Boolean = true,
        reminderLeadMinutes: Int = 10,
        reminderStyle: ReminderStyle = ReminderStyle.STANDARD,
    ): TaskEntity = defaultGenerator.buildTask(
        id = id,
        name = name,
        description = description,
        scheduledDay = scheduledDay,
        scheduledTime = scheduledTime,
        recurrenceType = recurrenceType,
        category = category,
        isActive = isActive,
        createdAt = createdAt,
        reminderEnabled = reminderEnabled,
        reminderLeadMinutes = reminderLeadMinutes,
        reminderStyle = reminderStyle,
    )

    fun buildQuest(
        id: Long = defaultGenerator.nextLong(),
        title: String = "Quest Title ${defaultGenerator.nextInt()}",
        time: String? = "10:00",
        category: QuestCategory = QuestCategory.Fitness,
        repeatLabel: String? = "Daily",
        done: Boolean = false,
        overdue: Boolean = false,
        xp: Int = 50,
        missed: Boolean = false,
    ): QuestUi = defaultGenerator.buildQuest(
        id = id,
        title = title,
        time = time,
        category = category,
        repeatLabel = repeatLabel,
        done = done,
        overdue = overdue,
        xp = xp,
        missed = missed,
    )

    fun buildStreak(
        id: Long = 1L,
        currentStreak: Int = 5,
        longestStreak: Int = 10,
        lastCompletedDate: LocalDate? = LocalDate.of(2026, 1, 15),
        perfectDaysCount: Int = 3,
    ): StreakEntity = defaultGenerator.buildStreak(
        id = id,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastCompletedDate = lastCompletedDate,
        perfectDaysCount = perfectDaysCount,
    )

    fun buildUserProfile(
        id: Long = 1L,
        username: String = "HeroUser",
        avatarId: String = "avatar_hero",
        level: Int = 1,
        totalXp: Int = 100,
        perfectDaysTowardNextLevel: Int = 1,
        createdAt: Long = 1_704_067_200_000L,
        supabaseUserId: String? = "test-uuid-user",
        leaderboardOptIn: Boolean = false,
        leaderboardDisplayName: String? = null,
    ): UserProfileEntity = defaultGenerator.buildUserProfile(
        id = id,
        username = username,
        avatarId = avatarId,
        level = level,
        totalXp = totalXp,
        perfectDaysTowardNextLevel = perfectDaysTowardNextLevel,
        createdAt = createdAt,
        supabaseUserId = supabaseUserId,
        leaderboardOptIn = leaderboardOptIn,
        leaderboardDisplayName = leaderboardDisplayName,
    )

    fun buildParty(
        id: String = "party-uuid-default",
        name: String = "Default Test Party",
        leaderId: String? = "leader-uuid-default",
        maxMembers: Int = 10,
        milestone: Long? = 1000L,
        milestoneSetAt: Instant? = Instant.parse("2026-01-01T00:00:00Z"),
        completedAt: Instant? = null,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        total: Long = 150L,
        memberCount: Int = 3,
        myStatus: MemberStatus? = MemberStatus.ACTIVE,
        isPublicRace: Boolean = false,
        type: PartyType = PartyType.DAROOD,
    ): Party = defaultGenerator.buildParty(
        id = id,
        name = name,
        leaderId = leaderId,
        maxMembers = maxMembers,
        milestone = milestone,
        milestoneSetAt = milestoneSetAt,
        completedAt = completedAt,
        createdAt = createdAt,
        total = total,
        memberCount = memberCount,
        myStatus = myStatus,
        isPublicRace = isPublicRace,
        type = type,
    )

    fun buildStudyItem(
        id: String = "study-item-uuid-default",
        partyId: String = "study-party-uuid-1",
        kind: StudyItemKind = StudyItemKind.ASSIGNMENT,
        title: String = "Default Assignment",
        subject: String? = "Math",
        notes: String? = "Notes",
        dueAt: Instant? = Instant.parse("2026-01-10T18:00:00Z"),
        pinned: Boolean = false,
        createdBy: String? = "user-uuid-1",
        createdAt: Instant = Instant.parse("2026-01-01T12:00:00Z"),
    ): StudyItem = defaultGenerator.buildStudyItem(
        id = id,
        partyId = partyId,
        kind = kind,
        title = title,
        subject = subject,
        notes = notes,
        dueAt = dueAt,
        pinned = pinned,
        createdBy = createdBy,
        createdAt = createdAt,
    )

    fun buildDaroodEntry(
        id: Long = 100L,
        partyId: String = "darood-party-uuid-1",
        userId: String? = "user-uuid-1",
        amount: Int = 33,
        localDate: LocalDate = LocalDate.of(2026, 1, 10),
        tz: String = "UTC",
        createdAt: Long = 1_704_067_200_000L,
        clientId: String? = "client-uuid-100",
    ): DaroodEntryEntity = defaultGenerator.buildDaroodEntry(
        id = id,
        partyId = partyId,
        userId = userId,
        amount = amount,
        localDate = localDate,
        tz = tz,
        createdAt = createdAt,
        clientId = clientId,
    )

    fun buildChessGame(
        id: String = "chess-game-uuid-default",
        partyId: String = "study-party-uuid-1",
        whiteId: String = "white-player-uuid-1",
        blackId: String = "black-player-uuid-2",
        timeControl: TimeControl = TimeControl.FIVE,
        rated: Boolean = false,
        status: GameStatus = GameStatus.ACTIVE,
        result: GameResult? = null,
        reason: ResultReason? = null,
        fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        turn: ChessColour = ChessColour.WHITE,
        whiteMs: Long = 300_000L,
        blackMs: Long = 300_000L,
        ply: Int = 0,
        lastMoveAt: Instant? = null,
        createdAt: Instant = Instant.parse("2026-01-01T10:00:00Z"),
        startedAt: Instant? = Instant.parse("2026-01-01T10:01:00Z"),
        endedAt: Instant? = null,
    ): ChessGame = defaultGenerator.buildChessGame(
        id = id,
        partyId = partyId,
        whiteId = whiteId,
        blackId = blackId,
        timeControl = timeControl,
        rated = rated,
        status = status,
        result = result,
        reason = reason,
        fen = fen,
        turn = turn,
        whiteMs = whiteMs,
        blackMs = blackMs,
        ply = ply,
        lastMoveAt = lastMoveAt,
        createdAt = createdAt,
        startedAt = startedAt,
        endedAt = endedAt,
    )
}
