package com.pixelquest.app.testing

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.study.data.remote.StudyReminderRemoteDataSource
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.study.domain.StudyReminderDraft
import com.pixelquest.app.study.domain.problem
import java.io.IOException
import java.time.Instant
import java.util.UUID

/**
 * Supabase's study_reminders as 20261019000045 to 049 make them, in memory: the party's are read by
 * its members, a member's own by them alone; only the creator, or the leader for a party one,
 * changes or deletes it; a key stores a reminder once. [online] false is no connection;
 * [loseNextAnswer] stores the next add but loses its answer on the way back.
 */
class FakeStudyReminderServer(var me: String, var now: Instant = Instant.parse("2026-10-21T10:00:00Z")) : StudyReminderRemoteDataSource {
    val rows = mutableListOf<StudyReminder>()
    private val keys = mutableMapOf<Pair<String, String>, String>()
    val leaders = mutableMapOf<String, String>()
    var online = true
    var loseNextAnswer = false
    var calls = 0
        private set

    private fun offline() = SupabaseResult.NetworkError(IOException("offline"))

    override suspend fun fetchReminders(partyId: String): SupabaseResult<List<StudyReminder>> {
        calls++
        if (!online) return offline()
        return SupabaseResult.Success(rows.filter { it.partyId == partyId && (it.audience == ReminderAudience.PARTY || it.createdBy == me) })
    }

    override suspend fun addReminder(partyId: String, draft: StudyReminderDraft, clientId: String): SupabaseResult<StudyReminder> {
        calls++
        if (!online) return offline()
        if (draft.problem() != null) return SupabaseResult.ServerError(400, "check_violation")
        // Stored under this key already (the data source reads it back after the 409): that one.
        keys[me to clientId]?.let { id -> return SupabaseResult.Success(rows.first { it.id == id }) }
        if (rows.count { it.partyId == partyId && it.createdBy == me } >= 100) return SupabaseResult.ServerError(400, StudyRefusal.TOO_MANY_REMINDERS.token)
        val row = StudyReminder(
            id = UUID.randomUUID().toString(), partyId = partyId, createdBy = me, title = draft.title.trim(), kind = draft.kind,
            scope = draft.scope, dueAt = draft.dueAt, weekStart = draft.weekStart, tz = draft.tz, repeat = draft.repeat,
            audience = draft.audience, alertOffsets = draft.alertOffsets, createdAt = now
        )
        rows += row
        keys[me to clientId] = row.id
        if (loseNextAnswer) {
            loseNextAnswer = false
            return offline()
        }
        return SupabaseResult.Success(row)
    }

    private fun mayChange(row: StudyReminder) =
        row.createdBy == me || (row.audience == ReminderAudience.PARTY && leaders[row.partyId] == me)

    override suspend fun editReminder(reminderId: String, draft: StudyReminderDraft): SupabaseResult<StudyReminder> {
        calls++
        if (!online) return offline()
        val index = rows.indexOfFirst { it.id == reminderId && mayChange(it) }
        if (index < 0) return StudyRefusal.result(StudyRefusal.NOT_ALLOWED)
        val edited = rows[index].copy(
            title = draft.title.trim(), kind = draft.kind, scope = draft.scope, dueAt = draft.dueAt, weekStart = draft.weekStart,
            tz = draft.tz, repeat = draft.repeat, alertOffsets = draft.alertOffsets
        )
        rows[index] = edited
        return SupabaseResult.Success(edited)
    }

    override suspend fun deleteReminder(reminderId: String): SupabaseResult<Unit> {
        calls++
        if (!online) return offline()
        if (!rows.removeIf { it.id == reminderId && mayChange(it) }) return StudyRefusal.result(StudyRefusal.NOT_ALLOWED)
        return SupabaseResult.Success(Unit)
    }

    /** A reminder someone else made, straight on the server. */
    fun seed(partyId: String, by: String, draft: StudyReminderDraft): StudyReminder {
        val row = StudyReminder(
            id = UUID.randomUUID().toString(), partyId = partyId, createdBy = by, title = draft.title, kind = draft.kind,
            scope = draft.scope, dueAt = draft.dueAt, weekStart = draft.weekStart, tz = draft.tz, repeat = draft.repeat,
            audience = draft.audience, alertOffsets = draft.alertOffsets, createdAt = now
        )
        rows += row
        return row
    }
}
