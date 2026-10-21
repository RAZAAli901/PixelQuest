package com.pixelquest.app.study.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Study reminders on the phone. A refresh replaces a party's reminders in one transaction; nothing
 * is written for a party that has left the phone meanwhile.
 */
@Dao
interface StudyReminderDao {

    @Query("SELECT EXISTS(SELECT 1 FROM darood_party WHERE id = :partyId)")
    suspend fun hasParty(partyId: String): Boolean

    /** The study parties on the phone the account is an active member of: whose reminders it reads. */
    @Query("SELECT id FROM darood_party WHERE partyType = 'STUDY' AND myStatus = 'ACTIVE'")
    suspend fun activeStudyParties(): List<String>

    // The server's reminders

    /** A party's reminders: weeks first by their Monday, then by moment, oldest made first. */
    @Query("SELECT * FROM study_reminder WHERE partyId = :partyId ORDER BY weekStart IS NULL, weekStart, dueAt, createdAt")
    fun observeReminders(partyId: String): Flow<List<StudyReminderEntity>>

    @Query("SELECT * FROM study_reminder WHERE id = :id")
    suspend fun getReminder(id: String): StudyReminderEntity?

    /** Every reminder on the phone, in every party: what the alarms are armed from. */
    @Query("SELECT * FROM study_reminder")
    suspend fun allReminders(): List<StudyReminderEntity>

    @Query("SELECT * FROM study_reminder")
    fun observeAllReminders(): Flow<List<StudyReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(reminders: List<StudyReminderEntity>)

    @Query("DELETE FROM study_reminder WHERE partyId = :partyId")
    suspend fun deleteReminders(partyId: String)

    @Query("DELETE FROM study_reminder WHERE id = :id")
    suspend fun deleteReminder(id: String)

    @Transaction
    suspend fun replaceReminders(partyId: String, reminders: List<StudyReminderEntity>): Boolean {
        if (!hasParty(partyId)) return false
        deleteReminders(partyId)
        upsertReminders(reminders)
        return true
    }

    @Transaction
    suspend fun upsertReminderIfKept(reminder: StudyReminderEntity): Boolean {
        if (!hasParty(reminder.partyId)) return false
        upsertReminders(listOf(reminder))
        return true
    }

    // Waiting to be sent

    @Query("SELECT * FROM study_reminder_pending WHERE partyId = :partyId ORDER BY createdAt")
    fun observePending(partyId: String): Flow<List<PendingStudyReminderEntity>>

    @Query("SELECT * FROM study_reminder_pending WHERE state = 'PENDING' ORDER BY createdAt")
    suspend fun pendingToSend(): List<PendingStudyReminderEntity>

    @Query("SELECT * FROM study_reminder_pending ORDER BY createdAt")
    suspend fun allPending(): List<PendingStudyReminderEntity>

    @Query("SELECT * FROM study_reminder_pending ORDER BY createdAt")
    fun observeAllPending(): Flow<List<PendingStudyReminderEntity>>

    @Transaction
    suspend fun insertPendingIfKept(pending: PendingStudyReminderEntity): Boolean {
        if (!hasParty(pending.partyId)) return false
        insertPending(pending)
        return true
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(pending: PendingStudyReminderEntity)

    /** The change waiting for [id]: a reminder's own (by its id), or a new one (by its key). */
    @Query("SELECT * FROM study_reminder_pending WHERE reminderId = :id OR (reminderId IS NULL AND clientId = :id) LIMIT 1")
    suspend fun getPending(id: String): PendingStudyReminderEntity?

    @Query("SELECT * FROM study_reminder_pending WHERE clientId = :clientId")
    suspend fun getPendingByKey(clientId: String): PendingStudyReminderEntity?

    @Query("DELETE FROM study_reminder_pending WHERE reminderId = :reminderId")
    suspend fun deletePendingFor(reminderId: String)

    /** A reminder's newest change replaces any still waiting: only the last one matters. */
    @Transaction
    suspend fun replacePendingFor(reminderId: String, pending: PendingStudyReminderEntity): Boolean {
        if (!hasParty(pending.partyId)) return false
        deletePendingFor(reminderId)
        insertPending(pending)
        return true
    }

    /** The server took [clientId]'s change: the reminder as it now is, in one go. */
    @Transaction
    suspend fun confirmSent(clientId: String, reminder: StudyReminderEntity) {
        deletePending(clientId)
        if (hasParty(reminder.partyId)) upsertReminders(listOf(reminder))
    }

    @Transaction
    suspend fun confirmDeleted(clientId: String, reminderId: String) {
        deletePending(clientId)
        deleteReminder(reminderId)
    }

    @Query("UPDATE study_reminder_pending SET attempts = attempts + 1 WHERE clientId = :clientId")
    suspend fun countAttempt(clientId: String)

    @Query("UPDATE study_reminder_pending SET state = 'FAILED', refusal = :refusal WHERE clientId = :clientId")
    suspend fun markFailed(clientId: String, refusal: String?)

    @Query("DELETE FROM study_reminder_pending WHERE clientId = :clientId")
    suspend fun deletePending(clientId: String)

    // Done on this phone

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markDone(done: StudyReminderDoneEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM study_reminder_done WHERE reminderId = :reminderId AND occurrenceAt = :occurrenceAt)")
    suspend fun isDone(reminderId: String, occurrenceAt: Long): Boolean

    /** Forgets done marks for occurrences before [before] (epoch millis): they can't alert again. */
    @Query("DELETE FROM study_reminder_done WHERE occurrenceAt < :before")
    suspend fun pruneDone(before: Long)
}
