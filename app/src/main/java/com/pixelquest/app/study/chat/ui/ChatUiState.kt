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
    val offline: Boolean = false
) {
    val canSend: Boolean get() = draft.isNotBlank() && draft.length <= com.pixelquest.app.study.chat.domain.StudyChatConfig.MESSAGE_MAX_CHARS
}
