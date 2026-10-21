package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.ui.chat.ChatBackground
import com.pixelquest.app.ui.chat.ChatTimeline
import com.pixelquest.app.ui.chat.ChatTimelineTags
import com.pixelquest.app.ui.chat.DaySeparator
import com.pixelquest.app.ui.theme.PqType
import java.time.LocalDate
import java.time.ZoneId

/** What the Chat tab can do. */
data class ChatActions(
    val onDraftChange: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onReply: (ChatMessageRow) -> Unit = {},
    val onCancelReply: () -> Unit = {},
    val onDelete: (ChatMessage) -> Unit = {},
    val onRetry: (String) -> Unit = {},
    val onDiscard: (String) -> Unit = {},
    val onReachOldest: () -> Unit = {},
    val onDismissNotice: () -> Unit = {},
    /** Reports a message; null while there's no way to report (the menu leaves it out). */
    val onReport: ((ChatMessage) -> Unit)? = null,
    /** A document was picked to share; null where sharing files isn't offered. */
    val onAttach: ((String) -> Unit)? = null,
    val onRemoveAttachment: () -> Unit = {},
    val onCancelUpload: (String) -> Unit = {},
    /** Opens a shared file; null where files aren't opened. */
    val onOpenFile: ((com.pixelquest.app.study.chat.domain.ChatFile) -> Unit)? = null,
    val onConfirmOpen: () -> Unit = {},
    /** The leader pins or unpins a file; null for everyone else. */
    val onPinFile: ((com.pixelquest.app.study.chat.domain.ChatFile, Boolean) -> Unit)? = null,
    val onCancelOpen: () -> Unit = {},
    val onRetryUpload: (String) -> Unit = {},
    val onDismissUpload: (String) -> Unit = {}
)

/** A line of the Chat tab: a day's heading, or a message. */
internal sealed interface ChatLine {
    val key: String

    data class Day(val day: LocalDate) : ChatLine {
        override val key get() = "day-$day"
    }

    data class Message(val row: ChatMessageRow) : ChatLine {
        override val key get() = "message-" + (row.message.clientId ?: row.message.id)
    }

    /** Where what the player hasn't read starts. */
    data object Unread : ChatLine {
        override val key get() = "unread"
    }

    /** A file the player is sharing, on its way. */
    data class Upload(val upload: ChatUploadRow) : ChatLine {
        override val key get() = "upload-" + upload.clientId
    }
}

/**
 * The rows, each day headed once as its first message's time falls in [zone], and the unread line
 * before the first of others' messages after [readAt] (none the first time the chat's opened).
 */
internal fun linesOf(rows: List<ChatMessageRow>, zone: ZoneId, readAt: java.time.Instant? = null): List<ChatLine> {
    val lines = mutableListOf<ChatLine>()
    var last: LocalDate? = null
    val firstUnread = readAt?.let { mark -> rows.firstOrNull { !it.mine && !it.message.deleted && it.message.createdAt.isAfter(mark) } }
    for (row in rows) {
        val day = row.message.createdAt.atZone(zone).toLocalDate()
        if (day != last) lines += ChatLine.Day(day)
        last = day
        if (row === firstUnread) lines += ChatLine.Unread
        lines += ChatLine.Message(row)
    }
    return lines
}

/**
 * A study party's Chat tab (docs/study-chat.md): its messages over the chat's ground, as the
 * Darood chat shows its own (the shared [ChatTimeline]), and the [input] under them.
 */
