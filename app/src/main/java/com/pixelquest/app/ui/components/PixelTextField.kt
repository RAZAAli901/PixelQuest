package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape

/**
 * A text field, as the screens from before UI v2 ask for one: now the skin's own (docs/ui-v2.md), its
 * [label] over it as a field label, inked, and why what was typed won't do ([errorText]) under it.
 */
@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    errorText: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val shape = pqShape(PqDimens.Gap)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) PqFieldLabel(label)
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PqDimens.TouchMin)
                .clip(shape)
                .background(PqColors.Paper)
                .border(PqDimens.Border, if (errorText != null) PqColors.Error else PqColors.Ink, shape)
                .padding(horizontal = PqDimens.Radius, vertical = PqDimens.Gap),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                textStyle = PqType.Body,
                cursorBrush = SolidColor(PqColors.Ink),
                // The label is drawn above the field: TalkBack hears it as the field's name.
                modifier = Modifier.fillMaxWidth().semantics { if (label != null) contentDescription = label },
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(text = placeholder, style = PqType.Body.copy(color = PqColors.Muted), maxLines = 1)
                    }
                    innerTextField()
                },
            )
        }
        if (errorText != null) {
            Text(text = errorText, style = PqType.Meta.copy(color = PqColors.Error), modifier = Modifier.padding(start = PqDimens.SpaceXs))
        }
    }
}
