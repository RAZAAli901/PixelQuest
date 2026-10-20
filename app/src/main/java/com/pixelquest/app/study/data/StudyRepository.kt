package com.pixelquest.app.study.data

import com.pixelquest.app.darood.data.SendReport
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.map
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.data.local.PartyDao
import com.pixelquest.app.study.data.local.PendingStudyEntity
import com.pixelquest.app.study.data.local.PendingStudyKind
import com.pixelquest.app.study.data.local.StudyCompletionEntity
import com.pixelquest.app.study.data.local.StudyDao
import com.pixelquest.app.study.data.local.StudyItemEntity
import com.pixelquest.app.study.data.local.StudySessionEntity
import com.pixelquest.app.study.data.local.StudyWeekScoreEntity
import com.pixelquest.app.study.data.local.StudyWeekWinnerEntity
import com.pixelquest.app.study.data.remote.StudyItemDraft
import com.pixelquest.app.study.data.remote.StudyRemoteDataSource
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.StudyRules
import com.pixelquest.app.study.domain.StudySession
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.domain.WeekScore
import com.pixelquest.app.study.domain.WeekWinner
import com.pixelquest.app.study.domain.weekStartOf
import com.pixelquest.app.study.sync.StudySyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A study party's own data for the screens (docs/study-party.md); its members come from the party
 * core. Screens watch the cache, which [refresh] fills from the server. Ticks and study time are saved
 * on the phone first and sent with their own keys, so a bad connection loses nothing and a retry
 * never counts twice; items, edits and undoing a tick already sent need the connection.
 */
interface StudyRepository {
    /** The signed-in account's id, or null. */
    fun myId(): String?

    /** Now, and the player's own day and week, as their completions and study time record them. */
    fun now(): Instant
    fun zone(): ZoneId
    fun today(): LocalDate
    fun thisWeek(): LocalDate = weekStartOf(today())

    /** A party's items: pinned first, then soonest due, those with no due date last. */
    fun observeItems(partyId: String): Flow<List<StudyItem>>

    fun observeItem(itemId: String): Flow<StudyItem?>

    /** Every member's completions in a party, newest first, with this phone's still waiting (PENDING or FAILED) among them. */
    fun observeCompletions(partyId: String): Flow<List<StudyCompletion>>

    /** A party's study time from [since] on, with this phone's still waiting. */
    fun observeSessions(partyId: String, since: LocalDate): Flow<List<StudySession>>

    /** Each member's points for the week starting [weekStart], highest first, with the usernames the account may see. */
    fun observeScores(partyId: String, weekStart: LocalDate): Flow<List<WeekScore>>

    /** Every week's scores the phone has, newest week first. */
    fun observeAllScores(partyId: String): Flow<List<WeekScore>>

    /** The party's recorded weekly winners, newest week first. */
    fun observeWinners(partyId: String): Flow<List<WeekWinner>>

    /** Ticks and study time still to send, across every party. */
    fun observePendingCount(): Flow<Int>

    /**
     * Brings a party's items, completions, recent study time, scores and winners from the server
     * into the cache, and closes last week once it has ended everywhere.
     */
    suspend fun refresh(partyId: String): SupabaseResult<Unit>

    /** Adds an item; only the leader may add it [pinned]. */
    suspend fun addItem(partyId: String, draft: StudyItemDraft, pinned: Boolean = false): SupabaseResult<StudyItem>

    /** Its creator or the leader changes an item. */
    suspend fun editItem(itemId: String, draft: StudyItemDraft): SupabaseResult<StudyItem>

    /** The leader pins or unpins an item. */
    suspend fun setPinned(itemId: String, pinned: Boolean): SupabaseResult<Unit>

    /** Its creator or the leader deletes an item, with every tick on it. */
    suspend fun deleteItem(itemId: String): SupabaseResult<Unit>

    /** Ticks [item] off for the account, today: saved, then sent. Ticked already, it's that tick. */
    suspend fun complete(item: StudyItem): StudyCompletion

    /**
     * Undoes the account's tick on [itemId]: one still waiting is simply dropped; one sent is taken
     * back on the server, within 24 hours of it ([StudyRefusal.NOT_ALLOWED] after).
     */
    suspend fun undo(itemId: String): SupabaseResult<Unit>

    /** Logs [minutes] of study time for today (5 to 240): saved, then sent. */
    suspend fun logStudy(partyId: String, minutes: Int): StudySession

    /** Sends everything waiting, oldest first; what the server refuses is kept, marked FAILED with its reason. */
    suspend fun sendPending(): SendReport

