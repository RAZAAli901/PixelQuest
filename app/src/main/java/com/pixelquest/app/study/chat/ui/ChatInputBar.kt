package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.domain.StudyChatConfig
import com.pixelquest.app.ui.chat.chatLook
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.text.NumberFormat

/**
 * The Chat tab's input, styled as the Darood chat's: a field for text (a few lines, then it
 * scrolls), up to 2,000 characters, with a counter once it nears them, and a send button. The
 * keyboard's Enter is a new line; the button sends.
 *
 * @param above what sits over the field (the message being answered).
 * @param leading what comes before the field (attaching a file).
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    /** Whether there's something to send; text alone by default (a picked file counts too). */
    canSend: Boolean = value.isNotBlank() && value.length <= StudyChatConfig.MESSAGE_MAX_CHARS,
    above: @Composable ColumnScope.() -> Unit = {},
    leading: @Composable RowScope.() -> Unit = {}
) {
    val look = chatLook()
    val label = stringResource(R.string.study_chat_input_label)
    Column(modifier = modifier.fillMaxWidth().background(PqColors.Paper).imePadding()) {
        Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = PqDimens.ScreenPad, vertical = 10.dp)
        ) {
            above()
            if (value.length >= StudyChatConfig.COUNTER_FROM) {
                val numbers = NumberFormat.getIntegerInstance()
                Text(
                    text = stringResource(R.string.study_chat_counter, numbers.format(value.length), numbers.format(StudyChatConfig.MESSAGE_MAX_CHARS)),
                    style = PqType.Meta.copy(color = if (value.length >= StudyChatConfig.MESSAGE_MAX_CHARS) PqColors.Error else PqColors.Ink2),
                    modifier = Modifier
                        .align(Alignment.End)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag(StudyChatTags.COUNTER)
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                leading()
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(look.fieldShape)
                        .background(PqColors.Ground)
                        .border(PqDimens.Border, PqColors.Ink, look.fieldShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(text = stringResource(R.string.study_chat_input_placeholder), style = PqType.Body.copy(color = PqColors.Muted), maxLines = 1)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { onValueChange(com.pixelquest.app.domain.validation.InputRules.typed(it, com.pixelquest.app.domain.validation.InputRules.Field.CHAT_MESSAGE)) },
                        maxLines = 5,
                        textStyle = PqType.Body.copy(color = PqColors.Ink),
                        cursorBrush = SolidColor(PqColors.Ink),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = label }
                            .testTag(StudyChatTags.INPUT)
                    )
                }
                ChatSendButton(enabled = canSend, onClick = onSend)
            }
        }
    }
}

/** The send button beside the field: red when the text can go, sunken and inert when it can't. */
@Composable
fun ChatSendButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(StudyChatTags.SEND)
            .size(48.dp)
            .comicPressable(
                onClick,
                bg = if (enabled) PqColors.Red else PqColors.Sunken,
                radius = 12.dp,
                shadow = if (enabled) 2.dp else 0.dp,
                enabled = enabled,
                onClickLabel = stringResource(R.string.study_chat_send),
                role = Role.Button
            )
    ) {
        Icon(Icons.Rounded.Send, contentDescription = stringResource(R.string.study_chat_send), tint = if (enabled) PqColors.OnRed else PqColors.Muted)
    }
}
