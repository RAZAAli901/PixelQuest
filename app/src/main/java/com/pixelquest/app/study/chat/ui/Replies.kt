package com.pixelquest.app.study.chat.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.ui.chat.memberColor
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/** How far a bubble is swiped before letting go answers it. */
private val SWIPE_TO_REPLY = 64.dp

/**
 * A message the player can answer: swiped to the right and let go past [SWIPE_TO_REPLY], as a
 * messaging app does, or by TalkBack's Reply action. Null [onReply], it's just shown.
 */
@Composable
internal fun Modifier.swipeToReply(onReply: (() -> Unit)?): Modifier {
    if (onReply == null) return this
    val label = stringResource(R.string.study_chat_reply)
    val latest by rememberUpdatedState(onReply)
    val threshold = with(LocalDensity.current) { SWIPE_TO_REPLY.toPx() }
    var dragged by remember { mutableFloatStateOf(0f) }
    val shown by animateFloatAsState(dragged, label = "swipe to reply")
    return this
        .semantics { customActions = listOf(CustomAccessibilityAction(label) { latest(); true }) }
        .graphicsLayer { translationX = shown }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (dragged >= threshold) latest()
                    dragged = 0f
                },
                onDragCancel = { dragged = 0f }
            ) { change, amount ->
                // Only to the right, and not much past the point that answers it.
                val next = (dragged + amount).coerceIn(0f, threshold * 1.4f)
                if (next != dragged) change.consume()
                dragged = next
            }
        }
}

/** What a message says, in a line: its text, or that it was deleted. */
@Composable
internal fun snippetOf(message: ChatMessage): String =
    if (message.deleted) stringResource(R.string.study_chat_deleted) else message.body.orEmpty().replace('\n', ' ')

/** The message a reply answers, quoted at the top of its bubble: who said it, and what. */
@Composable
fun ReplyQuote(message: ChatMessage, senderName: String?, onBubble: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier.height(IntrinsicSize.Min).testTag(StudyChatTags.QUOTE)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(memberColor(message.senderId)))
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = senderName?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone),
                style = PqType.MetaStrong.copy(color = onBubble),
                maxLines = 1
            )
            Text(text = snippetOf(message), style = PqType.Meta.copy(color = onBubble), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Over the input while answering: what's being answered, and a way to stop. */
@Composable
fun ReplyPreview(row: ChatMessageRow, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().testTag(StudyChatTags.REPLY_PREVIEW)
    ) {
        Box(Modifier.weight(1f)) {
            Column {
                Text(text = stringResource(R.string.study_chat_replying_to), style = PqType.Meta)
                ReplyQuote(row.message, row.senderName, onBubble = PqColors.Ink)
            }
        }
        PqIconButton(Icons.Rounded.Close, stringResource(R.string.study_chat_reply_cancel), onCancel, Modifier.testTag(StudyChatTags.REPLY_CANCEL))
    }
}
