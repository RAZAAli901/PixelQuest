package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.PixelAvatarFrame
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun OnboardingSummaryScreen(
    username: String,
    avatarId: String,
    difficultyLevel: DifficultyLevel,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📜 HERO SUMMARY",
                style = PixelTypography.titleLarge,
                color = PixelGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Review your character choices before starting your adventure.",
                style = PixelTypography.bodyMedium,
                color = PixelTextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            PixelAvatarFrame(
                avatarId = avatarId,
                level = 1,
                size = 96.dp
            )

            PixelCard(
                variant = PixelPanelVariant.BORDER,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = username.uppercase(),
                        style = PixelTypography.titleMedium,
                        color = PixelGold
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "AVATAR:", style = PixelTypography.bodySmall, color = PixelTextWhite)
                        Text(text = avatarId, style = PixelTypography.bodySmall, color = PixelGreen)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "DIFFICULTY:", style = PixelTypography.bodySmall, color = PixelTextWhite)
                        Text(
                            text = DifficultyMode.getDisplayName(difficultyLevel).uppercase(),
                            style = PixelTypography.bodySmall,
                            color = PixelCyan
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "PERFECT DAY:", style = PixelTypography.bodySmall, color = PixelTextWhite)
                        val pct = (DifficultyMode.getPerfectDayThreshold(difficultyLevel) * 100).toInt()
                        Text(text = "$pct%", style = PixelTypography.bodySmall, color = PixelGold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PixelButton(
                text = "◀ BACK",
                onClick = onBackClick,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.weight(1f)
            )
            PixelButton(
                text = "BEGIN YOUR QUEST ▶",
                onClick = onConfirmClick,
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
