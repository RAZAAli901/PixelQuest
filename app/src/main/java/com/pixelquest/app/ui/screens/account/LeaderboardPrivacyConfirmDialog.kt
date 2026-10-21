package com.pixelquest.app.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PqBullets
import com.pixelquest.app.ui.components.PqDialog
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * Asked before joining the public leaderboard: what other players will see, and what stays
 * private.
 */
@Composable
fun LeaderboardPrivacyConfirmDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PqDialog(
        title = "Leaderboard privacy",
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirm = "Opt in",
        onConfirm = onConfirm,
        dismiss = "Cancel",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(text = "Other players will see:", style = PqType.BodyStrong)
            PqBullets(listOf("Your display name, \"$displayName\"", "Your current and longest streaks", "Your level and total XP"))
            PqGroupLabel("Stays private")
            Text(
                text = "Your email, real name, profile photo, and personal quest titles remain strictly private and will never be exposed.",
                style = PqType.Body.copy(color = PqColors.Ink2)
            )
        }
    }
}
