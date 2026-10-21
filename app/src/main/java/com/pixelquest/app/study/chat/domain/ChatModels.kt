package com.pixelquest.app.study.chat.domain

import com.pixelquest.app.study.domain.SyncState
import java.time.Instant

// A study party's chat (docs/study-chat.md): its messages and shared files. The chat only talks:
// it never touches points, XP, levels or streaks.

/** The chat's limits and allowed types, mirroring the server's study_chat_config and study_chat_types (a test holds them equal). */
object StudyChatConfig {
    const val MESSAGE_MAX_CHARS = 2000
    const val FILE_MAX_BYTES = 20L * 1024 * 1024
    const val PARTY_MAX_BYTES = 500L * 1024 * 1024
    const val UPLOADS_PER_DAY = 20
    const val MESSAGES_PER_MINUTE = 30
    const val FILE_NAME_MAX_CHARS = 120

    /** When the counter under the text field shows: this close to the limit. */
    const val COUNTER_FROM = MESSAGE_MAX_CHARS - 200

    /** The allowed types and their extensions: nothing else is ever sent. */
    val ALLOWED_TYPES: Map<String, List<String>> = mapOf(
        "application/pdf" to listOf("pdf"),
        "application/msword" to listOf("doc"),
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to listOf("docx"),
        "application/vnd.ms-excel" to listOf("xls"),
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to listOf("xlsx"),
        "application/vnd.ms-powerpoint" to listOf("ppt"),
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" to listOf("pptx"),
        "text/plain" to listOf("txt"),
        "text/csv" to listOf("csv"),
        "image/png" to listOf("png"),
        "image/jpeg" to listOf("jpg", "jpeg")
    )
}

/** What a message is, as the server names it (chat_messages.kind). */
enum class ChatMessageKind(val wire: String) {
    TEXT("text"),
    FILE("file"),
    SYSTEM("system");

    companion object {
        fun fromWire(wire: String?): ChatMessageKind = entries.firstOrNull { it.wire == wire } ?: SYSTEM
    }
}

/**
 * A message in a study party's chat. A deleted one keeps its place with no text or file.
 *
 * @param clientId the phone's own key for one sent from it.
 * @param sync null once the server has it; PENDING while it waits to send, FAILED if refused.
 */
data class ChatMessage(
    val id: String,
    val partyId: String,
    val senderId: String?,
    val kind: ChatMessageKind,
    val body: String?,
    val fileId: String?,
    val replyTo: String?,
    val clientId: String?,
    val createdAt: Instant,
    val deletedAt: Instant?,
    val sync: SyncState? = null,
    /** What one of the server's lines is about (a chess challenge, a game). */
    val refId: String? = null
) {
    val deleted: Boolean get() = deletedAt != null
}

/** Where a shared file stands (chat_files.status). */
enum class ChatFileStatus(val wire: String) {
    PENDING("pending"),
    READY("ready"),
    REJECTED("rejected");

    companion object {
        fun fromWire(wire: String?): ChatFileStatus = entries.firstOrNull { it.wire == wire } ?: REJECTED
    }
}

/** A file's kind, for its icon and the Files tab's filter. */
enum class ChatFileType { PDF, WORD, EXCEL, POWERPOINT, TEXT, IMAGE;

    companion object {
        fun of(mime: String): ChatFileType = when {
            mime == "application/pdf" -> PDF
            mime == "application/msword" || mime.contains("wordprocessingml") -> WORD
            mime == "application/vnd.ms-excel" || mime.contains("spreadsheetml") || mime == "text/csv" -> EXCEL
            mime == "application/vnd.ms-powerpoint" || mime.contains("presentationml") -> POWERPOINT
            mime.startsWith("image/") -> IMAGE
            else -> TEXT
        }
    }
}

/**
 * A file shared in a study party's chat, as the server checked it: its real type and size.
 * [rejectReason] says why a rejected one was (only its uploader sees those).
 */
data class ChatFile(
    val id: String,
    val partyId: String,
    val uploaderId: String?,
    val displayName: String,
    val mime: String,
    val sizeBytes: Long,
    val sha256: String?,
    val status: ChatFileStatus,
    val rejectReason: String?,
    val pinned: Boolean,
    val createdAt: Instant,
    val readyAt: Instant?
) {
    val type: ChatFileType get() = ChatFileType.of(mime)
}
