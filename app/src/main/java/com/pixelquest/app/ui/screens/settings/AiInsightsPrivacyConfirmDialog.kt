package com.pixelquest.app.ui.screens.settings

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

/** Shown in builds that reach Gemini through the gemini-proxy Edge Function (all release builds). */
internal const val AI_PROXY_PRIVACY_NOTE =
    "• Requests pass through PixelQuest's server, which keeps the AI key private. The AI Coach is for " +
        "signed-in players, so the server checks your sign-in and counts your calls per account per day. " +
        "Your name and email are never put in the prompt."

/**
 * Asked before the AI Coach is turned on: what Google Gemini is given (anonymised numbers, never
 * quest titles or anything that names the player), and how often.
 */
@Composable
fun AiInsightsPrivacyConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PqDialog(
        title = "AI Coach and privacy",
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirm = "Turn on",
        onConfirm = onConfirm,
        dismiss = "Cancel",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PixelQuest uses Google Gemini to look at your completion patterns and write habit coaching briefs.",
                style = PqType.Body
            )
            PqGroupLabel("What it looks at")
            PqBullets(
                listOf(
                    "Anonymised streak numbers (days, completion rates)",
                    "Habit categories (Fitness, Learning, Health and so on)",
                    "Your hero level and difficulty"
                )
            )
            PqGroupLabel("What it never sees")
            PqBullets(
                listOf(
                    "Anything that identifies you",
                    "Your quest titles, descriptions and notes"
                )
            )
            Text(text = "Briefs are kept on your phone and limited, so it isn't asked too often.", style = PqType.Meta)
            if (com.pixelquest.app.BuildConfig.GEMINI_VIA_PROXY) {
                Text(text = AI_PROXY_PRIVACY_NOTE, style = PqType.Meta.copy(color = PqColors.Ink2))
            }
        }
    }
}
