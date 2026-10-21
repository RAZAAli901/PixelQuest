package com.pixelquest.app.testing

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.study.data.remote.StudyItemDraft
import com.pixelquest.app.study.data.remote.StudyRemoteDataSource
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.StudyPointsConfig
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.StudyRules
import com.pixelquest.app.study.domain.StudySession
import com.pixelquest.app.study.domain.WeekScore
import com.pixelquest.app.study.domain.WeekWinner
import com.pixelquest.app.study.domain.weekStartOf
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A study party's server in memory, keeping its main rules (docs/study-party.md): members only,
 * the creator or leader edits, the leader pins, one tick per member per item, undo within 24 hours,
 * keys that never count twice, the points, and closing a week. Who's in which party, who's signed
 * in and the time come from [parties] (its [FakeDaroodServer.me], [FakeDaroodServer.offline],
 * [FakeDaroodServer.now]).
 */
class FakeStudyServer(val parties: FakeDaroodServer) : StudyRemoteDataSource {

    /** The next tick or study time reaches the server, but its answer is lost on the way back. */
    var loseNextAnswer = false

    val items = linkedMapOf<String, StudyItem>()
    val completions = mutableListOf<StudyCompletion>()
    val sessions = mutableListOf<StudySession>()
    val winners = mutableListOf<WeekWinner>()
    val calls = mutableListOf<String>()
    private val closed = mutableSetOf<Pair<String, LocalDate>>()
    private var nextItem = 1
    private var nextRow = 1L

    // One request at a time, as the real server keeps its tables consistent: the app asks for a
    // party's items, completions, study time and board at once, from several threads.
    private fun <T> answer(name: String, block: (String) -> SupabaseResult<T>): SupabaseResult<T> = synchronized(this) {
        calls += name
        if (parties.offline) return SupabaseResult.NetworkError(IOException("offline"))
        val account = parties.me ?: return SupabaseResult.AuthError(IllegalStateException("signed out"))
        return block(account)
    }

    private fun refuse(refusal: StudyRefusal, code: Int = 400): SupabaseResult<Nothing> = SupabaseResult.ServerError(code, refusal.token)

    private fun isMember(party: String, user: String) =
        parties.parties[party]?.partyType == "study" && parties.members.any { it.partyId == party && it.userId == user && it.status == "active" }

    private fun isLeader(party: String, user: String) = parties.parties[party]?.leaderId == user

    private fun dayOk(day: LocalDate, tz: String): Boolean {
        val today = parties.now.atZone(java.time.ZoneId.of(tz)).toLocalDate()
        return !day.isAfter(today) && !day.isBefore(today.minusDays(StudyRules.DAYS_BACK_MAX))
    }

    /** An item as if a member added it on the server. */
    fun seedItem(party: String, by: String, title: String, kind: StudyItemKind = StudyItemKind.ASSIGNMENT, dueAt: java.time.Instant? = null, pinned: Boolean = false): StudyItem =
        StudyItem("item-${nextItem++}", party, kind, title, null, null, dueAt, pinned, by, parties.now).also { items[it.id] = it }

    /** A tick as if [user] made it on their phone. */
    fun seedCompletion(item: String, user: String, day: LocalDate = parties.now.atZone(ZoneOffset.UTC).toLocalDate()): StudyCompletion =
        StudyCompletion(nextRow++, null, item, items.getValue(item).partyId, user, parties.now, day, "UTC").also { completions += it }

    /** Study time as if [user] logged it on their phone. */
    fun seedSession(party: String, user: String, minutes: Int, day: LocalDate = parties.now.atZone(ZoneOffset.UTC).toLocalDate()): StudySession =
        StudySession(nextRow++, null, party, user, minutes, day, "UTC", parties.now).also { sessions += it }

    override suspend fun fetchItems(partyId: String) = answer("fetchItems") { me ->
        if (!isMember(partyId, me)) return@answer SupabaseResult.Success(emptyList())
        SupabaseResult.Success(items.values.filter { it.partyId == partyId }.sortedWith(compareBy({ it.dueAt == null }, { it.dueAt }, { it.createdAt })))
    }

