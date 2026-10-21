package com.pixelquest.app.study.data

import com.pixelquest.app.darood.data.SendReport
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.map
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.study.data.local.PendingReminderOp
import com.pixelquest.app.study.data.local.PendingStudyReminderEntity
import com.pixelquest.app.study.data.local.StudyReminderDao
import com.pixelquest.app.study.data.local.StudyReminderEntity
import com.pixelquest.app.study.data.remote.StudyReminderRemoteDataSource
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderProblem
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.study.domain.StudyReminderDraft
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.domain.problem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** How saving a reminder went. */
sealed interface ReminderSave {
    /** On the server. */
    data object Sent : ReminderSave

    /** Saved on the phone; it goes when there's a connection. */
    data object Waiting : ReminderSave

    /** Breaks one of the server's rules: nothing was saved. */
    data class Invalid(val problem: ReminderProblem) : ReminderSave

    /** The server said no; it stays on the phone, FAILED, until discarded. */
    data class Refused(val refusal: StudyRefusal) : ReminderSave
}

/**
 * A study party's reminders for the screens and the alarms (docs/study-reminders.md, On the phone).
 * They're kept on the phone, so they show and alert offline. A reminder made, changed or deleted
 * here is saved first and sent after, a new one with its own key so a retry never stores it twice.
 */
interface StudyReminderRepository {
    /** A party's reminders as the phone has them, with its own changes still waiting applied. */
    fun observeReminders(partyId: String): Flow<List<StudyReminder>>

    /** Every reminder the server has sent to this phone, in every party: what the alarms are armed from. */
    suspend fun allReminders(): List<StudyReminder>

    /** Brings a party's reminders from the server into the phone, after sending what's waiting. */
    suspend fun refresh(partyId: String): SupabaseResult<Unit>

    /** [refresh] for every study party the account is an active member of on this phone. */
    suspend fun refreshAll(): SupabaseResult<Unit>

    suspend fun create(partyId: String, draft: StudyReminderDraft): ReminderSave

    /** Changes [reminder]: its party, creator and audience stay as they are. */
    suspend fun edit(reminder: StudyReminder, draft: StudyReminderDraft): ReminderSave

    suspend fun delete(reminder: StudyReminder): ReminderSave

    /** Sends everything waiting, oldest first; what the server refuses is kept, FAILED, with its reason. */
    suspend fun sendPending(): SendReport

    /** Drops a refused change from the phone. */
    suspend fun discard(clientId: String)
}

