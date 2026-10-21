package com.pixelquest.app.study.chat.data.remote

import com.pixelquest.app.party.data.remote.parseInstant
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// A study party's chat as Supabase sends it (public.chat_messages, public.chat_files) and its Edge
// Functions answer (request-upload, finalize-upload, get-download-url), and the mappings.

/** A row of public.chat_messages. */
@Serializable
data class ChatMessageDto(
    @SerialName("id") val id: String,
    @SerialName("party_id") val partyId: String,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("kind") val kind: String,
    @SerialName("body") val body: String? = null,
    @SerialName("file_id") val fileId: String? = null,
    @SerialName("reply_to") val replyTo: String? = null,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("ref_id") val refId: String? = null
)

/** What a member sends: text, as themselves (the server fills in who and when). Its kind is always sent. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NewChatMessageDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("body") val body: String,
    @SerialName("reply_to") val replyTo: String? = null,
    @SerialName("client_id") val clientId: String,
    @EncodeDefault @SerialName("kind") val kind: String = "text"
)

/** A row of public.chat_files, without its storage path (never granted). */
@Serializable
data class ChatFileDto(
    @SerialName("id") val id: String,
    @SerialName("party_id") val partyId: String,
    @SerialName("uploader_id") val uploaderId: String? = null,
    @SerialName("display_name") val displayName: String,
    @SerialName("mime") val mime: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("sha256") val sha256: String? = null,
    @SerialName("status") val status: String,
    @SerialName("reject_reason") val rejectReason: String? = null,
    @SerialName("pinned") val pinned: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("ready_at") val readyAt: String? = null
)

/** What request-upload is asked. */
@Serializable
data class RequestUploadBody(
    @SerialName("party_id") val partyId: String,
    @SerialName("name") val name: String,
    @SerialName("mime") val mime: String,
    @SerialName("size") val size: Long
)

/** request-upload's answer: the pending file, and where to send its bytes (a path on the project). */
@Serializable
data class RequestUploadReply(
    @SerialName("file_id") val fileId: String,
    @SerialName("upload_url") val uploadPath: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("expires_in") val expiresIn: Int
)

/** finalize-upload's answer: ready (with its message) or rejected (with why). */
@Serializable
data class FinalizeReply(
    @SerialName("status") val status: String,
    @SerialName("reason") val reason: String? = null,
    @SerialName("message_id") val messageId: String? = null
)

/** get-download-url's answer: a short-lived path on the project, and what's there. */
@Serializable
data class DownloadReply(
    @SerialName("url") val path: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("name") val name: String,
    @SerialName("mime") val mime: String,
    @SerialName("size_bytes") val sizeBytes: Long
)

/** The functions' refusals: {"error": "..."}. */
@Serializable
data class FunctionError(@SerialName("error") val error: String)

fun ChatMessageDto.toDomain(): ChatMessage = ChatMessage(
    id = id,
    partyId = partyId,
    senderId = senderId,
    kind = ChatMessageKind.fromWire(kind),
    body = body,
    fileId = fileId,
    replyTo = replyTo,
    clientId = clientId,
    createdAt = parseInstant(createdAt),
    deletedAt = deletedAt?.let(::parseInstant),
    refId = refId
)

fun ChatFileDto.toDomain(): ChatFile = ChatFile(
    id = id,
    partyId = partyId,
    uploaderId = uploaderId,
    displayName = displayName,
    mime = mime,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    status = ChatFileStatus.fromWire(status),
    rejectReason = rejectReason,
    pinned = pinned,
    createdAt = parseInstant(createdAt),
    readyAt = readyAt?.let(::parseInstant)
)