    override suspend fun addItem(partyId: String, draft: StudyItemDraft, pinned: Boolean) = answer("addItem") { me ->
        when {
            !isMember(partyId, me) -> refuse(StudyRefusal.INVALID, 403)
            pinned && !isLeader(partyId, me) -> refuse(StudyRefusal.NOT_LEADER, 403)
            draft.title.trim().length !in StudyRules.TITLE_MIN..StudyRules.TITLE_MAX -> refuse(StudyRefusal.INVALID)
            draft.kind == StudyItemKind.QUIZ && draft.dueAt == null -> refuse(StudyRefusal.INVALID)
            else -> {
                val item = StudyItem(
                    "item-${nextItem++}", partyId, draft.kind, draft.title.trim(), draft.subject?.trim()?.ifEmpty { null },
                    draft.notes?.trim()?.ifEmpty { null }, draft.dueAt, pinned, me, parties.now
                )
                items[item.id] = item
                SupabaseResult.Success(item)
            }
        }
    }

    private fun mayChange(item: StudyItem?, me: String) = item != null && isMember(item.partyId, me) && (item.createdBy == me || isLeader(item.partyId, me))

    override suspend fun editItem(itemId: String, draft: StudyItemDraft) = answer("editItem") { me ->
        val item = items[itemId]
        when {
            !mayChange(item, me) -> refuse(StudyRefusal.NOT_ALLOWED, 403)
            draft.kind == StudyItemKind.QUIZ && draft.dueAt == null -> refuse(StudyRefusal.INVALID)
            else -> {
                val edited = item!!.copy(kind = draft.kind, title = draft.title.trim(), subject = draft.subject?.trim()?.ifEmpty { null },
                    notes = draft.notes?.trim()?.ifEmpty { null }, dueAt = draft.dueAt)
                items[itemId] = edited
                SupabaseResult.Success(edited)
            }
        }
    }

    override suspend fun setPinned(itemId: String, pinned: Boolean) = answer("setPinned") { me ->
        val item = items[itemId]
        when {
            !mayChange(item, me) -> refuse(StudyRefusal.NOT_ALLOWED, 403)
            !isLeader(item!!.partyId, me) -> refuse(StudyRefusal.NOT_LEADER, 403)
            else -> SupabaseResult.Success(Unit).also { items[itemId] = item.copy(pinned = pinned) }
        }
    }

    override suspend fun deleteItem(itemId: String) = answer("deleteItem") { me ->
        if (!mayChange(items[itemId], me)) return@answer refuse(StudyRefusal.NOT_ALLOWED, 403)
        items.remove(itemId)
        completions.removeAll { it.itemId == itemId }
        SupabaseResult.Success(Unit)
    }

    override suspend fun fetchCompletions(partyId: String) = answer("fetchCompletions") { me ->
        SupabaseResult.Success(if (isMember(partyId, me)) completions.filter { it.partyId == partyId }.sortedByDescending { it.completedAt } else emptyList())
    }

    override suspend fun complete(itemId: String, localDate: LocalDate, tz: String, clientId: String) = answer("complete") { me ->
        val item = items[itemId] ?: return@answer refuse(StudyRefusal.NO_SUCH_ITEM)
        // The same key, or another phone's tick: the one stored comes back, as the data source reads it.
        completions.firstOrNull { it.itemId == itemId && it.userId == me }?.let { return@answer SupabaseResult.Success(it) }
        when {
            !isMember(item.partyId, me) -> refuse(StudyRefusal.INVALID, 403)
            !dayOk(localDate, tz) -> refuse(StudyRefusal.BAD_DAY)
            else -> {
                val done = StudyCompletion(nextRow++, clientId, itemId, item.partyId, me, parties.now, localDate, tz)
                completions += done
                if (loseNextAnswer) {
                    loseNextAnswer = false
                    SupabaseResult.NetworkError(IOException("answer lost"))
                } else {
                    SupabaseResult.Success(done)
                }
            }
        }
    }

    override suspend fun undoCompletion(completionId: Long) = answer("undoCompletion") { me ->
        val done = completions.firstOrNull { it.id == completionId && it.userId == me && it.canUndoAt(parties.now) && isMember(it.partyId, me) }
            ?: return@answer refuse(StudyRefusal.NOT_ALLOWED, 403)
        completions.remove(done)
        SupabaseResult.Success(Unit)
    }