@Singleton
class StudyReminderRepositoryImpl(
    private val remote: StudyReminderRemoteDataSource,
    private val dao: StudyReminderDao,
    private val account: DaroodAccount,
    private val clock: () -> Long
) : StudyReminderRepository {
    @Inject
    constructor(remote: StudyReminderRemoteDataSource, dao: StudyReminderDao, account: DaroodAccount) :
        this(remote, dao, account, System::currentTimeMillis)

    /** One send at a time: two at once would send the same change twice. */
    private val sending = Mutex()

    override fun observeReminders(partyId: String): Flow<List<StudyReminder>> =
        combine(dao.observeReminders(partyId), dao.observePending(partyId)) { stored, waiting ->
            val me = account.id()
            val deleted = waiting.filter { it.op == PendingReminderOp.DELETE }.mapNotNull { it.reminderId }.toSet()
            val edits = waiting.filter { it.op == PendingReminderOp.EDIT }.associateBy { it.reminderId }
            stored.filter { it.id !in deleted }.map { row ->
                val edit = edits[row.id]
                if (edit == null) row.toDomain() else row.toDomain().withDraft(edit.toDraft(), edit.state)
            } + waiting.filter { it.op == PendingReminderOp.CREATE }.map { it.toNewReminder(me) }
        }

    override suspend fun allReminders(): List<StudyReminder> = dao.allReminders().map { it.toDomain() }

    override suspend fun refresh(partyId: String): SupabaseResult<Unit> {
        sendPending()
        val fetched = remote.fetchReminders(partyId)
        if (fetched is SupabaseResult.Success) dao.replaceReminders(partyId, fetched.data.map { it.toEntity() })
        return fetched.map { }
    }

    override suspend fun refreshAll(): SupabaseResult<Unit> {
        var result: SupabaseResult<Unit> = SupabaseResult.Success(Unit)
        for (partyId in dao.activeStudyParties()) {
            val one = refresh(partyId)
            if (one !is SupabaseResult.Success && result is SupabaseResult.Success) result = one
        }
        return result
    }

    override suspend fun create(partyId: String, draft: StudyReminderDraft): ReminderSave {
        draft.problem()?.let { return ReminderSave.Invalid(it) }
        val pending = draft.toPending(UUID.randomUUID().toString(), PendingReminderOp.CREATE, partyId, reminderId = null)
        if (!dao.insertPendingIfKept(pending)) return ReminderSave.Refused(StudyRefusal.NOT_A_MEMBER)
        return sendAndReport(pending.clientId)
    }

    override suspend fun edit(reminder: StudyReminder, draft: StudyReminderDraft): ReminderSave {
        val kept = draft.copy(audience = reminder.audience)
        kept.problem()?.let { return ReminderSave.Invalid(it) }
        val waiting = dao.getPending(reminder.id)
        if (waiting?.op == PendingReminderOp.CREATE) {
            // Not on the server yet: the new one simply goes as changed.
            dao.insertPending(kept.toPending(waiting.clientId, PendingReminderOp.CREATE, waiting.partyId, null).copy(createdAt = waiting.createdAt))
            return sendAndReport(waiting.clientId)
        }
        val pending = kept.toPending(UUID.randomUUID().toString(), PendingReminderOp.EDIT, reminder.partyId, reminder.id)
        if (!dao.replacePendingFor(reminder.id, pending)) return ReminderSave.Refused(StudyRefusal.NOT_A_MEMBER)
        return sendAndReport(pending.clientId)
    }

    override suspend fun delete(reminder: StudyReminder): ReminderSave {
        val waiting = dao.getPending(reminder.id)
        if (waiting?.op == PendingReminderOp.CREATE) {
            // Never sent: nothing to take back.
            dao.deletePending(waiting.clientId)
            return ReminderSave.Sent
        }
        val pending = PendingStudyReminderEntity(
            clientId = UUID.randomUUID().toString(), op = PendingReminderOp.DELETE, partyId = reminder.partyId,
            reminderId = reminder.id, createdAt = clock()
        )
        if (!dao.replacePendingFor(reminder.id, pending)) return ReminderSave.Refused(StudyRefusal.NOT_A_MEMBER)
        return sendAndReport(pending.clientId)
    }

    private suspend fun sendAndReport(clientId: String): ReminderSave {
        sendPending()
        val left = dao.getPendingByKey(clientId) ?: return ReminderSave.Sent
        return if (left.state == SyncState.FAILED) {
            ReminderSave.Refused(StudyRefusal.entries.firstOrNull { it.token.isNotEmpty() && it.token == left.refusal } ?: StudyRefusal.INVALID)
        } else {
            ReminderSave.Waiting
        }
    }

    override suspend fun sendPending(): SendReport = sending.withLock {
        var sent = 0
        var refused = 0
        val waiting = dao.pendingToSend()
        for ((index, row) in waiting.withIndex()) {
            dao.countAttempt(row.clientId)
            val result: SupabaseResult<*> = when (row.op) {
                PendingReminderOp.CREATE -> remote.addReminder(row.partyId, row.toDraft(), row.clientId)
                    .also { if (it is SupabaseResult.Success) dao.confirmSent(row.clientId, it.data.toEntity()) }
                PendingReminderOp.EDIT -> remote.editReminder(row.reminderId.orEmpty(), row.toDraft())
                    .also { if (it is SupabaseResult.Success) dao.confirmSent(row.clientId, it.data.toEntity()) }
                PendingReminderOp.DELETE -> remote.deleteReminder(row.reminderId.orEmpty())
                    .also { if (it is SupabaseResult.Success) dao.confirmDeleted(row.clientId, row.reminderId.orEmpty()) }
            }
            if (result is SupabaseResult.Success) {
                sent++
                continue
            }
            val refusal = StudyRefusal.of(result)
            // No connection, or signed out: this one and the rest wait, in order.
            if (refusal == null) return@withLock SendReport(sent, refused, waiting.size - index)
            dao.markFailed(row.clientId, refusal.token.ifEmpty { (result as SupabaseResult.ServerError).message })
            refused++
        }
        SendReport(sent, refused, 0)
    }

    override suspend fun discard(clientId: String) = dao.deletePending(clientId)

    private fun StudyReminderDraft.toPending(clientId: String, op: PendingReminderOp, partyId: String, reminderId: String?) =
        PendingStudyReminderEntity(
            clientId = clientId, op = op, partyId = partyId, reminderId = reminderId, title = title.trim(), kind = kind,
            scope = scope, dueAt = dueAt?.toEpochMilli(), weekStart = weekStart, tz = tz, repeat = repeat, audience = audience,
            alertOffsets = alertOffsets?.joinToString(","), createdAt = clock()
        )
}

private fun String.toOffsets(): List<Int> = split(',').mapNotNull { it.trim().toIntOrNull() }

internal fun StudyReminderEntity.toDomain() = StudyReminder(
    id = id, partyId = partyId, createdBy = createdBy, title = title, kind = kind, scope = scope,
    dueAt = dueAt?.let(Instant::ofEpochMilli), weekStart = weekStart, tz = tz, repeat = repeat, audience = audience,
    alertOffsets = alertOffsets?.toOffsets(), createdAt = Instant.ofEpochMilli(createdAt)
)

internal fun StudyReminder.toEntity() = StudyReminderEntity(
    id = id, partyId = partyId, createdBy = createdBy, title = title, kind = kind, scope = scope,
    dueAt = dueAt?.toEpochMilli(), weekStart = weekStart, tz = tz, repeat = repeat, audience = audience,
    alertOffsets = alertOffsets?.joinToString(","), createdAt = createdAt.toEpochMilli()
)

private fun PendingStudyReminderEntity.toDraft() = StudyReminderDraft(
    title = title.orEmpty(), kind = kind ?: ReminderKind.CUSTOM, scope = scope ?: ReminderScope.TIME,
    dueAt = dueAt?.let(Instant::ofEpochMilli), weekStart = weekStart, tz = tz.orEmpty(), repeat = repeat ?: ReminderRepeat.NONE,
    audience = audience ?: ReminderAudience.PARTY, alertOffsets = alertOffsets?.toOffsets()
)

private fun PendingStudyReminderEntity.toNewReminder(me: String?): StudyReminder {
    val draft = toDraft()
    return StudyReminder(
        id = clientId, partyId = partyId, createdBy = me, title = draft.title, kind = draft.kind, scope = draft.scope,
        dueAt = draft.dueAt, weekStart = draft.weekStart, tz = draft.tz, repeat = draft.repeat, audience = draft.audience,
        alertOffsets = draft.alertOffsets, createdAt = Instant.ofEpochMilli(createdAt), sync = state
    )
}

private fun StudyReminder.withDraft(draft: StudyReminderDraft, state: SyncState) = copy(
    title = draft.title, kind = draft.kind, scope = draft.scope, dueAt = draft.dueAt, weekStart = draft.weekStart,
    tz = draft.tz, repeat = draft.repeat, alertOffsets = draft.alertOffsets, sync = state
)
