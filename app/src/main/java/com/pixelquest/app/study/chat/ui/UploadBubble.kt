package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.data.local.UploadState
import com.pixelquest.app.ui.chat.bubbleColors
import com.pixelquest.app.ui.chat.chatLook
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * A file the player is sharing, on its way, as their own bubble: its name and size, a bar of how
 * much has gone, and where it is (waiting, sending, being checked); then Cancel while it's under
 * way, Try again if it couldn't be sent, or why it wasn't shared.
 */
@Composable
fun UploadBubble(upload: ChatUploadRow, onCancel: (String) -> Unit, onRetry: (String) -> Unit, onDismiss: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = bubbleColors(mine = true)
    val look = chatLook()
    val shape = look.bubbleShape(mine = true, tail = true)
    val underWay = upload.state in setOf(UploadState.QUEUED, UploadState.UPLOADING, UploadState.CHECKING)
    Column(horizontalAlignment = Alignment.End, modifier = modifier.fillMaxWidth().padding(top = 8.dp).testTag(StudyChatTags.upload(upload.clientId))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .widthIn(min = 200.dp, max = 300.dp)
                .then(look.shadowed(shape))
                .clip(shape)
                .background(colors.container)
                .then(look.outlined(shape))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = colors.content)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = upload.name, style = PqType.BodyStrong.copy(color = colors.content), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = sizeText(upload.sizeBytes), style = PqType.Meta.copy(color = colors.secondary))
                }
            }
            if (underWay) ProgressLine(upload)
            Text(
                text = statusText(upload),
                style = PqType.Meta.copy(color = if (upload.state == UploadState.REJECTED || upload.state == UploadState.FAILED) colors.content else colors.secondary),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag(StudyChatTags.UPLOAD_STATUS)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            when (upload.state) {
                UploadState.QUEUED, UploadState.UPLOADING, UploadState.CHECKING ->
                    Action(R.string.study_chat_upload_cancel, StudyChatTags.UPLOAD_CANCEL) { onCancel(upload.clientId) }
                UploadState.FAILED -> {
                    Action(R.string.study_chat_upload_retry, StudyChatTags.UPLOAD_RETRY) { onRetry(upload.clientId) }
                    Action(R.string.study_chat_upload_cancel, StudyChatTags.UPLOAD_CANCEL) { onCancel(upload.clientId) }
                }
                UploadState.REJECTED -> Action(R.string.study_chat_upload_dismiss, StudyChatTags.UPLOAD_DISMISS) { onDismiss(upload.clientId) }
                UploadState.READY, UploadState.CANCELLED -> Unit
            }
        }
    }
}

/** How much has gone, as a bar; read out as a progress bar. */
@Composable
private fun ProgressLine(upload: ChatUploadRow) {
    val progress = upload.progress ?: 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(chatLook().pillShape)
            .background(PqColors.OnRed.copy(alpha = 0.3f))
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
            .testTag(StudyChatTags.UPLOAD_PROGRESS)
    ) {
        Box(Modifier.fillMaxWidth(if (upload.state == UploadState.CHECKING) 1f else progress).height(6.dp).background(PqColors.OnRed))
    }
}

@Composable
private fun Action(label: Int, tag: String, onClick: () -> Unit) {
    Text(
        text = stringResource(label).uppercase(),
        style = PqType.Button.copy(color = PqColors.RedText),
        modifier = Modifier.testTag(tag).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

/** Where a share is, in words. */
@Composable
private fun statusText(upload: ChatUploadRow): String = when (upload.state) {
    UploadState.QUEUED -> stringResource(R.string.study_chat_upload_waiting)
    UploadState.UPLOADING -> upload.progress?.let { stringResource(R.string.study_chat_upload_sending, (it * 100).toInt()) }
        ?: stringResource(R.string.study_chat_upload_sending_unknown)
    UploadState.CHECKING, UploadState.READY -> stringResource(R.string.study_chat_upload_checking)
    UploadState.REJECTED -> stringResource(R.string.study_chat_upload_rejected, stringResource(rejectionText(upload.reason)))
    UploadState.FAILED, UploadState.CANCELLED -> stringResource(R.string.study_chat_upload_failed)
}

/** Why the server (or the phone) didn't keep a file, in a few words. */
fun rejectionText(reason: String?): Int = when (reason) {
    "bad_type", "type_mismatch", "bad_text", "bad_name" -> R.string.study_chat_reject_bad_type
    "macros" -> R.string.study_chat_reject_macros
    "embedded" -> R.string.study_chat_reject_embedded
    "too_large", "size_mismatch" -> R.string.study_chat_reject_too_large
    "empty" -> R.string.study_chat_reject_empty
    "party_storage_full" -> R.string.study_chat_reject_full
    "too_many_uploads" -> R.string.study_chat_reject_too_many
    "upload_expired" -> R.string.study_chat_reject_late
    else -> R.string.study_chat_reject_other
}
