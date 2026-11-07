package com.pixelquest.app.testing

import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.chess.data.ChessChallengeRepository
import com.pixelquest.app.chess.data.ChessRatingRepository
import com.pixelquest.app.chess.data.ChessRepository
import com.pixelquest.app.chess.data.SpectateRepository
import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChallengeStatus
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameEvent
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.data.DaroodRepository
import com.pixelquest.app.darood.data.SendReport
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DayTotals
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.PartyGoal
import com.pixelquest.app.darood.domain.RaceLine
import com.pixelquest.app.darood.domain.WeekResult
import com.pixelquest.app.darood.sync.PartyChange
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.PerTaskStats
import com.pixelquest.app.domain.repository.CloudProfileRepository
import com.pixelquest.app.domain.repository.HabitInsightRepository
import com.pixelquest.app.domain.repository.InsightCacheRepository
import com.pixelquest.app.domain.repository.LeaderboardRepository
import com.pixelquest.app.domain.repository.LeaderboardSortMode
import com.pixelquest.app.domain.repository.StatsRepository
import com.pixelquest.app.domain.repository.UserLeaderboardRank
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.party.data.remote.InviteOutcome
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.quran.data.BookmarkRepository
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Juz
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.domain.Translation
import com.pixelquest.app.study.chat.data.ChatSend
import com.pixelquest.app.study.chat.data.StudyChatRepository
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.data.ReminderSave
import com.pixelquest.app.study.data.StudyReminderRepository
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.data.remote.StudyItemDraft
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.study.domain.StudyReminderDraft
import com.pixelquest.app.study.domain.StudySession
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.domain.WeekScore
import com.pixelquest.app.study.domain.WeekWinner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

open class FakeAuthRepository(
    initialUser: AuthUser? = AuthUser("fake-auth-id", "hero@pixelquest.test", "Hero"),
) : AuthRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val userFlow = MutableStateFlow(initialUser)

    override val currentUser: Flow<AuthUser?> = userFlow
    override val serverUnreachable: Flow<Boolean> = flowOf(false)

    override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?): SupabaseResult<AuthUser> {
        checkSupabaseFailure<AuthUser>()?.let { return it }
        val user = AuthUser("google-auth-id", "google@pixelquest.test", "GoogleHero")
        userFlow.value = user
        return SupabaseResult.Success(user)
    }

    override suspend fun signOut(): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        userFlow.value = null
        return SupabaseResult.Success(Unit)
    }

    override suspend fun signOutEverywhere(): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        userFlow.value = null
        return SupabaseResult.Success(Unit)
    }

    override suspend fun getInitialUser(): AuthUser? {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return null
        return userFlow.value
    }
}

open class FakeCloudProfileRepository :
    CloudProfileRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val profiles = mutableMapOf<String, CloudProfileDto>()

    override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun fetchCloudProfile(userId: String): SupabaseResult<CloudProfileDto?> {
        checkSupabaseFailure<CloudProfileDto?>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(null)
        return SupabaseResult.Success(profiles[userId])
    }
}

open class FakeLeaderboardRepository :
    LeaderboardRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val rankedProfiles = MutableStateFlow<List<CloudProfileDto>>(emptyList())

    override suspend fun getProfiles(): SupabaseResult<List<CloudProfileDto>> {
        checkSupabaseFailure<List<CloudProfileDto>>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(emptyList())
        return SupabaseResult.Success(rankedProfiles.value)
    }

    override suspend fun getTopByStreak(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        checkSupabaseFailure<List<CloudProfileDto>>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(emptyList())
        return SupabaseResult.Success(
            rankedProfiles.value.sortedByDescending { it.currentStreak }.drop(offset.toInt()).take(limit.toInt()),
        )
    }

    override suspend fun getTopByLevel(limit: Long, offset: Long): SupabaseResult<List<CloudProfileDto>> {
        checkSupabaseFailure<List<CloudProfileDto>>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(emptyList())
        return SupabaseResult.Success(
            rankedProfiles.value.sortedByDescending { it.level }.drop(offset.toInt()).take(limit.toInt()),
        )
    }

    override suspend fun getCurrentUserRank(sortMode: LeaderboardSortMode, userId: String?): SupabaseResult<UserLeaderboardRank?> {
        checkSupabaseFailure<UserLeaderboardRank?>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(null)
        val profile = rankedProfiles.value.firstOrNull { it.id == userId } ?: return SupabaseResult.Success(null)
        return SupabaseResult.Success(UserLeaderboardRank(1, profile))
    }
}

