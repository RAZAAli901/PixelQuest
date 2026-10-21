package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val message = row.message
    if (message.kind == ChatMessageKind.SYSTEM) {
        SystemLine(message.body, modifier)
        return
    }
    Column(
        horizontalAlignment = if (row.mine) Alignment.End else Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (row.firstOfRun) 8.dp else 2.dp)
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
        MessageBubble(row, content)
        val key = message.clientId
        if (row.mine && message.sync == SyncState.FAILED && key != null) NotSentLine(onRetry = { onRetry(key) }, onDiscard = { onDiscard(key) })
    }
}

@Composable
private fun MessageBubble(row: ChatMessageRow, content: @Composable ColumnScope.() -> Unit) {
    val message = row.message
    val colors = bubbleColors(row.mine)
    val look = chatLook()
    val shape = look.bubbleShape(mine = row.mine, tail = row.firstOfRun)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .widthIn(min = 72.dp, max = 300.dp)
            .then(look.shadowed(shape))
            .clip(shape)
            .background(if (message.deleted) PqColors.Sunken else colors.container)
            .then(look.outlined(shape))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (message.deleted) {
            Text(
                text = stringResource(R.string.study_chat_deleted),
                style = PqType.Body.copy(color = PqColors.Muted, fontStyle = FontStyle.Italic)
            )
        } else {
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