    /** Tries a refused tick or study time again, as it was. */
    suspend fun retry(clientId: String): SendReport

    /** Drops a refused tick or study time from the phone. */
    suspend fun discard(clientId: String)
}

@Singleton
class StudyRepositoryImpl(
    private val remote: StudyRemoteDataSource,
    private val dao: StudyDao,
    private val parties: PartyDao,
    private val account: DaroodAccount,
    private val clock: () -> Long,
    private val timeZone: () -> ZoneId,
    private val syncScheduler: StudySyncScheduler? = null
) : StudyRepository {

    @Inject
    constructor(
        remote: StudyRemoteDataSource,
        dao: StudyDao,
        parties: PartyDao,
        account: DaroodAccount,
        syncScheduler: StudySyncScheduler
    ) : this(remote, dao, parties, account, System::currentTimeMillis, ZoneId::systemDefault, syncScheduler)

    override fun myId(): String? = account.id()

    override fun now(): Instant = Instant.ofEpochMilli(clock())

    override fun zone(): ZoneId = timeZone()

    override fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    override fun observeItems(partyId: String): Flow<List<StudyItem>> = dao.observeItems(partyId).map { rows -> rows.map { it.toDomain() } }

    override fun observeItem(itemId: String): Flow<StudyItem?> = dao.observeItem(itemId).map { it?.toDomain() }

    override fun observeCompletions(partyId: String): Flow<List<StudyCompletion>> =
        combine(dao.observePending(partyId), dao.observeCompletions(partyId)) { waiting, stored ->
            val me = account.id()
            waiting.filter { it.kind == PendingStudyKind.COMPLETION }.asReversed().map { it.toCompletion(me) } + stored.map { it.toDomain() }
        }

    override fun observeSessions(partyId: String, since: LocalDate): Flow<List<StudySession>> =
        combine(dao.observePending(partyId), dao.observeSessions(partyId, since)) { waiting, stored ->
            val me = account.id()
            waiting.filter { it.kind == PendingStudyKind.SESSION && !it.localDate.isBefore(since) }.asReversed().map { it.toSession(me) } +
                stored.map { it.toDomain() }
        }

    override fun observeScores(partyId: String, weekStart: LocalDate): Flow<List<WeekScore>> =
        combine(dao.observeScores(partyId, weekStart), parties.observeMembers(partyId)) { scores, members ->
            val names = members.associate { it.userId to it.username }
            scores.map { it.toDomain(names[it.userId]) }
        }

    override fun observeAllScores(partyId: String): Flow<List<WeekScore>> =
        combine(dao.observeAllScores(partyId), parties.observeMembers(partyId)) { scores, members ->
            val names = members.associate { it.userId to it.username }
            scores.map { it.toDomain(names[it.userId]) }
        }

    override fun observeWinners(partyId: String): Flow<List<WeekWinner>> =
        combine(dao.observeWinners(partyId), parties.observeMembers(partyId)) { winners, members ->
            val names = members.associate { it.userId to it.username }
            winners.map { WeekWinner(it.partyId, it.weekStart, it.userId, names[it.userId], it.points) }
        }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun refresh(partyId: String): SupabaseResult<Unit> {
        val since = weekStartOf(today()).minusWeeks(HISTORY_WEEKS)
        val items = remote.fetchItems(partyId)
        if (items !is SupabaseResult.Success) return items.map { }
        dao.replaceItems(partyId, items.data.map { it.toEntity() })
        val completions = remote.fetchCompletions(partyId)
        if (completions !is SupabaseResult.Success) return completions.map { }
        dao.replaceCompletions(partyId, completions.data.map { it.toEntity() })
        val sessions = remote.fetchSessions(partyId, since)
        if (sessions !is SupabaseResult.Success) return sessions.map { }
        dao.replaceSessions(partyId, since, sessions.data.map { it.toEntity() })
        closeLastWeek(partyId)
        return refreshScores(partyId, since)
    }

    private suspend fun refreshScores(partyId: String, since: LocalDate = weekStartOf(today()).minusWeeks(HISTORY_WEEKS)): SupabaseResult<Unit> {
        val scores = remote.fetchScores(partyId, since)
        if (scores !is SupabaseResult.Success) return scores.map { }
        dao.replaceScores(partyId, since, scores.data.map { it.toEntity() })
        val winners = remote.fetchWinners(partyId)
        if (winners !is SupabaseResult.Success) return winners.map { }
        dao.replaceWinners(partyId, winners.data.map { StudyWeekWinnerEntity(it.partyId, it.weekStart, it.userId, it.points) })
        return SupabaseResult.Success(Unit)
    }

    /**
     * Last week's board, recorded once it has ended everywhere: the server checks, and closing it
     * again changes nothing, so any member's phone may ask (docs/study-party.md, Weeks).
     */
    private suspend fun closeLastWeek(partyId: String) {
        val lastWeek = weekStartOf(today()).minusWeeks(1)
        val endedEverywhere = !now().atZone(ZoneOffset.UTC).toLocalDate().isBefore(lastWeek.plusDays(8))
        if (endedEverywhere) remote.closeWeek(partyId, lastWeek)
    }

    override suspend fun addItem(partyId: String, draft: StudyItemDraft, pinned: Boolean): SupabaseResult<StudyItem> =
        remote.addItem(partyId, draft, pinned).also { if (it is SupabaseResult.Success) dao.upsertItemIfKept(it.data.toEntity()) }

    override suspend fun editItem(itemId: String, draft: StudyItemDraft): SupabaseResult<StudyItem> =
        remote.editItem(itemId, draft).also { if (it is SupabaseResult.Success) dao.upsertItemIfKept(it.data.toEntity()) }

    override suspend fun setPinned(itemId: String, pinned: Boolean): SupabaseResult<Unit> =
        remote.setPinned(itemId, pinned).also { result ->
            if (result is SupabaseResult.Success) dao.getItem(itemId)?.let { dao.upsertItemIfKept(it.copy(pinned = pinned)) }
        }

    override suspend fun deleteItem(itemId: String): SupabaseResult<Unit> {
        val item = dao.getItem(itemId)
        return remote.deleteItem(itemId).also { result ->
            if (result is SupabaseResult.Success) {
                dao.removeItem(itemId)
                item?.let { refreshScores(it.partyId) }
            }
        }
    }

    override suspend fun complete(item: StudyItem): StudyCompletion {
        val me = account.id()
        dao.pendingCompletionOf(item.id)?.let { return it.toCompletion(me) }
        if (me != null) dao.completionOf(item.id, me)?.let { return it.toDomain() }
        val at = clock()
        val here = zone()
        val pending = PendingStudyEntity(
            clientId = UUID.randomUUID().toString(),
            kind = PendingStudyKind.COMPLETION,
            partyId = item.partyId,
            itemId = item.id,
            localDate = Instant.ofEpochMilli(at).atZone(here).toLocalDate(),
            tz = here.id,
            createdAt = at
        )
        dao.insertPending(pending)
        // Couldn't go now: the sync worker sends it once there's a connection.
        if (sendPending().needsRetry) syncScheduler?.scheduleSend()
        return dao.pendingCompletionOf(item.id)?.toCompletion(me)
            ?: me?.let { dao.completionOf(item.id, it) }?.toDomain()
            ?: pending.toCompletion(me)
    }

    override suspend fun undo(itemId: String): SupabaseResult<Unit> {
        dao.pendingCompletionOf(itemId)?.let { waiting ->
            dao.deletePending(waiting.clientId)
            return SupabaseResult.Success(Unit)
        }
        val me = account.id() ?: return SupabaseResult.AuthError(IllegalStateException("Not signed in"), "Sign in to use study parties.")
        val done = dao.completionOf(itemId, me) ?: return SupabaseResult.Success(Unit)
        if (!done.toDomain().canUndoAt(now())) return StudyRefusal.result(StudyRefusal.NOT_ALLOWED)
        return remote.undoCompletion(done.id).also { result ->
            if (result is SupabaseResult.Success) {
                dao.deleteCompletion(done.id)
                refreshScores(done.partyId)
            }
        }
    }

    override suspend fun logStudy(partyId: String, minutes: Int): StudySession {
        require(minutes in StudyRules.MINUTES_MIN..StudyRules.MINUTES_MAX) { "Study time is 5 to 240 minutes" }
        val at = clock()
        val here = zone()
        val pending = PendingStudyEntity(
            clientId = UUID.randomUUID().toString(),
            kind = PendingStudyKind.SESSION,
            partyId = partyId,
            minutes = minutes,
            localDate = Instant.ofEpochMilli(at).atZone(here).toLocalDate(),
            tz = here.id,
            createdAt = at
        )
        dao.insertPending(pending)
        if (sendPending().needsRetry) syncScheduler?.scheduleSend()
        return dao.getPending(pending.clientId)?.toSession(account.id())
            ?: dao.sessionByClientId(pending.clientId)?.toDomain()
            ?: pending.toSession(account.id())
    }

    override suspend fun sendPending(): SendReport {
        var sent = 0
        var refused = 0
        val touched = mutableSetOf<String>()
        val waiting = dao.pendingToSend()
        for ((index, row) in waiting.withIndex()) {
            dao.countAttempt(row.clientId)
            // Sent with its own key: if a send arrived but its answer didn't, this one gets it back.
            val result: SupabaseResult<*> = when (row.kind) {
                PendingStudyKind.COMPLETION -> remote.complete(row.itemId.orEmpty(), row.localDate, row.tz, row.clientId)
                    .also { if (it is SupabaseResult.Success) dao.confirmCompletionSent(row.clientId, it.data.toEntity().copy(clientId = row.clientId)) }
                PendingStudyKind.SESSION -> remote.logSession(row.partyId, row.minutes ?: 0, row.localDate, row.tz, row.clientId)
                    .also { if (it is SupabaseResult.Success) dao.confirmSessionSent(row.clientId, it.data.toEntity().copy(clientId = row.clientId)) }
            }
            if (result is SupabaseResult.Success) {
                sent++
                touched += row.partyId
                continue
            }
            val refusal = StudyRefusal.of(result)
            if (refusal == null) {
                // No connection, or signed out: this one and the rest wait, in order.
                touched.forEach { refreshScores(it) }
                return SendReport(sent, refused, waiting.size - index)
            }
            dao.setPendingState(row.clientId, SyncState.FAILED, refusal.token.ifEmpty { (result as SupabaseResult.ServerError).message })
            refused++
        }
        touched.forEach { refreshScores(it) }
        return SendReport(sent, refused, 0)
    }

    override suspend fun retry(clientId: String): SendReport {
        val row = dao.getPending(clientId) ?: return SendReport(0, 0, 0)
        if (row.state != SyncState.FAILED) return SendReport(0, 0, 0)
        dao.setPendingState(clientId, SyncState.PENDING, null)
        return sendPending().also { if (it.needsRetry) syncScheduler?.scheduleSend() }
    }

    override suspend fun discard(clientId: String) = dao.deletePending(clientId)

    internal companion object {
        /** How many weeks back the phone keeps study time and scores, for the past weeks. */
        const val HISTORY_WEEKS = 7L
    }
}

