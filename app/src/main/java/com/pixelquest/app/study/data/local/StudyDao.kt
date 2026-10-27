package com.pixelquest.app.study.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pixelquest.app.study.domain.SyncState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * A study party's cache. Screens watch the Flows; a refresh from the server replaces a party's
 * whole set in one transaction, so a screen never sees half of one. Nothing is written for a party
 * that has left the phone meanwhile (deleted, left or removed): its rows would point at nothing.
 */
@Dao
interface StudyDao {

    @Query("SELECT EXISTS(SELECT 1 FROM darood_party WHERE id = :partyId)")
    suspend fun hasParty(partyId: String): Boolean

    // Items

    /** A party's items: pinned first, then soonest due, those with no due date last. */
    @Query("SELECT * FROM study_item WHERE partyId = :partyId ORDER BY pinned DESC, dueAt IS NULL, dueAt, createdAt")
    fun observeItems(partyId: String): Flow<List<StudyItemEntity>>

    /**
     * Items in [partyId] that [userId] hasn't finished: pinned first, then soonest due, those with no due date last.
     * Filtered by the current user's study_completions row and pending completions, never by a global flag.
     */
    @Query(
        """SELECT * FROM study_item i WHERE i.partyId = :partyId
        AND NOT EXISTS (SELECT 1 FROM study_completion c WHERE c.itemId = i.id AND c.userId = :userId)
        AND NOT EXISTS (SELECT 1 FROM study_pending p WHERE p.itemId = i.id AND p.kind = 'COMPLETION' AND p.state = 'PENDING')
        ORDER BY i.pinned DESC, i.dueAt IS NULL, i.dueAt, i.createdAt""",
    )
    fun observeUnfinishedItems(partyId: String, userId: String): Flow<List<StudyItemEntity>>

    /**
     * Items in [partyId] that [userId] has finished: items with a study_completion for [userId]
     * or a pending completion on this device.
     */
    @Query(
        """SELECT * FROM study_item i WHERE i.partyId = :partyId
        AND (
            EXISTS (SELECT 1 FROM study_completion c WHERE c.itemId = i.id AND c.userId = :userId)
            OR EXISTS (SELECT 1 FROM study_pending p WHERE p.itemId = i.id AND p.kind = 'COMPLETION' AND p.state = 'PENDING')
        )
        ORDER BY i.dueAt DESC, i.createdAt DESC""",
    )
    fun observeCompletedItems(partyId: String, userId: String): Flow<List<StudyItemEntity>>

    @Query("SELECT * FROM study_item WHERE id = :id")
    fun observeItem(id: String): Flow<StudyItemEntity?>

    @Query("SELECT * FROM study_item WHERE id = :id")
    suspend fun getItem(id: String): StudyItemEntity?

    @Query("SELECT * FROM study_item WHERE partyId = :partyId")
    suspend fun getItemsForParty(partyId: String): List<StudyItemEntity>

    /** Every item due between [from] and [to] (epoch millis), in every party on the phone. */
    @Query("SELECT * FROM study_item WHERE dueAt >= :from AND dueAt < :to ORDER BY dueAt")
    suspend fun itemsDueBetween(from: Long, to: Long): List<StudyItemEntity>

    /**
     * What [userId] hasn't finished that's due before [until] (epoch millis), overdue too, soonest
     * first, in every party on the phone: a tick on the server, or still waiting to send, counts as done.
     */
    @Query(
        """SELECT * FROM study_item i WHERE i.dueAt IS NOT NULL AND i.dueAt < :until
        AND NOT EXISTS (SELECT 1 FROM study_completion c WHERE c.itemId = i.id AND c.userId = :userId)
        AND NOT EXISTS (SELECT 1 FROM study_pending p WHERE p.itemId = i.id AND p.kind = 'COMPLETION' AND p.state = 'PENDING')
        ORDER BY i.dueAt LIMIT :limit""",
    )
    fun observeUnfinishedDueBefore(userId: String, until: Long, limit: Int): Flow<List<StudyItemEntity>>

