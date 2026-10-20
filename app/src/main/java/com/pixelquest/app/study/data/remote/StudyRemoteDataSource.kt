package com.pixelquest.app.study.data.remote

import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.map
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.StudySession
import com.pixelquest.app.study.domain.WeekScore
import com.pixelquest.app.study.domain.WeekWinner
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** An item's fields as a member writes them; the server fills in the rest. */
data class StudyItemDraft(
    val kind: StudyItemKind,
    val title: String,
    val subject: String?,
    val notes: String?,
    val dueAt: Instant?
)

/**
 * A study party's own calls to Supabase (docs/study-party.md); its members go through the party
 * core's [com.pixelquest.app.party.data.remote.PartyRemoteDataSource]. Row-level security decides
 * what each call may see (supabase/schema.md, Study parties); every call is wrapped in
 * [safeSupabaseCall]. Refusals: [StudyRefusal].
 */
interface StudyRemoteDataSource {
    /** A party's items, soonest due first, those with no due date last. */
    suspend fun fetchItems(partyId: String): SupabaseResult<List<StudyItem>>

    /** Adds an item as the account; the item as the server stored it. Only the leader may add it [pinned]. */
    suspend fun addItem(partyId: String, draft: StudyItemDraft, pinned: Boolean = false): SupabaseResult<StudyItem>

    /** Its creator or the leader changes an item; the item now. */
    suspend fun editItem(itemId: String, draft: StudyItemDraft): SupabaseResult<StudyItem>

    /** The leader pins or unpins an item. */
    suspend fun setPinned(itemId: String, pinned: Boolean): SupabaseResult<Unit>

    /** Its creator or the leader deletes an item, and every tick on it. */
    suspend fun deleteItem(itemId: String): SupabaseResult<Unit>

    /** Every member's completions in a party, newest first. */
    suspend fun fetchCompletions(partyId: String): SupabaseResult<List<StudyCompletion>>

    /**
     * Ticks an item off as the account; the completion as the server stored it. [clientId] is its
     * own key: sent again (an answer lost on the way back), the one stored comes back, never a
     * second; ticked already from another phone, that one comes back.
     */
    suspend fun complete(itemId: String, localDate: LocalDate, tz: String, clientId: String): SupabaseResult<StudyCompletion>

    /** Undoes the account's own completion, within 24 hours of it; after that, [StudyRefusal.NOT_ALLOWED]. */
    suspend fun undoCompletion(completionId: Long): SupabaseResult<Unit>

    /** A party's study sessions from [since] on (every member's), newest first. */
    suspend fun fetchSessions(partyId: String, since: LocalDate): SupabaseResult<List<StudySession>>

    /** Logs study time as the account; the session as stored. [clientId] keeps it from counting twice, as [complete]'s does. */
    suspend fun logSession(partyId: String, minutes: Int, localDate: LocalDate, tz: String, clientId: String): SupabaseResult<StudySession>

    /** Each member's points in a party for the weeks from [since] on, without usernames. */
    suspend fun fetchScores(partyId: String, since: LocalDate): SupabaseResult<List<WeekScore>>

    /** A party's recorded weekly winners, newest week first. */
    suspend fun fetchWinners(partyId: String): SupabaseResult<List<WeekWinner>>

    /** Asks for the week starting [weekStart] (a Monday) to be closed: its winners, closed now or before. */
    suspend fun closeWeek(partyId: String, weekStart: LocalDate): SupabaseResult<List<WeekWinner>>
}