open class FakeHabitInsightRepository :
    HabitInsightRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val insightFlow = MutableStateFlow<HabitInsightResponse?>(null)

    override suspend fun generateHabitInsight(forceRefresh: Boolean): GeminiResult<HabitInsightResponse> {
        checkFailure()
        val current = insightFlow.value
        return if (current != null) {
            GeminiResult.Success(current)
        } else {
            GeminiResult.ApiError(500, "No insight generated")
        }
    }

    override val latestInsight: Flow<HabitInsightResponse?> = insightFlow

    val reportedInsights = mutableListOf<Pair<String, String>>()

    override suspend fun reportInsight(content: String, reason: String): Result<Unit> {
        checkFailure()
        reportedInsights.add(content to reason)
        return Result.success(Unit)
    }
}

open class FakeInsightCacheRepository :
    InsightCacheRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val cached = MutableStateFlow<InsightCacheEntity?>(null)

    override suspend fun saveInsight(response: HabitInsightResponse, dataHash: String) {
        checkFailure()
        cached.value = InsightCacheEntity.fromInsightResponse(response, dataHash)
    }

    override suspend fun getLatestInsight(): InsightCacheEntity? {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return null
        return cached.value
    }

    override fun observeLatestInsight(): Flow<InsightCacheEntity?> = cached

    override suspend fun isCacheValid(currentDataHash: String, maxAgeMillis: Long): Boolean {
        checkFailure()
        val entry = cached.value ?: return false
        return entry.dataHash == currentDataHash && (System.currentTimeMillis() - entry.generatedAt) < maxAgeMillis
    }

    override suspend fun clearExpired(maxAgeMillis: Long): Int {
        checkFailure()
        cached.value = null
        return 1
    }

    override suspend fun clearAll(): Int {
        checkFailure()
        cached.value = null
        return 1
    }
}

open class FakeStatsRepository :
    StatsRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    var completionRate: Float = 1.0f
    var perTaskStats = PerTaskStats(
        taskId = 1L,
        completionCount = 10,
        totalScheduledCount = 10,
        completionRate = 1.0f,
        currentStreak = 10,
        longestStreak = 10,
    )

    override fun getCompletionRateOverRange(startDate: LocalDate, endDate: LocalDate): Flow<Float> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(0f)
        return flowOf(completionRate)
    }

    override fun getDailyStatusForRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, DailyStatus>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyMap())
        return flowOf(mapOf(startDate to DailyStatus.PERFECT))
    }

    override fun getPerTaskStats(taskId: Long): Flow<PerTaskStats> {
        checkFailure()
        return flowOf(perTaskStats)
    }
}

