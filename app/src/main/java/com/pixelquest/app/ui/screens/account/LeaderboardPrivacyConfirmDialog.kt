package com.pixelquest.app.ui.screens.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqBullets
import com.pixelquest.app.ui.components.PqDialog
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * Asked before joining the public leaderboard: what other players will see, what stays
 * private, and acceptance of the Terms of Use & Community Guidelines (P021).
 */
@Composable
fun LeaderboardPrivacyConfirmDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var termsAccepted by remember { mutableStateOf(false) }

    PqDialog(
        title = "Leaderboard privacy",
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirm = "Opt in",
        onConfirm = {
            if (termsAccepted) {
                onConfirm()
            }
        },
        confirmEnabled = termsAccepted,
        dismiss = "Cancel",
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(text = "Other players will see:", style = PqType.BodyStrong)
            PqBullets(listOf("Your display name, \"$displayName\"", "Your current and longest streaks", "Your level and total XP"))
            PqGroupLabel("Stays private")
            Text(
                text = "Your email, real name, profile photo, and personal quest titles remain strictly private and will never be exposed.",
                style = PqType.Body.copy(color = PqColors.Ink2),
            )
            PqGroupLabel("Terms of Use")
            Text(
                text = "By joining, you agree not to use offensive, hateful, or explicit display names, and to play fairly.",
                style = PqType.Meta.copy(color = PqColors.Ink2),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Checkbox) { termsAccepted = !termsAccepted },
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PqColors.Gold,
                        checkmarkColor = PqColors.Ink,
                    ),
                )
                Text(
                    text = stringResource(R.string.terms_agree_checkbox),
                    style = PqType.BodyStrong,
                )
            }
        }
    }
}
