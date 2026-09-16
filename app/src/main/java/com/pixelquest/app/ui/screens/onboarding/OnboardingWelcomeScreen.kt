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
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun OnboardingWelcomeScreen(
    onStartClick: () -> Unit = {}
) {
    val colors = PixelTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "⚔️ WELCOME HERO ⚔️",
                style = PixelTypography.titleLarge,
                color = colors.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Turn your daily routines into retro RPG quests!",
                style = PixelTypography.bodyMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )

            PixelCard(
                variant = PixelPanelVariant.BORDER,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📜 ", style = PixelTypography.titleMedium)
                        Column {
                            Text(text = "DAILY QUESTS", style = PixelTypography.titleSmall, color = colors.primary)
                            Text(text = "Set daily habits and schedule alarm reminders.", style = PixelTypography.bodySmall, color = colors.onSurface)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥 ", style = PixelTypography.titleMedium)
                        Column {
                            Text(text = "BUILD STREAKS", style = PixelTypography.titleSmall, color = colors.secondary)
                            Text(text = "Maintain consecutive perfect days for bonus XP.", style = PixelTypography.bodySmall, color = colors.onSurface)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛡️ ", style = PixelTypography.titleMedium)
                        Column {
                            Text(text = "LEVEL UP HERO", style = PixelTypography.titleSmall, color = colors.tertiary)
                            Text(text = "Earn levels, unlock avatars, and climb history.", style = PixelTypography.bodySmall, color = colors.onSurface)
                        }
                    }
                }
            }
        }

        PixelButton(
            text = "START YOUR JOURNEY ▶",
            onClick = onStartClick,
            variant = PixelButtonVariant.YELLOW,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
    }
}