open class FakePartyRepository :
    PartyRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val partiesFlow = MutableStateFlow<List<Party>>(emptyList())
    val membersFlow = MutableStateFlow<Map<String, List<PartyMember>>>(emptyMap())

    override fun observeMyParties(): Flow<List<Party>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return partiesFlow
    }

    override fun observeParty(partyId: String): Flow<Party?> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(null)
        return partiesFlow.map { list -> list.firstOrNull { it.id == partyId } }
    }

    override fun observeMembers(partyId: String): Flow<List<PartyMember>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return membersFlow.map { it[partyId] ?: emptyList() }
    }

    override suspend fun refreshParties(): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun refreshParty(partyId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome> {
        checkSupabaseFailure<InviteOutcome>()?.let { return it }
        return SupabaseResult.Success(InviteOutcome.SENT)
    }

    override suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus> {
        checkSupabaseFailure<MemberStatus>()?.let { return it }
        return SupabaseResult.Success(if (accept) MemberStatus.ACTIVE else MemberStatus.DECLINED)
    }

    override suspend fun leaveParty(partyId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun renameParty(partyId: String, name: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        partiesFlow.value = partiesFlow.value.map { if (it.id == partyId) it.copy(name = name) else it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun transferLeadership(partyId: String, userId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        partiesFlow.value = partiesFlow.value.map { if (it.id == partyId) it.copy(leaderId = userId) else it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun deleteParty(partyId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        partiesFlow.value = partiesFlow.value.filterNot { it.id == partyId }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun createParty(name: String, maxMembers: Int, type: PartyType): SupabaseResult<String> {
        checkSupabaseFailure<String>()?.let { return it }
        val id = "party-${partiesFlow.value.size + 1}"
        val party = Party(
            id = id,
            name = name,
            leaderId = "me",
            maxMembers = maxMembers,
            milestone = 1000L,
            milestoneSetAt = Instant.now(),
            completedAt = null,
            createdAt = Instant.now(),
            type = type,
        )
        partiesFlow.value = partiesFlow.value + party
        return SupabaseResult.Success(id)
    }

    override fun followInvitations(): Flow<Unit> = flowOf(Unit)
}

open class FakeDaroodRepository :
    DaroodRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val entriesFlow = MutableStateFlow<Map<String, List<DaroodEntry>>>(emptyMap())
    val memberStatsFlow = MutableStateFlow<Map<String, List<MemberStats>>>(emptyMap())
    var fixedToday: LocalDate = LocalDate.of(2026, 1, 1)

    override fun observeMemberStats(partyId: String): Flow<List<MemberStats>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return memberStatsFlow.map { it[partyId] ?: emptyList() }
    }

    override fun observeEntries(partyId: String, limit: Int): Flow<List<DaroodEntry>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return entriesFlow.map { (it[partyId] ?: emptyList()).take(limit) }
    }

    override fun today(): LocalDate = fixedToday

    override suspend fun fetchDayTotals(partyId: String, day: LocalDate): SupabaseResult<DayTotals> {
        checkSupabaseFailure<DayTotals>()?.let { return it }
        return SupabaseResult.Success(DayTotals(100L, 50L))
    }

    override suspend fun fetchRaceBoard(): SupabaseResult<List<RaceLine>> {
        checkSupabaseFailure<List<RaceLine>>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(emptyList())
        return SupabaseResult.Success(emptyList())
    }

    override suspend fun fetchMyDays(partyId: String): SupabaseResult<Map<LocalDate, Long>> {
        checkSupabaseFailure<Map<LocalDate, Long>>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(emptyMap())
        return SupabaseResult.Success(mapOf(fixedToday to 50L))
    }

    override suspend fun closeLastWeek(partyId: String): SupabaseResult<WeekResult?> {
        checkSupabaseFailure<WeekResult?>()?.let { return it }
        return SupabaseResult.Success(null)
    }

    override suspend fun fetchGoals(partyId: String): SupabaseResult<List<PartyGoal>> {
        checkSupabaseFailure<List<PartyGoal>>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(emptyList())
        return SupabaseResult.Success(emptyList())
    }

    override suspend fun markWeekTold(partyId: String, weekStart: LocalDate): Boolean = true
    override fun observeWeekResults(partyId: String): Flow<List<WeekResult>> = flowOf(emptyList())
    override suspend fun fetchWeekResults(partyId: String, weeks: Int): SupabaseResult<List<WeekResult>> = SupabaseResult.Success(emptyList())
    override fun observePendingTotalOn(partyId: String, day: LocalDate): Flow<Long> = flowOf(0L)
    override fun observePendingCount(): Flow<Int> = flowOf(0)
    override suspend fun addEntry(partyId: String, amount: Int): DaroodEntry = DaroodEntry(
        id = 1L,
        clientId = "client-1",
        partyId = partyId,
        userId = "me",
        amount = amount,
        localDate = fixedToday,
        tz = "UTC",
        createdAt = Instant.now(),
        state = EntryState.SYNCED,
    )
    override suspend fun sendPending(): SendReport = SendReport(0, 0, 0)
    override fun followParty(partyId: String): Flow<PartyChange> = flowOf()
    override suspend fun retryEntry(clientId: String): SendReport = SendReport(0, 0, 0)
    override suspend fun loadOlderEntries(partyId: String): SupabaseResult<Boolean> = SupabaseResult.Success(false)
    override suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String> = SupabaseResult.Success("darood-party-1")
    override suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long> = SupabaseResult.Success(target)
    override suspend fun setRaceBoard(partyId: String, on: Boolean): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
}

open class FakeDaroodPartyRepository(
    val partyRepo: FakePartyRepository = FakePartyRepository(),
    val daroodRepo: FakeDaroodRepository = FakeDaroodRepository(),
) : DaroodPartyRepository,
    PartyRepository by partyRepo,
    DaroodRepository by daroodRepo,
    FailureInjectable {
    override var failureType: FakeFailureType
        get() = partyRepo.failureType
        set(value) {
            partyRepo.failureType = value
            daroodRepo.failureType = value
        }
}

open class FakeStudyRepository :
    StudyRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val itemsFlow = MutableStateFlow<Map<String, List<StudyItem>>>(emptyMap())
    val completionsFlow = MutableStateFlow<Map<String, List<StudyCompletion>>>(emptyMap())
    val sessionsFlow = MutableStateFlow<Map<String, List<StudySession>>>(emptyMap())

    override fun myId(): String? = "study-user-id"
    override fun now(): Instant = Instant.parse("2026-01-01T12:00:00Z")
    override fun zone(): ZoneId = ZoneId.of("UTC")
    override fun today(): LocalDate = LocalDate.of(2026, 1, 1)

    override fun observeItems(partyId: String): Flow<List<StudyItem>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return itemsFlow.map { it[partyId] ?: emptyList() }
    }

    override fun observeItem(itemId: String): Flow<StudyItem?> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(null)
        return itemsFlow.map { all -> all.values.flatten().firstOrNull { it.id == itemId } }
    }

    override suspend fun getItem(id: String): StudyItem? {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return null
        return itemsFlow.value.values.flatten().firstOrNull { it.id == id }
    }

    override fun observeDueSoon(until: Instant, limit: Int): Flow<List<StudyItem>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return itemsFlow.map { all -> all.values.flatten().filter { it.dueAt != null && it.dueAt <= until }.take(limit) }
    }

    override fun observeCompletions(partyId: String): Flow<List<StudyCompletion>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return completionsFlow.map { it[partyId] ?: emptyList() }
    }

    override fun observeSessions(partyId: String, since: LocalDate): Flow<List<StudySession>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return sessionsFlow.map { it[partyId] ?: emptyList() }
    }

    override fun observeScores(partyId: String, weekStart: LocalDate): Flow<List<WeekScore>> = flowOf(emptyList())
    override fun observeAllScores(partyId: String): Flow<List<WeekScore>> = flowOf(emptyList())
    override fun observeWinners(partyId: String): Flow<List<WeekWinner>> = flowOf(emptyList())
    override fun observePendingCount(): Flow<Int> = flowOf(0)
    override suspend fun refresh(partyId: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    override suspend fun addItem(partyId: String, draft: StudyItemDraft, pinned: Boolean): SupabaseResult<StudyItem> =
        SupabaseResult.Success(
            StudyItem(
                id = "item-1",
                partyId = partyId,
                kind = draft.kind,
                title = draft.title,
                subject = draft.subject,
                notes = draft.notes,
                dueAt = draft.dueAt,
                pinned = pinned,
                createdBy = "me",
                createdAt = Instant.now(),
            ),
        )
    override suspend fun editItem(itemId: String, draft: StudyItemDraft): SupabaseResult<StudyItem> =
        SupabaseResult.Success(
            StudyItem(
                id = itemId,
                partyId = "p1",
                kind = draft.kind,
                title = draft.title,
                subject = draft.subject,
                notes = draft.notes,
                dueAt = draft.dueAt,
                pinned = false,
                createdBy = "me",
                createdAt = Instant.now(),
            ),
        )
    override suspend fun setPinned(itemId: String, pinned: Boolean): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    override suspend fun deleteItem(itemId: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    override suspend fun complete(item: StudyItem): StudyCompletion = StudyCompletion(
        id = 1L,
        clientId = "client-comp-1",
        itemId = item.id,
        partyId = item.partyId,
        userId = "me",
        localDate = today(),
        tz = "UTC",
        completedAt = Instant.now(),
        state = SyncState.SYNCED,
    )
    override suspend fun undo(itemId: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    override suspend fun logStudy(partyId: String, minutes: Int): StudySession = StudySession(
        id = 1L,
        clientId = "client-sess-1",
        partyId = partyId,
        userId = "me",
        minutes = minutes,
        localDate = today(),
        tz = "UTC",
        createdAt = Instant.now(),
        state = SyncState.SYNCED,
    )
    override suspend fun sendPending(): SendReport = SendReport(0, 0, 0)
    override suspend fun retry(clientId: String): SendReport = SendReport(0, 0, 0)
    override suspend fun discard(clientId: String) {}
}

open class FakeStudyReminderRepository :
    StudyReminderRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val remindersFlow = MutableStateFlow<Map<String, List<StudyReminder>>>(emptyMap())

    override fun observeReminders(partyId: String): Flow<List<StudyReminder>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return remindersFlow.map { it[partyId] ?: emptyList() }
    }

    override suspend fun allReminders(): List<StudyReminder> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return emptyList()
        return remindersFlow.value.values.flatten()
    }

    override suspend fun refresh(partyId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun refreshAll(): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun create(partyId: String, draft: StudyReminderDraft): ReminderSave {
        checkFailure()
        return ReminderSave.Sent
    }

    override suspend fun edit(reminder: StudyReminder, draft: StudyReminderDraft): ReminderSave {
        checkFailure()
        return ReminderSave.Sent
    }

    override suspend fun delete(reminder: StudyReminder): ReminderSave {
        checkFailure()
        return ReminderSave.Sent
    }

    override suspend fun sendPending(): SendReport {
        checkFailure()
        return SendReport(0, 0, 0)
    }

    override suspend fun discard(clientId: String) {
        checkFailure()
    }
}

