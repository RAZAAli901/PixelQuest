package com.pixelquest.app.study.chat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.data.PickedFile
import com.pixelquest.app.study.chat.domain.StudyChatConfig
import com.pixelquest.app.ui.chat.chatLook
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * Attach, before the field: the system's own document picker, for the allowed types only. It
 * hands over the one document chosen, readable by the app; the app never asks to read storage.
 */
@Composable
fun ChatAttachButton(onPicked: (String) -> Unit, modifier: Modifier = Modifier) {
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { onPicked(it.toString()) } }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(StudyChatTags.ATTACH)
            .size(48.dp)
            .comicPressable(
                { pick.launch(StudyChatConfig.ALLOWED_TYPES.keys.toTypedArray()) },
                bg = PqColors.Paper,
                radius = 12.dp,
                shadow = 2.dp,
                onClickLabel = stringResource(R.string.study_chat_attach),
                role = Role.Button
            )
    ) {
        Icon(Icons.Rounded.AttachFile, contentDescription = stringResource(R.string.study_chat_attach), tint = PqColors.Ink)
    }
}

/** A file's size as people say it: 980 KB, 1.2 MB. */
@Composable
fun sizeText(bytes: Long): String = android.text.format.Formatter.formatShortFileSize(LocalContext.current, bytes.coerceAtLeast(0))

/** The file picked to share, over the field until it's sent or taken away. */
@Composable
fun AttachmentChip(file: PickedFile, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val look = chatLook()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(PqColors.Ground, look.fieldShape)
            .border(PqDimens.BorderThin, PqColors.Ink, look.fieldShape)
            .padding(start = 12.dp)
            .testTag(StudyChatTags.ATTACHMENT)
    ) {
        Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = PqColors.Ink)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.name, style = PqType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = sizeText(file.sizeBytes), style = PqType.Meta)
        }
        PqIconButton(Icons.Rounded.Close, stringResource(R.string.study_chat_attach_remove), onRemove, Modifier.testTag(StudyChatTags.ATTACHMENT_REMOVE))
    }
}
