package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatFileType
import com.pixelquest.app.ui.chat.chatLook
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** A file kind's icon, and the word for it. */
fun ChatFileType.icon(): ImageVector = when (this) {
    ChatFileType.PDF -> Icons.Rounded.PictureAsPdf
    ChatFileType.WORD -> Icons.Rounded.Description
    ChatFileType.EXCEL -> Icons.Rounded.TableChart
    ChatFileType.POWERPOINT -> Icons.Rounded.Slideshow
    ChatFileType.TEXT -> Icons.Rounded.Article
    ChatFileType.IMAGE -> Icons.Rounded.Image
}

fun ChatFileType.label(): Int = when (this) {
    ChatFileType.PDF -> R.string.study_file_type_pdf
    ChatFileType.WORD -> R.string.study_file_type_word
    ChatFileType.EXCEL -> R.string.study_file_type_excel
    ChatFileType.POWERPOINT -> R.string.study_file_type_powerpoint
    ChatFileType.TEXT -> R.string.study_file_type_text
    ChatFileType.IMAGE -> R.string.study_file_type_image
}

/**
 * A shared file, inside its message's bubble (or on the Files tab): its kind's icon, its name, its
 * kind and size, who shared it when [uploader] is given, and where it is: ready to open, or (the
 * uploader's own) still being checked, or rejected. A ready one opens with [onOpen].
 */
@Composable
fun FileCard(
    file: ChatFile,
    onOpen: ((ChatFile) -> Unit)?,
    modifier: Modifier = Modifier,
    content: Color = PqColors.Ink,
    secondary: Color = PqColors.Ink2,
    uploader: String? = null
) {
    val look = chatLook()
    val ready = file.status == ChatFileStatus.READY
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(PqDimens.BorderThin, content.copy(alpha = 0.6f), look.fieldShape)
            .then(
                if (ready && onOpen != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.study_file_open)) { onOpen(file) }
                } else {
                    Modifier
                }
            )
            .padding(10.dp)
            .testTag(StudyChatTags.file(file.id))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(PqColors.Paper, look.fieldShape).border(PqDimens.BorderThin, PqColors.Ink, look.fieldShape)
        ) {
            Icon(file.type.icon(), contentDescription = null, tint = PqColors.Ink)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.displayName, style = PqType.BodyStrong.copy(color = content), maxLines = 2, overflow = TextOverflow.Ellipsis)
            val parts = listOfNotNull(stringResource(file.type.label()), sizeText(file.sizeBytes), uploader?.let { stringResource(R.string.study_file_by, it) })
            Text(text = parts.joinToString(" · "), style = PqType.Meta.copy(color = secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!ready) {
                Text(
                    text = when (file.status) {
                        ChatFileStatus.PENDING -> stringResource(R.string.study_chat_upload_checking)
                        else -> stringResource(R.string.study_chat_upload_rejected, stringResource(rejectionText(file.rejectReason)))
                    },
                    style = PqType.Meta.copy(color = if (file.status == ChatFileStatus.REJECTED) PqColors.Error else secondary),
                    modifier = Modifier.testTag(StudyChatTags.FILE_STATUS)
                )
            }
        }
    }
}