    /** All unfinished items with a due date for [userId] across all study parties. */
    @Query(
        """SELECT * FROM study_item i WHERE i.dueAt IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM study_completion c WHERE c.itemId = i.id AND c.userId = :userId)
        AND NOT EXISTS (SELECT 1 FROM study_pending p WHERE p.itemId = i.id AND p.kind = 'COMPLETION' AND p.state = 'PENDING')
        ORDER BY i.dueAt""",
    )
    suspend fun unfinishedItemsWithDue(userId: String): List<StudyItemEntity>

    /** Whether [userId] has marked [itemId] completed (stored or pending). */
    @Query(
        """SELECT EXISTS (
            SELECT 1 FROM study_completion WHERE itemId = :itemId AND userId = :userId
            UNION
            SELECT 1 FROM study_pending WHERE itemId = :itemId AND kind = 'COMPLETION' AND state = 'PENDING'
        )""",
    )
    suspend fun isItemCompleted(itemId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<StudyItemEntity>)

    @Query("DELETE FROM study_item WHERE partyId = :partyId")
    suspend fun deleteItems(partyId: String)

    @Query("DELETE FROM study_item WHERE id = :id")
    suspend fun deleteItem(id: String)

    /** An item gone: its ticks too, and any still waiting to be sent. */
    @Transaction
    suspend fun removeItem(id: String) {
        deleteItem(id)
        deleteCompletionsOf(id)
        deletePendingFor(id)
    }

    @Transaction
    suspend fun replaceItems(partyId: String, items: List<StudyItemEntity>): Boolean {
        if (!hasParty(partyId)) return false
        deleteItems(partyId)
        upsertItems(items)
        return true
    }

    @Transaction
    suspend fun upsertItemIfKept(item: StudyItemEntity): Boolean {
        if (!hasParty(item.partyId)) return false
        upsertItems(listOf(item))
        return true
    }

    // Completions

    @Query("SELECT * FROM study_completion WHERE partyId = :partyId ORDER BY completedAt DESC")
    fun observeCompletions(partyId: String): Flow<List<StudyCompletionEntity>>

    @Query("SELECT * FROM study_completion WHERE itemId = :itemId ORDER BY completedAt")
    fun observeCompletionsOf(itemId: String): Flow<List<StudyCompletionEntity>>

    @Query("SELECT * FROM study_completion WHERE itemId = :itemId AND userId = :userId")
    suspend fun completionOf(itemId: String, userId: String): StudyCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletions(completions: List<StudyCompletionEntity>)

    @Query("DELETE FROM study_completion WHERE partyId = :partyId")
    suspend fun deleteCompletions(partyId: String)

    @Query("DELETE FROM study_completion WHERE itemId = :itemId")
    suspend fun deleteCompletionsOf(itemId: String)

    @Query("DELETE FROM study_completion WHERE id = :id")
    suspend fun deleteCompletion(id: Long)

    @Transaction
    suspend fun replaceCompletions(partyId: String, completions: List<StudyCompletionEntity>): Boolean {
        if (!hasParty(partyId)) return false
        deleteCompletions(partyId)
        upsertCompletions(completions)
        return true
    }

    // Study time

    @Query("SELECT * FROM study_session WHERE partyId = :partyId AND localDate >= :since ORDER BY createdAt DESC")
    fun observeSessions(partyId: String, since: LocalDate): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessions(sessions: List<StudySessionEntity>)

    @Query("DELETE FROM study_session WHERE partyId = :partyId AND localDate >= :since")
    suspend fun deleteSessionsSince(partyId: String, since: LocalDate)

    /** The server's sessions from [since] on replace the phone's from then. */
    @Transaction
    suspend fun replaceSessions(partyId: String, since: LocalDate, sessions: List<StudySessionEntity>): Boolean {
        if (!hasParty(partyId)) return false
        deleteSessionsSince(partyId, since)
        upsertSessions(sessions)
        return true
    }

    // Scores and winners