    override suspend fun fetchSessions(partyId: String, since: LocalDate) = answer("fetchSessions") { me ->
        SupabaseResult.Success(
            if (isMember(partyId, me)) sessions.filter { it.partyId == partyId && !it.localDate.isBefore(since) }.sortedByDescending { it.createdAt } else emptyList()
        )
    }

    override suspend fun logSession(partyId: String, minutes: Int, localDate: LocalDate, tz: String, clientId: String) = answer("logSession") { me ->
        sessions.firstOrNull { it.userId == me && it.clientId == clientId }?.let { return@answer SupabaseResult.Success(it) }
        when {
            !isMember(partyId, me) -> refuse(StudyRefusal.INVALID, 403)
            minutes !in StudyRules.MINUTES_MIN..StudyRules.MINUTES_MAX -> refuse(StudyRefusal.INVALID)
            !dayOk(localDate, tz) -> refuse(StudyRefusal.BAD_DAY)
            else -> {
                val session = StudySession(nextRow++, clientId, partyId, me, minutes, localDate, tz, parties.now)
                sessions += session
                if (loseNextAnswer) {
                    loseNextAnswer = false
                    SupabaseResult.NetworkError(IOException("answer lost"))
                } else {
                    SupabaseResult.Success(session)
                }
            }
        }
    }

    /** Every member's points by week, as study_week_scores counts them. */
    fun scores(partyId: String): List<WeekScore> {
        data class Key(val user: String, val week: LocalDate)
        val ticks = completions.filter { it.partyId == partyId }.groupBy { Key(it.userId!!, weekStartOf(it.localDate)) }
        val studied = sessions.filter { it.partyId == partyId }.groupBy { Key(it.userId!!, weekStartOf(it.localDate)) }
        return (ticks.keys + studied.keys).map { key ->
            val done = ticks[key].orEmpty()
            val itemPoints = done.sumOf { StudyPointsConfig.pointsFor(items[it.itemId]?.kind ?: StudyItemKind.ASSIGNMENT) }
            val bonus = done.sumOf { StudyPointsConfig.earlyBonus(items[it.itemId]?.dueAt, it.completedAt) }
            val days = studied[key].orEmpty().groupBy { it.localDate }.values.map { day -> day.sumOf { it.minutes } }
            val studyPoints = days.sumOf { StudyPointsConfig.studyPoints(it) }
            WeekScore(partyId, key.user, null, key.week, itemPoints + bonus + studyPoints, itemPoints, bonus, studyPoints, done.size, days.sum())
        }
    }

    override suspend fun fetchScores(partyId: String, since: LocalDate) = answer("fetchScores") { me ->
        SupabaseResult.Success(
            if (isMember(partyId, me)) scores(partyId).filter { !it.weekStart.isBefore(since) }.sortedWith(compareByDescending<WeekScore> { it.weekStart }.thenByDescending { it.points }) else emptyList()
        )
    }

    override suspend fun fetchWinners(partyId: String) = answer("fetchWinners") { me ->
        SupabaseResult.Success(if (isMember(partyId, me)) winners.filter { it.partyId == partyId }.sortedByDescending { it.weekStart } else emptyList())
    }

    override suspend fun closeWeek(partyId: String, weekStart: LocalDate) = answer("closeWeek") { me ->
        when {
            !isMember(partyId, me) -> refuse(StudyRefusal.NOT_A_MEMBER, 403)
            weekStartOf(weekStart) != weekStart -> refuse(StudyRefusal.BAD_WEEK)
            parties.now.atZone(ZoneOffset.UTC).toLocalDate().isBefore(weekStart.plusDays(8)) -> refuse(StudyRefusal.WEEK_NOT_OVER)
            else -> {
                if (closed.add(partyId to weekStart)) {
                    val week = scores(partyId).filter { it.weekStart == weekStart }
                    val top = week.maxOfOrNull { it.points } ?: 0
                    if (top > 0) week.filter { it.points == top }.forEach { winners += WeekWinner(partyId, weekStart, it.userId, null, it.points) }
                }
                SupabaseResult.Success(winners.filter { it.partyId == partyId && it.weekStart == weekStart })
            }
        }
    }
}
