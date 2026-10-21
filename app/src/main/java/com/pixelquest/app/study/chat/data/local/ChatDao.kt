package com.pixelquest.app.study.chat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pixelquest.app.study.domain.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * A study party's chat on the phone. Screens watch the Flows. Nothing is written for a party that
 * has left the phone meanwhile: its rows would point at nothing.
 */
@Dao
interface ChatDao {

    @Query("SELECT EXISTS(SELECT 1 FROM darood_party WHERE id = :partyId)")
    suspend fun hasParty(partyId: String): Boolean

    // Messages

    /** A party's newest [limit] messages, oldest first, as the chat shows them. */
    @Query("SELECT * FROM (SELECT * FROM chat_message WHERE partyId = :partyId ORDER BY createdAt DESC, id DESC LIMIT :limit) ORDER BY createdAt, id")
    fun observeMessages(partyId: String, limit: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_message WHERE id = :id")
    suspend fun getMessage(id: String): ChatMessageEntity?

    /** The oldest message the phone has of a party, to ask the server for what came before it. */
    @Query("SELECT * FROM chat_message WHERE partyId = :partyId ORDER BY createdAt, id LIMIT 1")
    suspend fun oldestMessage(partyId: String): ChatMessageEntity?

    /** The newest message the phone has of a party, to ask the server for what came after it. */
    @Query("SELECT * FROM chat_message WHERE partyId = :partyId ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun newestMessage(partyId: String): ChatMessageEntity?

    @Query("SELECT COUNT(*) FROM chat_message WHERE partyId = :partyId")
    suspend fun messageCount(partyId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<ChatMessageEntity>)

    @Transaction
    suspend fun upsertMessagesIfKept(partyId: String, messages: List<ChatMessageEntity>): Boolean {
        if (!hasParty(partyId)) return false
        upsertMessages(messages)
        return true
    }

    @Query("UPDATE chat_message SET deletedAt = :at, body = NULL, fileId = NULL WHERE id = :id")
    suspend fun markDeleted(id: String, at: Long)

    // Waiting to be sent

    @Query("SELECT * FROM chat_message_pending WHERE partyId = :partyId ORDER BY createdAt")
    fun observePending(partyId: String): Flow<List<PendingChatMessageEntity>>

    @Query("SELECT * FROM chat_message_pending WHERE state = 'PENDING' ORDER BY createdAt")
    suspend fun pendingToSend(): List<PendingChatMessageEntity>

    @Query("SELECT * FROM chat_message_pending WHERE clientId = :clientId")
    suspend fun getPending(clientId: String): PendingChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(pending: PendingChatMessageEntity)

    @Transaction
    suspend fun insertPendingIfKept(pending: PendingChatMessageEntity): Boolean {
        if (!hasParty(pending.partyId)) return false
        insertPending(pending)
        return true
    }

    @Query("UPDATE chat_message_pending SET attempts = attempts + 1 WHERE clientId = :clientId")
    suspend fun countAttempt(clientId: String)

    @Query("UPDATE chat_message_pending SET state = :state, refusal = :refusal WHERE clientId = :clientId")
    suspend fun setPendingState(clientId: String, state: SyncState, refusal: String?)

    @Query("DELETE FROM chat_message_pending WHERE clientId = :clientId")
    suspend fun deletePending(clientId: String)

    /** The server took [clientId]'s message: in the chat as it's stored, in one go. */
    @Transaction
    suspend fun confirmSent(clientId: String, message: ChatMessageEntity) {
        deletePending(clientId)
        if (hasParty(message.partyId)) upsertMessages(listOf(message))
    }

    // Files

    /** A party's files the phone has, newest first. */
    @Query("SELECT * FROM chat_file WHERE partyId = :partyId ORDER BY createdAt DESC")
    fun observeFiles(partyId: String): Flow<List<ChatFileEntity>>

    @Query("SELECT * FROM chat_file WHERE id = :id")
    suspend fun getFile(id: String): ChatFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFiles(files: List<ChatFileEntity>)

    @Transaction
    suspend fun upsertFilesIfKept(partyId: String, files: List<ChatFileEntity>): Boolean {
        if (!hasParty(partyId)) return false
        upsertFiles(files)
        return true
    }

    @Query("DELETE FROM chat_file WHERE id = :id")
    suspend fun deleteFile(id: String)

    // Uploads under way

    @Query("SELECT * FROM chat_upload WHERE partyId = :partyId ORDER BY createdAt")
    fun observeUploads(partyId: String): Flow<List<ChatUploadEntity>>

    @Query("SELECT * FROM chat_upload WHERE clientId = :clientId")
    suspend fun getUpload(clientId: String): ChatUploadEntity?

    @Query("SELECT * FROM chat_upload WHERE state IN ('QUEUED', 'UPLOADING', 'CHECKING') ORDER BY createdAt")
    suspend fun uploadsUnderWay(): List<ChatUploadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUpload(upload: ChatUploadEntity)

    @Transaction
    suspend fun insertUploadIfKept(upload: ChatUploadEntity): Boolean {
        if (!hasParty(upload.partyId)) return false
        upsertUpload(upload)
        return true
    }

    @Query("DELETE FROM chat_upload WHERE clientId = :clientId")
    suspend fun deleteUpload(clientId: String)

    // How far it's read

    @Query("SELECT lastReadAt FROM chat_read WHERE partyId = :partyId")
    fun observeLastRead(partyId: String): Flow<Long?>

    @Query("SELECT lastReadAt FROM chat_read WHERE partyId = :partyId")
    suspend fun lastRead(partyId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastRead(read: ChatReadEntity)

    /** Moves the read mark on, never back; nothing for a party not on the phone. */
    @Transaction
    suspend fun markRead(partyId: String, at: Long) {
        if (!hasParty(partyId)) return
        val now = lastRead(partyId)
        if (now == null || at > now) setLastRead(ChatReadEntity(partyId, at))
    }

    /** Unread messages per party: others' messages after the read mark, not deleted. */
    @Query(
        """SELECT m.partyId AS partyId, COUNT(*) AS unread FROM chat_message m
        LEFT JOIN chat_read r ON r.partyId = m.partyId
        WHERE m.deletedAt IS NULL AND (m.senderId IS NULL OR m.senderId != :me) AND m.kind != 'SYSTEM'
          AND m.createdAt > COALESCE(r.lastReadAt, 0)
        GROUP BY m.partyId"""
    )
    fun observeUnread(me: String): Flow<List<UnreadCount>>
}

/** A party's unread messages. */
data class UnreadCount(val partyId: String, val unread: Int)
