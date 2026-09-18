package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

/**
 * Step 34: Polished explanatory enable-dialog for Simple Mode with proper
 * theme-aware pixel styling, structured feature breakdown, and clear reassurance.
 */
@Composable
fun SimpleModeEnableDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val haptic = LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss) {
        PixelCard(
            modifier = modifier.fillMaxWidth(0.95f),
            variant = PixelPanelVariant.BORDER,
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.primaryContainer.copy(alpha = 0.25f))
                        .border(1.dp, colors.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📋 SIMPLE MODE",
                        style = PixelTypography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Streamlines PixelQuest into a clean, minimalist task checklist without gamification pressure.",
                    style = PixelTypography.bodyMedium,
                    color = colors.onSurface,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Explanatory breakdown items
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExplainingRow(
                        icon = "🙈",
                        title = "Hidden UI",
                        description = "Streaks, XP points, levels, and celebratory popups are suppressed.",
                        colors = colors
                    )
                    ExplainingRow(
                        icon = "🛡️",
                        title = "Safe Tracking",
                        description = "100% of habit data and streaks continue recording silently in the background.",
                        colors = colors
                    )
                    ExplainingRow(
                        icon = "🔄",
                        title = "Reversible Anytime",
                        description = "Return to Full Game Mode whenever you like with all progress intact.",
                        colors = colors
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PixelButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        variant = PixelButtonVariant.BLUE,
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = "ENABLE",
                        onClick = {
                            PixelHaptics.performSuccessPattern(haptic)
                            onConfirm()
                        },
                        variant = PixelButtonVariant.YELLOW,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplainingRow(
    icon: String,
    title: String,
    description: String,
    colors: com.pixelquest.app.ui.theme.AppColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = PixelTypography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(
                text = title,
                style = PixelTypography.labelMedium,
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = PixelTypography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