@Composable
fun StudyChatPane(
    state: ChatUiState,
    zone: ZoneId,
    actions: ChatActions,
    modifier: Modifier = Modifier,
    input: @Composable ColumnScope.() -> Unit = {
        ChatInputBar(
            value = state.draft,
            onValueChange = actions.onDraftChange,
            onSend = actions.onSend,
            canSend = state.canSendAnything,
            above = {
                state.replyingTo?.let { ReplyPreview(it, onCancel = actions.onCancelReply) }
                state.attachment?.let { AttachmentChip(it, onRemove = actions.onRemoveAttachment) }
            },
            leading = { actions.onAttach?.let { ChatAttachButton(onPicked = it) } }
        )
    },
    message: (@Composable (ChatMessageRow) -> Unit)? = null
) {
    // The message whose menu is open, and the one waiting for its delete to be confirmed.
    var menuFor by remember { mutableStateOf<ChatMessageRow?>(null) }
    var deleting by remember { mutableStateOf<ChatMessageRow?>(null) }
    val line: @Composable (ChatMessageRow) -> Unit = message ?: { row ->
        MessageBubbleRow(
            row,
            onRetry = actions.onRetry,
            onDiscard = actions.onDiscard,
            onReply = { actions.onReply(row) },
            onMenu = { menuFor = row },
            onOpenFile = actions.onOpenFile
        )
    }
    menuFor?.let { row ->
        MessageMenu(
            row = row,
            items = menuItemsFor(row, state.isLeader, canReport = actions.onReport != null),
            onReply = { actions.onReply(row) },
            onDelete = {
                menuFor = null
                deleting = row
            },
            onReport = { actions.onReport?.invoke(row.message) },
            onDismiss = { menuFor = null }
        )
    }
    state.confirmingOpen?.let { file ->
        FileWarningDialog(file, onConfirm = actions.onConfirmOpen, onDismiss = actions.onCancelOpen)
    }
    deleting?.let { row ->
        DeleteMessageDialog(
            onConfirm = {
                deleting = null
                actions.onDelete(row.message)
            },
            onDismiss = { deleting = null }
        )
    }
    Column(modifier = modifier.fillMaxSize().testTag(StudyChatTags.PANE)) {
        ChatBackground(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (state.loaded && state.rows.isEmpty()) {
                EmptyChat()
            } else {
                ChatTimeline(
                    lines = linesOf(state.rows, zone, state.lastReadAt) + state.uploads.map { ChatLine.Upload(it) },
                    key = { it.key },
                    isMessage = { it is ChatLine.Message || it is ChatLine.Upload },
                    isMine = { (it as? ChatLine.Message)?.row?.mine == true || it is ChatLine.Upload },
                    loadingOlder = state.loadingOlder,
                    onReachOldest = actions.onReachOldest,
                    tags = ChatTimelineTags(list = StudyChatTags.LIST, loadingOlder = StudyChatTags.LOADING_OLDER, jump = StudyChatTags.JUMP),
                    modifier = Modifier.fillMaxSize()
                ) { line ->
                    when (line) {
                        is ChatLine.Day -> DaySeparator(day = line.day, today = state.today, tag = StudyChatTags.day(line.day))
                        is ChatLine.Message -> line(line.row)
                        ChatLine.Unread -> UnreadLine()
                        is ChatLine.Upload -> UploadBubble(line.upload, onCancel = actions.onCancelUpload, onRetry = actions.onRetryUpload, onDismiss = actions.onDismissUpload)
                    }
                }
            }
        }
        state.notice?.let { ChatNoticeLine(it, actions.onDismissNotice) }
        input()
    }
}

/** What the last send came to, over the input; a tap puts it away. */
@Composable
private fun ChatNoticeLine(notice: ChatNotice, onDismiss: () -> Unit) {
    val text = when (notice) {
        ChatNotice.SavedOffline -> R.string.study_chat_saved_offline
        ChatNotice.Offline -> R.string.study_chat_offline
        ChatNotice.FileUnreadable -> R.string.study_chat_file_unreadable
        ChatNotice.FileNotAllowed -> R.string.study_file_not_allowed
        ChatNotice.FileCorrupted -> R.string.study_file_corrupted
        ChatNotice.NoViewer -> R.string.study_file_no_viewer
        is ChatNotice.FileProblem -> when (notice.problem) {
            com.pixelquest.app.study.chat.domain.UploadProblem.BAD_TYPE -> R.string.study_chat_file_bad_type
            com.pixelquest.app.study.chat.domain.UploadProblem.MACROS -> R.string.study_chat_file_macros
            com.pixelquest.app.study.chat.domain.UploadProblem.ARCHIVE -> R.string.study_chat_file_archive
            com.pixelquest.app.study.chat.domain.UploadProblem.EMPTY -> R.string.study_chat_file_empty
            com.pixelquest.app.study.chat.domain.UploadProblem.TOO_LARGE -> R.string.study_chat_file_too_large
            com.pixelquest.app.study.chat.domain.UploadProblem.BAD_NAME -> R.string.study_chat_file_bad_name
        }
        is ChatNotice.Refused -> when (notice.refusal) {
            com.pixelquest.app.study.domain.StudyRefusal.TOO_MANY_MESSAGES -> R.string.study_chat_too_many
            else -> com.pixelquest.app.study.ui.noticeText(com.pixelquest.app.study.ui.StudyNotice.Refused(notice.refusal))
        }
    }
    Text(
        text = stringResource(text),
        style = PqType.Meta.copy(color = com.pixelquest.app.ui.theme.PqColors.Ink2),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(StudyChatTags.NOTICE)
    )
}