open class FakeStudyChatRepository :
    StudyChatRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val messagesFlow = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    override fun observeMessages(partyId: String, limit: Int): Flow<List<ChatMessage>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return messagesFlow.map { (it[partyId] ?: emptyList()).take(limit) }
    }

    override fun observeFiles(partyId: String): Flow<List<ChatFile>> = flowOf(emptyList())
    override fun observeLastRead(partyId: String): Flow<Instant?> = flowOf(null)
    override fun observeUnread(): Flow<Map<String, Int>> = flowOf(emptyMap())

    override suspend fun refresh(partyId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun loadOlder(partyId: String): SupabaseResult<Boolean> {
        checkSupabaseFailure<Boolean>()?.let { return it }
        return SupabaseResult.Success(false)
    }

    override suspend fun send(partyId: String, text: String, replyTo: String?): ChatSend {
        checkFailure()
        return ChatSend.Sent
    }

    override suspend fun sendPending(): SendReport {
        checkFailure()
        return SendReport(0, 0, 0)
    }

    override suspend fun retry(clientId: String): ChatSend = ChatSend.Sent
    override suspend fun discard(clientId: String) {}
    override suspend fun delete(messageId: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    override suspend fun markRead(partyId: String) {}
}

open class FakeChessRepository :
    ChessRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val games = mutableMapOf<String, ChessGame>()

    override suspend fun fetchGame(gameId: String): SupabaseResult<Pair<ChessGame, List<ChessMove>>> {
        checkSupabaseFailure<Pair<ChessGame, List<ChessMove>>>()?.let { return it }
        val game = games[gameId] ?: return SupabaseResult.ServerError(404, "Game not found")
        return SupabaseResult.Success(game to emptyList())
    }

    override suspend fun myLiveGame(): SupabaseResult<ChessGame?> {
        checkSupabaseFailure<ChessGame?>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(null)
        return SupabaseResult.Success(games.values.firstOrNull { it.status.live })
    }

    override suspend fun makeMove(gameId: String, ply: Int, uci: String): SupabaseResult<ChessGame> {
        checkSupabaseFailure<ChessGame>()?.let { return it }
        val game = games[gameId] ?: return SupabaseResult.ServerError(404, "Game not found")
        return SupabaseResult.Success(game.copy(ply = ply))
    }

    override suspend fun resign(gameId: String): SupabaseResult<ChessGame> {
        checkSupabaseFailure<ChessGame>()?.let { return it }
        val game = games[gameId] ?: return SupabaseResult.ServerError(404, "Game not found")
        return SupabaseResult.Success(game)
    }

    override suspend fun offerDraw(gameId: String): SupabaseResult<ChessGame> {
        checkSupabaseFailure<ChessGame>()?.let { return it }
        val game = games[gameId] ?: return SupabaseResult.ServerError(404, "Game not found")
        return SupabaseResult.Success(game)
    }

    override suspend fun answerDraw(gameId: String, accept: Boolean): SupabaseResult<ChessGame> {
        checkSupabaseFailure<ChessGame>()?.let { return it }
        val game = games[gameId] ?: return SupabaseResult.ServerError(404, "Game not found")
        return SupabaseResult.Success(game)
    }

    override suspend fun claimTimeout(gameId: String): SupabaseResult<ChessGame> {
        checkSupabaseFailure<ChessGame>()?.let { return it }
        val game = games[gameId] ?: return SupabaseResult.ServerError(404, "Game not found")
        return SupabaseResult.Success(game)
    }

    override fun follow(gameId: String): Flow<GameEvent> = flowOf()
}

