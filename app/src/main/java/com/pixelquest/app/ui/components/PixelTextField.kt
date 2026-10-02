package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelTypography

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
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val activeMode = com.pixelquest.app.ui.theme.PixelTheme.mode
    if (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        ComicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = label,
            placeholder = placeholder,
            errorText = errorText,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation
        )
        return
    }

    // Theme roles, not fixed Pixel colours: in Pixel they resolve to the same gold, white, muted and
    // red, and in Light they give dark text on the white field instead of white on white.
    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = PixelTypography.labelLarge,
                color = colors.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            contentPadding = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                textStyle = PixelTypography.bodyMedium.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = PixelTypography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
        }
        if (errorText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                style = PixelTypography.bodySmall,
                color = colors.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