private fun StudyItemEntity.toDomain() = StudyItem(
    id, partyId, kind, title, subject, notes, dueAt?.let(Instant::ofEpochMilli), pinned, createdBy, Instant.ofEpochMilli(createdAt)
)

private fun StudyItem.toEntity() = StudyItemEntity(
    id, partyId, kind, title, subject, notes, dueAt?.toEpochMilli(), pinned, createdBy, createdAt.toEpochMilli()
)

private fun StudyCompletionEntity.toDomain() = StudyCompletion(
    id, clientId, itemId, partyId, userId, Instant.ofEpochMilli(completedAt), localDate, tz, SyncState.SYNCED
)

private fun StudyCompletion.toEntity() = StudyCompletionEntity(
    id ?: 0, partyId, itemId, userId, completedAt.toEpochMilli(), localDate, tz, clientId
)

private fun StudySessionEntity.toDomain() = StudySession(
    id, clientId, partyId, userId, minutes, localDate, tz, Instant.ofEpochMilli(createdAt), SyncState.SYNCED
)

private fun StudySession.toEntity() = StudySessionEntity(
    id ?: 0, partyId, userId, minutes, localDate, tz, createdAt.toEpochMilli(), clientId
)

private fun WeekScore.toEntity() = StudyWeekScoreEntity(
    partyId, userId, weekStart, points, itemPoints, bonusPoints, studyPoints, itemsCompleted, studyMinutes
)

private fun StudyWeekScoreEntity.toDomain(username: String?) = WeekScore(
    partyId, userId, username, weekStart, points, itemPoints, bonusPoints, studyPoints, itemsCompleted, studyMinutes
)

private fun PendingStudyEntity.toCompletion(me: String?) = StudyCompletion(
    null, clientId, itemId.orEmpty(), partyId, me, Instant.ofEpochMilli(createdAt), localDate, tz, state
)

private fun PendingStudyEntity.toSession(me: String?) = StudySession(
    null, clientId, partyId, me, minutes ?: 0, localDate, tz, Instant.ofEpochMilli(createdAt), state
)