open class FakeChessChallengeRepository :
    ChessChallengeRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val challengesFlow = MutableStateFlow<Map<String, List<Challenge>>>(emptyMap())

    override suspend fun challenge(partyId: String, toUser: String?, timeControl: TimeControl, colour: ColourChoice, rated: Boolean): SupabaseResult<Challenge> {
        checkSupabaseFailure<Challenge>()?.let { return it }
        val challenge = Challenge(
            id = "challenge-1",
            partyId = partyId,
            fromUser = "me",
            toUser = toUser,
            timeControl = timeControl,
            colour = colour,
            rated = rated,
            status = ChallengeStatus.OPEN,
            expiresAt = Instant.now().plusSeconds(600),
            createdAt = Instant.now(),
        )
        return SupabaseResult.Success(challenge)
    }

    override suspend fun respond(challengeId: String, accept: Boolean): SupabaseResult<ChessGame?> {
        checkSupabaseFailure<ChessGame?>()?.let { return it }
        return SupabaseResult.Success(null)
    }

    override suspend fun cancel(challengeId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }

    override suspend fun fetchChallenge(challengeId: String): SupabaseResult<Challenge?> {
        checkSupabaseFailure<Challenge?>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(null)
        return SupabaseResult.Success(null)
    }

    override fun observeChallenges(partyId: String): Flow<List<Challenge>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return challengesFlow.map { it[partyId] ?: emptyList() }
    }

    override suspend fun refreshChallenges(partyId: String): SupabaseResult<Unit> {
        checkSupabaseFailure<Unit>()?.let { return it }
        return SupabaseResult.Success(Unit)
    }
}