/** "Unread messages", across the chat where they start. */
@Composable
private fun UnreadLine() {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag(StudyChatTags.UNREAD)
    ) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f).height(2.dp).background(com.pixelquest.app.ui.theme.PqColors.Red))
        Text(text = stringResource(R.string.study_chat_unread), style = PqType.MetaStrong.copy(color = com.pixelquest.app.ui.theme.PqColors.RedText))
        androidx.compose.foundation.layout.Box(Modifier.weight(1f).height(2.dp).background(com.pixelquest.app.ui.theme.PqColors.Red))
    }
}

@Composable
private fun BoxScope.EmptyChat() {
    Text(
        text = stringResource(R.string.study_chat_empty),
        style = PqType.Body,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center).padding(24.dp).testTag(StudyChatTags.EMPTY)
    )
}

/** Test tags for the study chat. */
object StudyChatTags {
    const val PANE = "study_chat"
    const val LIST = "study_chat_list"
    const val LOADING_OLDER = "study_chat_loading_older"
    const val JUMP = "study_chat_jump"
    const val EMPTY = "study_chat_empty"
    const val INPUT = "study_chat_input"
    const val SEND = "study_chat_send"
    const val COUNTER = "study_chat_counter"
    const val FILES_SEARCH = "study_files_search"
    const val FILES_NEWEST = "study_files_newest"
    const val FILES_OLDEST = "study_files_oldest"
    const val FILES_NONE = "study_files_none"
    fun filesType(type: com.pixelquest.app.study.chat.domain.ChatFileType?) = "study_files_type_${type?.name ?: "ALL"}"
    const val FILE_STATUS = "study_chat_file_status"
    fun file(id: String) = "study_chat_file_$id"
    const val UPLOAD_STATUS = "study_chat_upload_status"
    const val UPLOAD_PROGRESS = "study_chat_upload_progress"
    const val UPLOAD_CANCEL = "study_chat_upload_cancel"
    const val UPLOAD_RETRY = "study_chat_upload_retry"
    const val UPLOAD_DISMISS = "study_chat_upload_dismiss"
    fun upload(clientId: String) = "study_chat_upload_$clientId"
    const val ATTACH = "study_chat_attach"
    const val ATTACHMENT = "study_chat_attachment"
    const val ATTACHMENT_REMOVE = "study_chat_attachment_remove"
    const val UNREAD = "study_chat_unread"
    const val MENU = "study_chat_menu"
    const val MENU_COPY = "study_chat_menu_copy"
    const val MENU_REPLY = "study_chat_menu_reply"
    const val MENU_DELETE = "study_chat_menu_delete"
    const val MENU_REPORT = "study_chat_menu_report"
    const val QUOTE = "study_chat_quote"
    const val REPLY_PREVIEW = "study_chat_reply_preview"
    const val REPLY_CANCEL = "study_chat_reply_cancel"
    const val PENDING = "study_chat_pending"
    const val SENT = "study_chat_sent"
    const val FAILED = "study_chat_failed"
    const val RETRY = "study_chat_retry"
    const val DISCARD = "study_chat_discard"
    const val SYSTEM = "study_chat_system_"
    const val NOTICE = "study_chat_notice"
    fun day(day: LocalDate) = "study_chat_day_$day"
    fun message(id: String) = "study_chat_message_$id"
}