@Singleton
class SupabaseStudyRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest,
    private val account: DaroodAccount
) : StudyRemoteDataSource {

    private fun signedOut() = SupabaseResult.AuthError(IllegalStateException("Not signed in"), "Sign in to use study parties.")

    private suspend fun <T> signedIn(block: suspend (String) -> T): SupabaseResult<T> {
        val me = account.id() ?: return signedOut()
        return safeSupabaseCall { block(me) }
    }

    override suspend fun fetchItems(partyId: String): SupabaseResult<List<StudyItem>> = signedIn {
        postgrest[ITEMS].select(Columns.list(ITEM_COLUMNS)) {
            filter { eq("party_id", partyId) }
            order("due_at", Order.ASCENDING, nullsFirst = false)
            order("created_at", Order.ASCENDING)
        }.decodeList<StudyItemDto>().map { it.toDomain() }
    }

    override suspend fun addItem(partyId: String, draft: StudyItemDraft, pinned: Boolean): SupabaseResult<StudyItem> = signedIn {
        val row = NewStudyItemDto(
            partyId = partyId,
            kind = draft.kind.wire,
            title = draft.title.trim(),
            subject = draft.subject?.trim()?.ifEmpty { null },
            notes = draft.notes?.trim()?.ifEmpty { null },
            dueAt = draft.dueAt?.toString(),
            pinned = pinned
        )
        postgrest[ITEMS].insert(row) { select(Columns.list(ITEM_COLUMNS)) }.decodeSingle<StudyItemDto>().toDomain()
    }

    override suspend fun editItem(itemId: String, draft: StudyItemDraft): SupabaseResult<StudyItem> {
        val edited = signedIn {
            postgrest[ITEMS].update({
                set("kind", draft.kind.wire)
                set("title", draft.title.trim())
                set("subject", draft.subject?.trim()?.ifEmpty { null })
                set("notes", draft.notes?.trim()?.ifEmpty { null })
                set("due_at", draft.dueAt?.toString())
            }) {
                select(Columns.list(ITEM_COLUMNS))
                filter { eq("id", itemId) }
            }.decodeList<StudyItemDto>()
        }
        // Row-level security lets only the creator's or the leader's change through; anyone else's
        // changes no row, without an error, so that's read as the refusal it is.
        return when {
            edited is SupabaseResult.Success && edited.data.isEmpty() -> StudyRefusal.result(StudyRefusal.NOT_ALLOWED)
            else -> edited.map { it.first().toDomain() }
        }
    }

    override suspend fun setPinned(itemId: String, pinned: Boolean): SupabaseResult<Unit> =
        changedOne(signedIn {
            postgrest[ITEMS].update({ set("pinned", pinned) }) {
                select(Columns.list("id"))
                filter { eq("id", itemId) }
            }.decodeList<JsonObject>()
        })

    override suspend fun deleteItem(itemId: String): SupabaseResult<Unit> =
        changedOne(signedIn {
            postgrest[ITEMS].delete {
                select(Columns.list("id"))
                filter { eq("id", itemId) }
            }.decodeList<JsonObject>()
        })

    override suspend fun fetchCompletions(partyId: String): SupabaseResult<List<StudyCompletion>> = signedIn {
        postgrest[COMPLETIONS].select(Columns.list(COMPLETION_COLUMNS)) {
            filter { eq("party_id", partyId) }
            order("completed_at", Order.DESCENDING)
        }.decodeList<StudyCompletionDto>().map { it.toDomain() }
    }

    override suspend fun complete(itemId: String, localDate: LocalDate, tz: String, clientId: String): SupabaseResult<StudyCompletion> {
        val sent = signedIn {
            postgrest[COMPLETIONS].insert(NewStudyCompletionDto(itemId, localDate.toString(), tz, clientId)) {
                select(Columns.list(COMPLETION_COLUMNS))
            }.decodeSingle<StudyCompletionDto>().toDomain()
        }
        if (!sent.isDuplicate()) return sent
        // Sent before, or ticked from another phone: one completion per member per item, so it's that one.
        return signedIn { me ->
            postgrest[COMPLETIONS].select(Columns.list(COMPLETION_COLUMNS)) {
                filter {
                    eq("item_id", itemId)
                    eq("user_id", me)
                }
            }.decodeSingle<StudyCompletionDto>().toDomain()
        }
    }

    override suspend fun undoCompletion(completionId: Long): SupabaseResult<Unit> =
        changedOne(signedIn { me ->
            postgrest[COMPLETIONS].delete {
                select(Columns.list("id"))
                filter {
                    eq("id", completionId)
                    eq("user_id", me)
                }
            }.decodeList<JsonObject>()
        })

    override suspend fun fetchSessions(partyId: String, since: LocalDate): SupabaseResult<List<StudySession>> = signedIn {
        postgrest[SESSIONS].select(Columns.list(SESSION_COLUMNS)) {
            filter {
                eq("party_id", partyId)
                gte("local_date", since.toString())
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<StudySessionDto>().map { it.toDomain() }
    }

    override suspend fun logSession(partyId: String, minutes: Int, localDate: LocalDate, tz: String, clientId: String): SupabaseResult<StudySession> {
        val sent = signedIn {
            postgrest[SESSIONS].insert(NewStudySessionDto(partyId, minutes, localDate.toString(), tz, clientId)) {
                select(Columns.list(SESSION_COLUMNS))
            }.decodeSingle<StudySessionDto>().toDomain()
        }
        if (!sent.isDuplicate()) return sent
        // Sent before, its answer lost: the one stored under its key.
        return signedIn { me ->
            postgrest[SESSIONS].select(Columns.list(SESSION_COLUMNS)) {
                filter {
                    eq("user_id", me)
                    eq("client_id", clientId)
                }
            }.decodeSingle<StudySessionDto>().toDomain()
        }
    }

    override suspend fun fetchScores(partyId: String, since: LocalDate): SupabaseResult<List<WeekScore>> = signedIn {
        postgrest[SCORES].select(Columns.list(SCORE_COLUMNS)) {
            filter {
                eq("party_id", partyId)
                gte("week_start", since.toString())
            }
            order("week_start", Order.DESCENDING)
            order("points", Order.DESCENDING)
        }.decodeList<WeekScoreDto>().map { it.toDomain() }
    }

    override suspend fun fetchWinners(partyId: String): SupabaseResult<List<WeekWinner>> = signedIn {
        postgrest[WINNERS].select(Columns.list("party_id", "week_start", "user_id", "points")) {
            filter { eq("party_id", partyId) }
            order("week_start", Order.DESCENDING)
        }.decodeList<WeekWinnerDto>().map { it.toDomain() }
    }

    override suspend fun closeWeek(partyId: String, weekStart: LocalDate): SupabaseResult<List<WeekWinner>> = signedIn {
        postgrest.rpc("close_study_week", buildJsonObject {
            put("party_id", partyId)
            put("week_start", weekStart.toString())
        }).decodeAs<CloseWeekReply>().winners.map { WeekWinner(partyId, weekStart, it.userId, null, it.points) }
    }

    /** A write that row-level security may quietly let nothing through: no row is [StudyRefusal.NOT_ALLOWED]. */
    private fun changedOne(result: SupabaseResult<List<JsonObject>>): SupabaseResult<Unit> =
        if (result is SupabaseResult.Success && result.data.isEmpty()) StudyRefusal.result(StudyRefusal.NOT_ALLOWED) else result.map { }

    /** The server already holds this row (a unique key): 409, as PostgREST answers a unique violation. */
    private fun SupabaseResult<*>.isDuplicate() = this is SupabaseResult.ServerError && code == 409

    internal companion object {
        const val ITEMS = "study_items"
        const val COMPLETIONS = "study_completions"
        const val SESSIONS = "study_sessions"
        const val SCORES = "study_week_scores"
        const val WINNERS = "study_week_winners"
        val ITEM_COLUMNS = listOf("id", "party_id", "kind", "title", "subject", "notes", "due_at", "pinned", "created_by", "created_at")
        val COMPLETION_COLUMNS = listOf("id", "item_id", "party_id", "user_id", "completed_at", "local_date", "tz", "client_id")
        val SESSION_COLUMNS = listOf("id", "party_id", "user_id", "minutes", "local_date", "tz", "client_id", "created_at")
        val SCORE_COLUMNS = listOf(
            "party_id", "user_id", "week_start", "points", "item_points", "bonus_points", "study_points", "items_completed", "study_minutes"
        )
    }
}