open class FakeChessRatingRepository :
    ChessRatingRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    override fun observeRatings(userId: String): Flow<List<Rating>> = flowOf(emptyList())
    override suspend fun refreshRatings(userId: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
    override suspend fun headToHead(other: String): SupabaseResult<List<HeadToHead>> = SupabaseResult.Success(emptyList())
    override suspend fun leaderboard(partyId: String, timeControl: TimeControl?): SupabaseResult<List<Rating>> = SupabaseResult.Success(emptyList())
}

open class FakeSpectateRepository :
    SpectateRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    override suspend fun liveGames(partyId: String): SupabaseResult<List<ChessGame>> {
        checkSupabaseFailure<List<ChessGame>>()?.let { return it }
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return SupabaseResult.Success(emptyList())
        return SupabaseResult.Success(emptyList())
    }
    override fun watch(gameId: String): Flow<GameEvent> = flowOf()
}

open class FakeQuranRepository :
    QuranRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    override val isReady: Flow<Boolean> = flowOf(true)

    override fun surahs(): Flow<List<Surah>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return flowOf(emptyList())
    }

    override suspend fun surah(number: Int): Surah? {
        checkFailure()
        return null
    }

    override fun juzs(): Flow<List<Juz>> = flowOf(emptyList())
    override suspend fun ayahs(surah: Int, page: Int, pageSize: Int): List<Ayah> = emptyList()
    override suspend fun ayahRange(surah: Int, from: Int, to: Int): List<Ayah> = emptyList()
    override suspend fun ayah(surah: Int, number: Int): Ayah? = null
    override suspend fun searchTranslation(text: String, limit: Int): List<Ayah> = emptyList()
    override suspend fun searchArabic(text: String, limit: Int): List<Ayah> = emptyList()
    override fun translations(): Flow<List<Translation>> = flowOf(emptyList())
}

open class FakeBookmarkRepository :
    BookmarkRepository,
    FailureInjectable {
    override var failureType: FakeFailureType = FakeFailureType.NONE
    val bookmarks = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())

    override fun bookmarkedIn(surah: Int): Flow<Set<Int>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptySet())
        return bookmarks.map { set -> set.filter { it.first == surah }.map { it.second }.toSet() }
    }

    override fun all(): Flow<List<Pair<Int, Int>>> {
        checkFailure()
        if (failureType == FakeFailureType.EMPTY_RESPONSE) return flowOf(emptyList())
        return bookmarks.map { it.toList() }
    }

    override suspend fun toggle(surah: Int, ayah: Int): Boolean {
        checkFailure()
        val pair = surah to ayah
        val current = bookmarks.value
        return if (pair in current) {
            bookmarks.value = current - pair
            false
        } else {
            bookmarks.value = current + pair
            true
        }
    }
}
