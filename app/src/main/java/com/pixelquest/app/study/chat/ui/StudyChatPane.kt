package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val onDismissNotice: () -> Unit = {}
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
}

/** The rows, each day headed once as its first message's time falls in [zone]. */
internal fun linesOf(rows: List<ChatMessageRow>, zone: ZoneId): List<ChatLine> {
    val lines = mutableListOf<ChatLine>()
    var last: LocalDate? = null
    for (row in rows) {
        val day = row.message.createdAt.atZone(zone).toLocalDate()
        if (day != last) lines += ChatLine.Day(day)
        last = day
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
    input: @Composable ColumnScope.() -> Unit = { ChatInputBar(value = state.draft, onValueChange = actions.onDraftChange, onSend = actions.onSend) },
    message: @Composable (ChatMessageRow) -> Unit = { MessageText(it) }
) {
    Column(modifier = modifier.fillMaxSize().testTag(StudyChatTags.PANE)) {
        ChatBackground(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (state.loaded && state.rows.isEmpty()) {
                EmptyChat()
            } else {
                ChatTimeline(
                    lines = linesOf(state.rows, zone),
                    key = { it.key },
                    isMessage = { it is ChatLine.Message },
                    isMine = { (it as? ChatLine.Message)?.row?.mine == true },
                    loadingOlder = state.loadingOlder,
                    onReachOldest = actions.onReachOldest,
                    tags = ChatTimelineTags(list = StudyChatTags.LIST, loadingOlder = StudyChatTags.LOADING_OLDER, jump = StudyChatTags.JUMP),
                    modifier = Modifier.fillMaxSize()
                ) { line ->
                    when (line) {
                        is ChatLine.Day -> DaySeparator(day = line.day, today = state.today, tag = StudyChatTags.day(line.day))
                        is ChatLine.Message -> message(line.row)
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

@Composable
private fun BoxScope.EmptyChat() {
    Text(
        text = stringResource(R.string.study_chat_empty),
        style = PqType.Body,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center).padding(24.dp).testTag(StudyChatTags.EMPTY)
    )
}

/** A message as plain text, until the chat has its bubbles. */
@Composable
private fun MessageText(row: ChatMessageRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag(StudyChatTags.message(row.message.id))) {
        if (row.firstOfRun && !row.mine) row.senderName?.let { Text(it, style = PqType.MetaStrong) }
        Text(row.message.body ?: stringResource(R.string.study_chat_deleted), style = PqType.Body)
    }
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
    const val NOTICE = "study_chat_notice"
    fun day(day: LocalDate) = "study_chat_day_$day"
    fun message(id: String) = "study_chat_message_$id"
}
