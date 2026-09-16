package com.pixelquest.app.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelDialog
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

/**
 * Confirmation dialog shown before enabling public leaderboard participation.
 * Explicitly breaks down public vs private data.
 */
@Composable
fun LeaderboardPrivacyConfirmDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    PixelDialog(
        title = "LEADERBOARD PRIVACY",
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "The following will become publicly visible to other players:",
                style = PixelTypography.bodyMedium,
                color = colors.primary
            )
            Text(
                text = "• Display Name: \"$displayName\"",
                style = PixelTypography.bodySmall,
                color = colors.tertiary
            )
            Text(
                text = "• Current & Longest Streaks",
                style = PixelTypography.bodySmall,
                color = colors.onSurface
            )
            Text(
                text = "• Current Level & Total XP",
                style = PixelTypography.bodySmall,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "🛡️ PRIVACY GUARANTEE:",
                style = PixelTypography.bodyMedium,
                color = colors.primary
            )
            Text(
                text = "Your Google email, real name, profile photo, and personal quest titles remain strictly private and will never be exposed.",
                style = PixelTypography.bodySmall,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            PixelButton(
                text = "✅ CONFIRM & OPT IN",
                onClick = onConfirm,
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.fillMaxWidth()
            )

            PixelButton(
                text = "❌ CANCEL",
                onClick = onDismiss,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
