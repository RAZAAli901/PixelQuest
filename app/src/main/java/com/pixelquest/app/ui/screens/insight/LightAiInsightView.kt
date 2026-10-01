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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * Step 19: Light-mode visual treatment for AI Habit Insights.
 * Emphasizes clean, daylight readability: crisp paper surfaces, slate borders,
 * high-contrast typography, and purposeful accent indicators.
 */
@Composable
fun LightAiInsightView(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val typography = MaterialTheme.typography
    val soundManager = LocalSoundManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clean Daytime Coach Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modern clean coach avatar frame
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF2FF))
                        .border(1.5.dp, Color(0xFF6366F1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌱", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Habit Insights & Coaching",
                        style = typography.titleMedium,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Personalized feedback powered by Gemini AI",
                        style = typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State dispatch
        when (uiState) {
            is AiInsightUiState.Success -> {
                LightSuccessCard(
                    uiState = uiState,
                    onRefresh = {
                        soundManager?.playClickSound()
                        onRefresh()
                    }
                )
            }
            else -> {
                AiInsightStateRouter(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Light,
                    onRefresh = onRefresh,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
fun LightSuccessCard(
    uiState: AiInsightUiState.Success,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val insight = uiState.insight
    val typography = MaterialTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Habit Evaluation",
                        style = typography.titleSmall,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (uiState.isCached) Color(0xFFEFF6FF) else Color(0xFFECFDF5))
                        .border(
                            1.dp,
                            if (uiState.isCached) Color(0xFFBFDBFE) else Color(0xFFA7F3D0),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (uiState.isCached) "Cached" else "Fresh",
                        color = if (uiState.isCached) Color(0xFF1D4ED8) else Color(0xFF047857),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Observation
            LightInsightSection(
                title = "Observation",
                body = insight.summary,
                accentColor = Color(0xFF3B82F6),
                backgroundColor = Color(0xFFF8FAFC)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Section 2: Strategy / Suggestion
            LightInsightSection(
                title = "Actionable Strategy",
                body = insight.suggestion,
                accentColor = Color(0xFF10B981),
                backgroundColor = Color(0xFFF0FDF4)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Section 3: Encouragement
            LightInsightSection(
                title = "Encouragement",
                body = insight.encouragement,
                accentColor = Color(0xFF8B5CF6),
                backgroundColor = Color(0xFFF5F3FF)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedTime = rememberDateFormatted(insight.generatedAt)
                Text(
                    text = "Generated on $formattedTime",
                    style = typography.labelSmall,
                    color = Color(0xFF64748B)
                )

                PixelButton(
                    text = if (uiState.canRefresh) "Refresh Insight" else "Cooldown",
                    variant = if (uiState.canRefresh) PixelButtonVariant.YELLOW else PixelButtonVariant.BLUE,
                    onClick = onRefresh,
                    enabled = uiState.canRefresh,
                    modifier = Modifier.height(38.dp)
                )
            }

            if (!uiState.canRefresh && uiState.remainingCooldownSeconds > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                val hours = (uiState.remainingCooldownSeconds + 3599) / 3600
                Text(
                    text = "Next insight available in ~$hours hours",
                    style = typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun LightInsightSection(
    title: String,
    body: String,
    accentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = typography.bodyMedium,
            color = Color(0xFF1E293B),
            lineHeight = 21.sp
        )
    }
}
