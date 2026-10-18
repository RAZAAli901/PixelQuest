package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * The Namaz screen's snackbar: a question with its answer, such as taking a prayed mark back.
 * PixelSnackbar is for warnings (a red ⚠️ line), which these aren't.
 */
@Composable
internal fun NamazSnackbar(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelCard(variant = PixelPanelVariant.BORDER, contentPadding = 12.dp, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = PixelTheme.colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null) {
                PixelButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
