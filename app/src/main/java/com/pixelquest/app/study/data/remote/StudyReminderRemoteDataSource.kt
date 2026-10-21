package com.pixelquest.app.study.data.remote

import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.map
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.study.domain.StudyReminderDraft
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A study party's reminders on Supabase (docs/study-reminders.md). Row-level security decides what
 * each call may see and change (20261019000047, 20261019000048); every call is wrapped in
 * [safeSupabaseCall]. Refusals: [StudyRefusal].
 */
interface StudyReminderRemoteDataSource {
    /** The party's reminders the account may read: the party's, and its own. */
    suspend fun fetchReminders(partyId: String): SupabaseResult<List<StudyReminder>>

    /**
     * Adds a reminder as the account; the reminder as the server stored it. [clientId] is its own
     * key: sent again (an answer lost on the way back), the one stored comes back, never a second.
     */
    suspend fun addReminder(partyId: String, draft: StudyReminderDraft, clientId: String): SupabaseResult<StudyReminder>

    /** Its creator, or the leader for a party reminder, changes it; the reminder now. */
    suspend fun editReminder(reminderId: String, draft: StudyReminderDraft): SupabaseResult<StudyReminder>

    /** Its creator, or the leader for a party reminder, deletes it. */
    suspend fun deleteReminder(reminderId: String): SupabaseResult<Unit>
}

@Singleton
class SupabaseStudyReminderRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest,
    private val account: DaroodAccount
) : StudyReminderRemoteDataSource {

    private suspend fun <T> signedIn(block: suspend (String) -> T): SupabaseResult<T> {
        val me = account.id() ?: return SupabaseResult.AuthError(IllegalStateException("Not signed in"), "Sign in to use study parties.")
        return safeSupabaseCall { block(me) }
    }

    override suspend fun fetchReminders(partyId: String): SupabaseResult<List<StudyReminder>> = signedIn {
        postgrest[REMINDERS].select(Columns.list(COLUMNS)) {
            filter { eq("party_id", partyId) }
            order("created_at", Order.ASCENDING)
        }.decodeList<StudyReminderDto>().map { it.toDomain() }
    }

    override suspend fun addReminder(partyId: String, draft: StudyReminderDraft, clientId: String): SupabaseResult<StudyReminder> {
        val sent = signedIn {
            postgrest[REMINDERS].insert(draft.toNewDto(partyId, clientId)) {
                select(Columns.list(COLUMNS))
            }.decodeSingle<StudyReminderDto>().toDomain()
        }
        // 409: the server has it under this key already, its answer lost on the way: that one.
        if (!(sent is SupabaseResult.ServerError && sent.code == 409)) return sent
        return signedIn { me ->
            postgrest[REMINDERS].select(Columns.list(COLUMNS)) {
                filter {
                    eq("created_by", me)
                    eq("client_id", clientId)
                }
            }.decodeSingle<StudyReminderDto>().toDomain()
        }
    }

    override suspend fun editReminder(reminderId: String, draft: StudyReminderDraft): SupabaseResult<StudyReminder> {
        val edited = signedIn {
            postgrest[REMINDERS].update(draft.toEditDto()) {
                select(Columns.list(COLUMNS))
                filter { eq("id", reminderId) }
            }.decodeList<StudyReminderDto>()
        }
        // Row-level security quietly changes no row for anyone else: that's the refusal it is.
        return when {
            edited is SupabaseResult.Success && edited.data.isEmpty() -> StudyRefusal.result(StudyRefusal.NOT_ALLOWED)
            else -> edited.map { it.first().toDomain() }
        }
    }

    override suspend fun deleteReminder(reminderId: String): SupabaseResult<Unit> {
        val deleted = signedIn {
            postgrest[REMINDERS].delete {
                select(Columns.list("id"))
                filter { eq("id", reminderId) }
            }.decodeList<JsonObject>()
        }
        return if (deleted is SupabaseResult.Success && deleted.data.isEmpty()) StudyRefusal.result(StudyRefusal.NOT_ALLOWED) else deleted.map { }
    }

    internal companion object {
        const val REMINDERS = "study_reminders"
        val COLUMNS = listOf(
            "id", "party_id", "created_by", "title", "kind", "scope", "due_at", "week_start", "tz", "repeat", "audience", "alert_offsets", "created_at"
        )
    }
}
