package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/** What a message's menu offers, for [row] and who's looking ([isLeader]). */
data class MessageMenuItems(val copy: Boolean, val reply: Boolean, val delete: Boolean, val report: Boolean)

/**
 * The menu's items for [row]: copy and reply for a message with text, sent; delete for the player's
 * own, or any as the party's leader (the server checks too); report for anyone else's, while
 * there's a way to report.
 */
fun menuItemsFor(row: ChatMessageRow, isLeader: Boolean, canReport: Boolean): MessageMenuItems {
    val message = row.message
    val live = !message.deleted && message.sync == null
    return MessageMenuItems(
        copy = live && !message.body.isNullOrEmpty(),
        reply = live,
        delete = live && (row.mine || isLeader),
        report = live && !row.mine && canReport
    )
}

/**
 * A message's menu, from a long press (mock 4E's sheet): Copy, Reply, Delete and Report, as
 * [menuItemsFor] allows. Delete asks first; it goes for everyone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageMenu(
    row: ChatMessageRow,
    items: MessageMenuItems,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    PqSheet(onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp).testTag(StudyChatTags.MENU)) {
            Text(
                text = row.message.body ?: stringResource(R.string.study_chat_deleted),
                style = PqType.Meta,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).semantics { heading() }
            )
            if (items.copy) {
                MenuLine(Icons.Rounded.ContentCopy, R.string.study_chat_copy, StudyChatTags.MENU_COPY) {
                    clipboard.setText(AnnotatedString(row.message.body.orEmpty()))
                    onDismiss()
                }
            }
            if (items.reply) MenuLine(Icons.AutoMirrored.Rounded.Reply, R.string.study_chat_reply, StudyChatTags.MENU_REPLY) { onReply(); onDismiss() }
            if (items.delete) MenuLine(Icons.Rounded.Delete, R.string.study_chat_delete, StudyChatTags.MENU_DELETE, danger = true) { onDelete() }
            if (items.report) MenuLine(Icons.Rounded.Flag, R.string.study_chat_report, StudyChatTags.MENU_REPORT) { onReport(); onDismiss() }
        }
    }
}

@Composable
private fun MenuLine(icon: ImageVector, label: Int, tag: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp)
            .testTag(tag)
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) PqColors.Error else PqColors.Ink)
        Text(text = stringResource(label), style = PqType.BodyStrong.copy(color = if (danger) PqColors.Error else PqColors.Ink))
    }
}

/**
 * The first file opened from a party: files come from other people. The server checks each one's
 * type by its bytes, but not what it says inside; open only what was expected.
 */
@Composable
fun FileWarningDialog(file: com.pixelquest.app.study.chat.domain.ChatFile, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PqConfirmDialog(
        title = stringResource(R.string.study_file_warning_title),
        message = stringResource(R.string.study_file_warning_message, file.displayName),
        confirm = stringResource(R.string.study_file_warning_open),
        dismiss = stringResource(R.string.pq_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/** Before a delete: it goes for everyone in the party, and can't be brought back. */
@Composable
fun DeleteMessageDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PqConfirmDialog(
        title = stringResource(R.string.study_chat_delete_title),
        message = stringResource(R.string.study_chat_delete_message),
        confirm = stringResource(R.string.study_chat_delete),
        dismiss = stringResource(R.string.study_reminder_keep),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