    @Query("SELECT * FROM study_week_score WHERE partyId = :partyId AND weekStart = :weekStart ORDER BY points DESC")
    fun observeScores(partyId: String, weekStart: LocalDate): Flow<List<StudyWeekScoreEntity>>

    /** Every week's scores in a party, newest week first. */
    @Query("SELECT * FROM study_week_score WHERE partyId = :partyId ORDER BY weekStart DESC, points DESC")
    fun observeAllScores(partyId: String): Flow<List<StudyWeekScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScores(scores: List<StudyWeekScoreEntity>)

    @Query("DELETE FROM study_week_score WHERE partyId = :partyId AND weekStart >= :since")
    suspend fun deleteScoresSince(partyId: String, since: LocalDate)

    @Transaction
    suspend fun replaceScores(partyId: String, since: LocalDate, scores: List<StudyWeekScoreEntity>): Boolean {
        if (!hasParty(partyId)) return false
        deleteScoresSince(partyId, since)
        upsertScores(scores)
        return true
    }

    @Query("SELECT * FROM study_week_winner WHERE partyId = :partyId ORDER BY weekStart DESC")
    fun observeWinners(partyId: String): Flow<List<StudyWeekWinnerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWinners(winners: List<StudyWeekWinnerEntity>)

    @Query("DELETE FROM study_week_winner WHERE partyId = :partyId")
    suspend fun deleteWinners(partyId: String)

    @Transaction
    suspend fun replaceWinners(partyId: String, winners: List<StudyWeekWinnerEntity>): Boolean {
        if (!hasParty(partyId)) return false
        deleteWinners(partyId)
        upsertWinners(winners)
        return true
    }

    // Waiting to be sent

    // rowid rises with every insert, so it's the order they were made in, even two within the same
    // millisecond, or across a change to the phone's clock.

    /** A party's waiting and refused rows, in the order made. */
    @Query("SELECT * FROM study_pending WHERE partyId = :partyId ORDER BY rowid")
    fun observePending(partyId: String): Flow<List<PendingStudyEntity>>

    /** Everything still to send, oldest first. */
    @Query("SELECT * FROM study_pending WHERE state = 'PENDING' ORDER BY rowid")
    suspend fun pendingToSend(): List<PendingStudyEntity>

    @Query("SELECT COUNT(*) FROM study_pending WHERE state = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM study_pending WHERE clientId = :clientId")
    suspend fun getPending(clientId: String): PendingStudyEntity?

    /** The server's copy of study time logged on this phone. */
    @Query("SELECT * FROM study_session WHERE clientId = :clientId")
    suspend fun sessionByClientId(clientId: String): StudySessionEntity?

    @Query("SELECT * FROM study_pending WHERE kind = 'COMPLETION' AND itemId = :itemId")
    suspend fun pendingCompletionOf(itemId: String): PendingStudyEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPending(row: PendingStudyEntity)

    @Query("DELETE FROM study_pending WHERE clientId = :clientId")
    suspend fun deletePending(clientId: String)

    @Query("DELETE FROM study_pending WHERE itemId = :itemId")
    suspend fun deletePendingFor(itemId: String)

    @Query("UPDATE study_pending SET attempts = attempts + 1 WHERE clientId = :clientId")
    suspend fun countAttempt(clientId: String)

    @Query("UPDATE study_pending SET state = :state, refusal = :refusal WHERE clientId = :clientId")
    suspend fun setPendingState(clientId: String, state: SyncState, refusal: String?)

    /** A tick reached the server: the server's copy replaces the phone's, in one step. */
    @Transaction
    suspend fun confirmCompletionSent(clientId: String, stored: StudyCompletionEntity) {
        if (hasParty(stored.partyId)) upsertCompletions(listOf(stored))
        deletePending(clientId)
    }

    /** Study time reached the server: as [confirmCompletionSent]. */
    @Transaction
    suspend fun confirmSessionSent(clientId: String, stored: StudySessionEntity) {
        if (hasParty(stored.partyId)) upsertSessions(listOf(stored))
        deletePending(clientId)
    }
}
