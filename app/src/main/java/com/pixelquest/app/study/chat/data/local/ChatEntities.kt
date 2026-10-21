package com.pixelquest.app.study.chat.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.domain.SyncState

// A study party's chat on the phone (docs/study-chat.md, On the phone), from version 20: the
// server's messages and files, messages waiting to be sent, the uploads under way, and how far the
// player has read. Every row goes with its party. Times are epoch millis.

/** A message the server has. */
@Entity(
    tableName = "chat_message",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId", "createdAt"), Index("clientId")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val senderId: String?,
    val kind: ChatMessageKind,
    val body: String?,
    val fileId: String?,
    val replyTo: String?,
    val clientId: String?,
    val createdAt: Long,
    val deletedAt: Long?
)

/**
 * A text written on this phone, not yet on the server: saved first, then sent with its own key
 * ([clientId]), so a retry never stores it twice. PENDING until sent, FAILED if refused ([refusal]).
 */
@Entity(
    tableName = "chat_message_pending",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId")]
)
data class PendingChatMessageEntity(
    @PrimaryKey val clientId: String,
    val partyId: String,
    val body: String,
    val replyTo: String?,
    val createdAt: Long,
    val state: SyncState = SyncState.PENDING,
    @ColumnInfo(defaultValue = "0") val attempts: Int = 0,
    val refusal: String? = null
)

/** A shared file the server has, ready, or (the player's own) pending or rejected. */
@Entity(
    tableName = "chat_file",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId", "createdAt")]
)
data class ChatFileEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val uploaderId: String?,
    val displayName: String,
    val mime: String,
    val sizeBytes: Long,
    val sha256: String?,
    val status: ChatFileStatus,
    val rejectReason: String?,
    val pinned: Boolean,
    val createdAt: Long,
    val readyAt: Long?
)

/** Where an upload from this phone is: asked for, sending, being checked, or done one way or another. */
enum class UploadState { QUEUED, UPLOADING, CHECKING, READY, REJECTED, FAILED, CANCELLED }

/**
 * A file the player is sharing from this phone, from picking it to its check's answer, so it shows
 * its progress, and can be cancelled or tried again, even after the app was closed. [uri] is the
 * picked document's own address (read with the system's permission for it, never a path);
 * [fileId] is the server's once asked; [reason] why it was rejected or failed.
 */
@Entity(
    tableName = "chat_upload",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId")]
)
data class ChatUploadEntity(
    @PrimaryKey val clientId: String,
    val partyId: String,
    val uri: String,
    val displayName: String,
    val mime: String,
    val sizeBytes: Long,
    val fileId: String? = null,
    val state: UploadState = UploadState.QUEUED,
    @ColumnInfo(defaultValue = "0") val sentBytes: Long = 0,
    val reason: String? = null,
    val createdAt: Long
)

/** How far the player has read a party's chat. */
@Entity(
    tableName = "chat_read",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)]
)
data class ChatReadEntity(
    @PrimaryKey val partyId: String,
    val lastReadAt: Long
)
