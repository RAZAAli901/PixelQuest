package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Step 17: Base AI Insight Card layout.
 * Structured with Summary, Suggestion, and Encouragement sections,
 * rendered inside the established dispatch-aware [PixelCard] and interactive [PixelButton].
 */
@Composable
fun AiInsightCard(
    insight: HabitInsightResponse,
    isCached: Boolean,
    canRefresh: Boolean,
    remainingCooldownSeconds: Long,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode = PixelTheme.mode
    val colors = PixelTheme.colors
    val typography = MaterialTheme.typography

    val dateFormatted = rememberDateFormatted(insight.generatedAt)

    PixelCard(
        modifier = modifier.fillMaxWidth(),
        variant = PixelPanelVariant.BEIGE,
        comicVariant = ComicPanelVariant.SURFACE,
        contentPadding = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row: Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✦",
                        color = colors.primaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (themeMode) {
                            ThemeMode.Comic -> "COACH INTEL"
                            ThemeMode.Light -> "Habit Analysis"
                            else -> "QUESTMASTER LOG"
                        },
                        style = typography.titleMedium,
                        color = colors.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                }

                InsightStatusBadge(isCached = isCached)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 1: Summary / Observation
            InsightSectionBlock(
                title = when (themeMode) {
                    ThemeMode.Comic -> "THE SITUATION"
                    ThemeMode.Light -> "Observation"
                    else -> "STATUS SCAN"
                },
                content = insight.summary,
                accentColor = colors.secondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Suggestion / Strategy
            InsightSectionBlock(
                title = when (themeMode) {
                    ThemeMode.Comic -> "TACTICAL MOVE"
                    ThemeMode.Light -> "Suggestion"
                    else -> "TACTICAL ADVICE"
                },
                content = insight.suggestion,
                accentColor = colors.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Encouragement / Hero Cheer
            InsightSectionBlock(
                title = when (themeMode) {
                    ThemeMode.Comic -> "POWER SURGE"
                    ThemeMode.Light -> "Encouragement"
                    else -> "QUEST BLESSING"
                },
                content = insight.encouragement,
                accentColor = Color(0xFF10B981)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Timestamp + Refresh Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Generated $dateFormatted",
                    style = typography.labelSmall,
                    color = colors.onSurface.copy(alpha = 0.6f)
                )

                PixelButton(
                    text = if (canRefresh) "REFRESH" else "COOLDOWN",
                    variant = if (canRefresh) PixelButtonVariant.YELLOW else PixelButtonVariant.BLUE,
                    onClick = onRefresh,
                    enabled = canRefresh
                )
            }

            if (!canRefresh && remainingCooldownSeconds > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                val hours = (remainingCooldownSeconds + 3599) / 3600
                Text(
                    text = "Next live update ready in ~${hours}h",
                    style = typography.labelSmall,
                    color = colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun InsightSectionBlock(
    title: String,
    content: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val typography = MaterialTheme.typography

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = typography.bodyMedium,
            color = colors.onSurface,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
fun InsightStatusBadge(
    isCached: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeBg = if (isCached) Color(0xFF3B82F6) else Color(0xFF10B981)
    val label = if (isCached) "CACHED" else "LIVE"

    Box(
        modifier = modifier
            .background(badgeBg.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = badgeBg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun rememberDateFormatted(timestamp: Long): String {
    return androidx.compose.runtime.remember(timestamp) {
        if (timestamp <= 0L) return@remember "recently"
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    }
}
