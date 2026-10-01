package com.pixelquest.app.ui.screens.settings

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
 * Informed consent and privacy disclosure dialog for AI Habit Insights.
 * Explains data minimization, anonymized metrics sent to Google Gemini,
 * and zero PII transmission guarantee.
 */
@Composable
fun AiInsightsPrivacyConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    PixelDialog(
        title = "AI INSIGHTS & PRIVACY",
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "PixelQuest uses Google Gemini to analyze completion patterns and generate actionable habit coaching debriefs.",
                style = PixelTypography.bodySmall,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "📊 WHAT IS ANALYZED:",
                style = PixelTypography.bodyMedium,
                color = colors.primary
            )
            Text(
                text = "• Anonymized streak statistics (days, completion rates)",
                style = PixelTypography.bodySmall,
                color = colors.onSurface
            )
            Text(
                text = "• Habit categories (e.g. Fitness, Learning, Health)",
                style = PixelTypography.bodySmall,
                color = colors.onSurface
            )
            Text(
                text = "• Current hero level and difficulty mode",
                style = PixelTypography.bodySmall,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "🛡️ PRIVACY GUARANTEE:",
                style = PixelTypography.bodyMedium,
                color = colors.primary
            )
            Text(
                text = "• ZERO Personal Identifiable Information (PII) is shared.",
                style = PixelTypography.bodySmall,
                color = colors.tertiary
            )
            Text(
                text = "• Custom quest titles, descriptions, and notes are NEVER transmitted.",
                style = PixelTypography.bodySmall,
                color = colors.tertiary
            )
            Text(
                text = "• Results are cached locally and rate-limited to avoid excessive cloud calls.",
                style = PixelTypography.bodySmall,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            PixelButton(
                text = "✅ ACCEPT & OPT IN",
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
