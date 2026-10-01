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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.ui.components.ComicButton
import com.pixelquest.app.ui.components.ComicButtonVariant
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 20: Comic-mode visual treatment for AI Habit Insights.
 * Renders the coach feedback inside pop-art speech/thought-bubble panels,
 * complete with dynamic comic typography (Bangers), bold black ink outlines,
 * flat drop shadows, and dramatic action bubbles.
 */
@Composable
fun ComicAiInsightView(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Comic Coach Header Banner
        ComicPanel(
            variant = ComicPanelVariant.BURNT_ORANGE,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Comic Hero Oracle Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ComicTokens.PanelSurface)
                        .border(2.5.dp, ComicTokens.SolidBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💥", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COACH ORACLE SPEAKS!",
                        fontFamily = BangersFontFamily,
                        fontSize = 22.sp,
                        letterSpacing = 0.8.sp,
                        color = ComicTokens.SolidBlack
                    )
                    Text(
                        text = "TACTICAL HABIT BRIEFING",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ComicTokens.SolidBlack.copy(alpha = 0.75f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ComicTokens.GoldAccent)
                        .border(2.dp, ComicTokens.SolidBlack, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ISSUE #25",
                        fontFamily = BangersFontFamily,
                        fontSize = 13.sp,
                        color = ComicTokens.SolidBlack
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State dispatch
        when (uiState) {
            is AiInsightUiState.Success -> {
                ComicSuccessView(
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
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Comic,
                    onRefresh = onRefresh,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
fun ComicSuccessView(
    uiState: AiInsightUiState.Success,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val insight = uiState.insight

    Column(modifier = modifier.fillMaxWidth()) {
        // Speech Bubble 1: The Situation (Summary)
        ComicSpeechBubble(
            title = "THE DISPATCH!",
            body = insight.summary,
            tagColor = ComicTokens.BurntOrange,
            bubbleBg = ComicTokens.PanelSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Speech Bubble 2: Action Directive (Suggestion)
        ComicSpeechBubble(
            title = "HEROIC DIRECTIVE!",
            body = insight.suggestion,
            tagColor = ComicTokens.SkyBlue,
            bubbleBg = Color(0xFFF0F9FF)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Speech Bubble 3: Power Surge (Encouragement)
        ComicSpeechBubble(
            title = "POWER SURGE!!",
            body = insight.encouragement,
            tagColor = ComicTokens.GoldAccent,
            bubbleBg = Color(0xFFFFFBEB)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Comic Action Footer Panel
        ComicPanel(
            variant = ComicPanelVariant.SURFACE,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val formattedTime = rememberDateFormatted(insight.generatedAt)
                    Text(
                        text = if (uiState.isCached) "ARCHIVED TRANSMISSION" else "FRESH FROM HQ!",
                        fontFamily = BangersFontFamily,
                        fontSize = 14.sp,
                        color = if (uiState.isCached) ComicTokens.SkyBlue else ComicTokens.CoralRed
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ComicTokens.TextSecondary
                    )
                }

                ComicButton(
                    text = if (uiState.canRefresh) "REFRESH INTEL!" else "RECHARGING...",
                    onClick = onRefresh,
                    variant = if (uiState.canRefresh) ComicButtonVariant.PRIMARY else ComicButtonVariant.SKY_BLUE,
                    enabled = uiState.canRefresh
                )
            }
        }

        if (!uiState.canRefresh && uiState.remainingCooldownSeconds > 0L) {
            Spacer(modifier = Modifier.height(6.dp))
            val hours = (uiState.remainingCooldownSeconds + 3599) / 3600
            Text(
                text = "Next transmission ready in ~$hours hrs!",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ComicTokens.TextSecondary,
                modifier = Modifier.align(Alignment.End).padding(end = 4.dp)
            )
        }
    }
}

/**
 * Speech-bubble container with comic black border, flat black drop shadow,
 * and a stylized bubble title badge.
 */
@Composable
fun ComicSpeechBubble(
    title: String,
    body: String,
    tagColor: Color,
    bubbleBg: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
            .comicDropShadow(offsetX = 4.dp, offsetY = 4.dp, shape = shape)
            .background(bubbleBg, shape)
            .comicBorder(width = 2.5.dp, color = ComicTokens.SolidBlack, shape = shape)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Speech Bubble Pointer / Title Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagColor)
                        .border(1.5.dp, ComicTokens.SolidBlack, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = BangersFontFamily,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = ComicTokens.SolidBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = body,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ComicTokens.TextPrimary
            )
        }
    }
}
