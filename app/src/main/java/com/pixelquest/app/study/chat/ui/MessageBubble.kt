package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.ui.chat.bubbleColors
import com.pixelquest.app.ui.chat.bubbleTime
import com.pixelquest.app.ui.chat.chatLook
import com.pixelquest.app.ui.chat.memberColor
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * One message in a study party's chat, on its side as the Darood chat places its bubbles: the
 * player's own on the right in the primary red, everyone else's on the left on paper, each
 * sender's name over their run in their own colour. A run of one sender's messages sits close,
 * named once. A deleted message keeps its place, saying so; the server's own lines are a pill.
 *
 * @param content what's inside the bubble under its text (a reply's quote above it, a file).
 */
@Composable
fun MessageBubbleRow(
    row: ChatMessageRow,
    modifier: Modifier = Modifier,
    onRetry: (String) -> Unit = {},
    onDiscard: (String) -> Unit = {},
    /** Answers it (swiped, or TalkBack's action); null for one that can't be (deleted, or not sent yet). */
    onReply: (() -> Unit)? = null,
    /** Opens its menu (a long press); null for none. */
    onMenu: (() -> Unit)? = null,
    /** Opens a shared file (a file message's); null where files aren't opened. */
    onOpenFile: ((com.pixelquest.app.study.chat.domain.ChatFile) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val message = row.message
    if (message.kind == ChatMessageKind.SYSTEM) {
        // Chess's lines are cards, drawn by the party's chess (J152).
        val chess = com.pixelquest.app.chess.ui.LocalChessChatCards.current
        val ref = message.refId
        if (chess != null && ref != null && message.body in com.pixelquest.app.chess.ui.CHESS_CHAT_TOKENS) {
            Box(modifier) { chess(message.body!!, ref) }
            return
        }
        SystemLine(message.body, modifier)
        return
    }
    Column(
        horizontalAlignment = if (row.mine) Alignment.End else Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (row.firstOfRun) 8.dp else 2.dp)
            .swipeToReply(onReply?.takeIf { !message.deleted && message.sync == null })
            .testTag(StudyChatTags.message(message.clientId ?: message.id))
    ) {
        if (row.firstOfRun && !row.mine) {
            Text(
                text = row.senderName?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone),
                style = PqType.MetaStrong.copy(color = memberColor(message.senderId)),
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        MessageBubble(row, onMenu) {
            row.file?.let { FileCard(it, onOpen = onOpenFile, content = bubbleColors(row.mine).content, secondary = bubbleColors(row.mine).secondary) }
            // A file message whose file the phone doesn't have yet (it's coming with the next refresh).
            if (row.file == null && message.kind == ChatMessageKind.FILE && !message.deleted) {
                Text(text = stringResource(R.string.study_file_loading), style = PqType.Meta.copy(color = bubbleColors(row.mine).secondary))
            }
            content()
        }
        val key = message.clientId
        if (row.mine && message.sync == SyncState.FAILED && key != null) NotSentLine(onRetry = { onRetry(key) }, onDiscard = { onDiscard(key) })
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun MessageBubble(row: ChatMessageRow, onMenu: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val message = row.message
    val colors = bubbleColors(row.mine)
    val look = chatLook()
    val shape = look.bubbleShape(mine = row.mine, tail = row.firstOfRun)
    val said = bubbleSaid(row)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            // TalkBack reads it as one sentence: who, what, when, and for the player's own, whether it's sent.
            .semantics(mergeDescendants = true) { contentDescription = said }
            .widthIn(min = 72.dp, max = 300.dp)
            .then(look.shadowed(shape))
            .clip(shape)
            .background(if (message.deleted) PqColors.Sunken else colors.container)
            .then(look.outlined(shape))
            .then(
                if (onMenu != null) {
                    Modifier.combinedClickable(onClickLabel = null, onLongClickLabel = stringResource(R.string.study_chat_menu), onLongClick = onMenu, onClick = {})
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (message.deleted) {
            Text(
                text = stringResource(R.string.study_chat_deleted),
                style = PqType.Body.copy(color = PqColors.Muted, fontStyle = FontStyle.Italic)
            )
        } else {
            row.replyTo?.let { ReplyQuote(it, row.replyToName, onBubble = colors.content) }
            content()
            message.body?.let { Text(text = it, style = PqType.Body.copy(color = colors.content)) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = bubbleTime(message.createdAt), style = PqType.Meta.copy(color = if (message.deleted) PqColors.Muted else colors.secondary))
            if (row.mine && !message.deleted) SendMark(message.sync, colors.secondary)
        }
    }
}

/** What TalkBack says for a bubble: "bram said: Anyone done Q3?, at 10:05" (the player's own: "You said..., sent"). */
@Composable
private fun bubbleSaid(row: ChatMessageRow): String {
    val message = row.message
    val time = bubbleTime(message.createdAt)
    val what = when {
        message.deleted -> stringResource(R.string.study_chat_deleted)
        row.file != null -> stringResource(R.string.study_chat_said_file, row.file.displayName) + (message.body?.let { ", $it" } ?: "")
        else -> message.body.orEmpty()
    }
    val reply = row.replyTo?.let { stringResource(R.string.study_chat_said_reply, row.replyToName ?: stringResource(R.string.darood_someone)) + " " }.orEmpty()
    if (!row.mine) return reply + stringResource(R.string.study_chat_said_theirs, row.senderName ?: stringResource(R.string.darood_someone), what, time)
    val state = stringResource(
        when (message.sync) {
            SyncState.PENDING -> R.string.darood_bubble_said_sending
            SyncState.FAILED -> R.string.darood_bubble_said_not_sent
            else -> R.string.darood_bubble_said_sent
        }
    )
    return reply + stringResource(R.string.study_chat_said_mine, what, time) + " " + state
}

/** On the player's own: a clock while it waits on the phone, a tick once sent, a warning if refused. */
@Composable
private fun SendMark(sync: SyncState?, color: Color) {
    val (mark, tag) = when (sync) {
        SyncState.PENDING -> "🕓" to StudyChatTags.PENDING
        SyncState.FAILED -> "⚠" to StudyChatTags.FAILED
        else -> "✓" to StudyChatTags.SENT
    }
    Text(text = mark, style = PqType.Meta.copy(color = color), modifier = Modifier.testTag(tag))
}

/** Under a refused message: it wasn't sent, and the ways on. */
@Composable
private fun NotSentLine(onRetry: () -> Unit, onDiscard: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
        Text(text = stringResource(R.string.study_chat_not_sent), style = PqType.Meta.copy(color = PqColors.Error))
        for ((label, action, tag) in listOf(
            Triple(R.string.study_chat_retry, onRetry, StudyChatTags.RETRY),
            Triple(R.string.study_chat_discard, onDiscard, StudyChatTags.DISCARD)
        )) {
            Text(
                text = stringResource(label).uppercase(),
                style = PqType.Button.copy(color = PqColors.RedText),
                modifier = Modifier
                    .testTag(tag)
                    .clickable(role = Role.Button, onClick = action)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
    }
}

/** One of the server's own lines (a token it sends), worded and centred as a pill. */
@Composable
private fun SystemLine(token: String?, modifier: Modifier) {
    val look = chatLook()
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = stringResource(R.string.study_chat_system_line),
            style = PqType.Meta,
            modifier = Modifier
                .background(PqColors.Paper, look.pillShape)
                .then(look.outlined(look.pillShape))
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .testTag(StudyChatTags.SYSTEM + (token ?: ""))
        )
    }
}
