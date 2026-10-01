package com.pixelquest.app.ui.screens.insight

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

/**
 * Step 18: Pixel-mode visual treatment for AI Habit Insights.
 * Emphasizes 8-bit retro arcade identity: CRT monitor vibe, dark background,
 * scanline aesthetics, golden quest badges, and terminal-style telemetry headers.
 */
@Composable
fun PixelAiInsightView(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val soundManager = LocalSoundManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Retro Arcade Questmaster Header Banner
        PixelCard(
            variant = PixelPanelVariant.BEIGE,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 8-bit Sage Avatar frame
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF2E1065))
                        .border(2.dp, Color(0xFFFBBF24)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🧙", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "QUESTMASTER COACH",
                        style = PixelTypography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SYS.GEMINI.AI // ONLINE",
                            style = PixelTypography.labelSmall,
                            color = Color(0xFF10B981),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // State-driven body
        when (uiState) {
            is AiInsightUiState.Success -> {
                PixelSuccessCard(
                    uiState = uiState,
                    onRefresh = {
                        soundManager?.playClickSound()
                        onRefresh()
                    }
                )
            }
            else -> {
                // Dispatches non-success states through the state router
                AiInsightStateRouter(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Pixel,
                    onRefresh = onRefresh,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
fun PixelSuccessCard(
    uiState: AiInsightUiState.Success,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val insight = uiState.insight
    val colors = PixelTheme.colors

    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Arcade scanline title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[QUEST TELEMETRY]",
                    style = PixelTypography.labelMedium,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .background(if (uiState.isCached) Color(0xFF1E3A8A) else Color(0xFF065F46))
                        .border(1.dp, if (uiState.isCached) Color(0xFF60A5FA) else Color(0xFF34D399))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (uiState.isCached) "CACHED" else "FRESH",
                        style = PixelTypography.labelSmall,
                        color = if (uiState.isCached) Color(0xFF93C5FD) else Color(0xFF6EE7B7),
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 1: Observation Scan
            PixelTelemetryBlock(
                tag = "► OBSERVATION SCAN",
                body = insight.summary,
                tagColor = Color(0xFF60A5FA),
                bodyColor = colors.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Strategic Protocol
            PixelTelemetryBlock(
                tag = "► STRATEGIC PROTOCOL",
                body = insight.suggestion,
                tagColor = Color(0xFF34D399),
                bodyColor = colors.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Heroic Blessing
            PixelTelemetryBlock(
                tag = "► HEROIC BLESSING",
                body = insight.encouragement,
                tagColor = Color(0xFFFBBF24),
                bodyColor = Color(0xFFFDE68A)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Terminal Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedTime = rememberDateFormatted(insight.generatedAt)
                Text(
                    text = "LOGGED: $formattedTime",
                    style = PixelTypography.labelSmall,
                    color = colors.onSurface.copy(alpha = 0.6f),
                    fontSize = 9.sp
                )

                PixelButton(
                    text = if (uiState.canRefresh) "REFRESH" else "COOLDOWN",
                    variant = if (uiState.canRefresh) PixelButtonVariant.YELLOW else PixelButtonVariant.BLUE,
                    onClick = onRefresh,
                    enabled = uiState.canRefresh,
                    modifier = Modifier.height(36.dp)
                )
            }

            if (!uiState.canRefresh && uiState.remainingCooldownSeconds > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                val hours = (uiState.remainingCooldownSeconds + 3599) / 3600
                Text(
                    text = "RECHARGE IN ~${hours}H",
                    style = PixelTypography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun PixelTelemetryBlock(
    tag: String,
    body: String,
    tagColor: Color,
    bodyColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = tag,
            style = PixelTypography.labelMedium,
            color = tagColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                .border(1.dp, tagColor.copy(alpha = 0.3f))
                .padding(10.dp)
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
                lineHeight = 20.sp
            )
        }
    }
}
