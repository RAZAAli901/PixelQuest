package com.pixelquest.app.study.chat.ui

import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatMessage
import java.time.LocalDate

/** A message on the screen: with its file (a file message), the message it answers, and its sender's name. */
data class ChatMessageRow(
    val message: ChatMessage,
    val mine: Boolean,
    val senderName: String?,
    val file: ChatFile? = null,
    val replyTo: ChatMessage? = null,
    val replyToName: String? = null,
    /** Starts its sender's run (one sender's messages in a row): their name shows once, here. */
    val firstOfRun: Boolean = true,
    val lastOfRun: Boolean = true
)

/** A file the player is sharing, on its way: how far it's got, or why it stopped. */
data class ChatUploadRow(
    val clientId: String,
    val name: String,
    val mime: String,
    val sizeBytes: Long,
    val sentBytes: Long,
    val state: com.pixelquest.app.study.chat.data.local.UploadState,
    val reason: String?
) {
    /** How much has gone, 0 to 1; null while it isn't known (the document didn't say its size). */
    val progress: Float? get() = if (sizeBytes > 0) (sentBytes.toFloat() / sizeBytes).coerceIn(0f, 1f) else null
}

/** What a study party's Chat tab shows. */
data class ChatUiState(
    val loaded: Boolean = false,
    val rows: List<ChatMessageRow> = emptyList(),
    /** The party's shared files, ready and not deleted, newest first (the Files tab). */
    val files: List<ChatFile> = emptyList(),
    val draft: String = "",
    /** The message the next one answers, chosen with Reply. */
    val replyingTo: ChatMessageRow? = null,
    val myId: String? = null,
    val isLeader: Boolean = false,
    /** Where the unread divider goes: before the first message after this moment, not the player's own. */
    val lastReadAt: java.time.Instant? = null,
    val today: LocalDate = LocalDate.MIN,
    val loadingOlder: Boolean = false,
    val offline: Boolean = false,
    /** The last thing tried, said once: a send saved offline, or refused. */
    val notice: ChatNotice? = null,
    /** Others' messages not yet read, for the Chat tab's count. */
    val unread: Int = 0,
    /** The file picked to share, waiting to be sent. */
    val attachment: com.pixelquest.app.study.chat.data.PickedFile? = null,
    /** Files the player is sharing, on their way, oldest first. */
    val uploads: List<ChatUploadRow> = emptyList(),
    /** The shared file being fetched to open, while it is. */
    val opening: String? = null,
    /** The file waiting for the player to say yes to the first-open warning. */
    val confirmingOpen: ChatFile? = null,
    /** Each item's materials, by item id. */
    val itemFiles: Map<String, List<String>> = emptyMap()
) {
    /** Something to send: text, a file, or both. */
    val canSendAnything: Boolean get() = canSend || (attachment != null && draft.length <= com.pixelquest.app.study.chat.domain.StudyChatConfig.MESSAGE_MAX_CHARS)

    val canSend: Boolean get() = draft.isNotBlank() && draft.length <= com.pixelquest.app.study.chat.domain.StudyChatConfig.MESSAGE_MAX_CHARS
}
