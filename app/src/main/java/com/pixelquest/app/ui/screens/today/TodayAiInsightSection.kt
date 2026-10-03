package com.pixelquest.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.components.ComicButton
import com.pixelquest.app.ui.components.ComicButtonVariant
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.screens.insight.AiInsightCard
import com.pixelquest.app.ui.screens.insight.AiInsightDisabledState
import com.pixelquest.app.ui.screens.insight.AiInsightErrorState
import com.pixelquest.app.ui.screens.insight.AiInsightLoadingState
import com.pixelquest.app.ui.screens.insight.AiInsightNotEnoughDataState
import com.pixelquest.app.ui.screens.insight.AiInsightRateLimitedState
import com.pixelquest.app.ui.screens.insight.AiInsightUiState
import com.pixelquest.app.ui.screens.insight.AiInsightViewModel
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 24: Contextual AI Habit Insights card surfaced on the Today dashboard.
 * Seamlessly integrates into TodayScreen's quest feed, dynamically rendering
 * insights or an opt-in prompt across Pixel, Light, and Comic modes.
 */
@Composable
fun TodayAiInsightSection(
    onNavigateToAiInsight: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiInsightViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSimpleMode by viewModel.isSimpleMode.collectAsState()
    val themeMode = PixelTheme.mode

    Box(modifier = modifier.fillMaxWidth()) {
        when (val state = uiState) {
            is AiInsightUiState.Success -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AiInsightCard(
                        insight = state.insight,
                        isCached = state.isCached,
                        canRefresh = state.canRefresh,
                        remainingCooldownSeconds = state.remainingCooldownSeconds,
                        onRefresh = { viewModel.refreshInsight() }
                    )
                }
            }
            is AiInsightUiState.Loading -> {
                AiInsightLoadingState(themeMode = themeMode)
            }
            is AiInsightUiState.RateLimited -> {
                AiInsightRateLimitedState(
                    state = state,
                    themeMode = themeMode,
                    onRefresh = { viewModel.refreshInsight() }
                )
            }
            is AiInsightUiState.NotEnoughData -> {
                AiInsightNotEnoughDataState(
                    state = state,
                    themeMode = themeMode
                )
            }
            is AiInsightUiState.Disabled -> {
                TodayAiInsightOptInCard(
                    themeMode = themeMode,
                    isSimpleMode = isSimpleMode,
                    onEnableClick = onNavigateToSettings
                )
            }
            is AiInsightUiState.CapReached -> {
                com.pixelquest.app.ui.screens.insight.AiInsightCapReachedState(
                    state = state,
                    themeMode = themeMode
                )
            }
            is AiInsightUiState.Error -> {
                AiInsightErrorState(
                    state = state,
                    themeMode = themeMode,
                    onRetry = { viewModel.refreshInsight() }
                )
            }
        }
    }
}

@Composable
fun TodayAiInsightOptInCard(
    themeMode: ThemeMode,
    onEnableClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean = false
) {
    when (themeMode) {
        ThemeMode.Comic -> {
            ComicPanel(
                variant = ComicPanelVariant.BURNT_ORANGE,
                modifier = modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isSimpleMode) "🌱" else "💥", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSimpleMode) "UNLOCK AI HABIT COACH!" else "UNLOCK AI QUESTMASTER!",
                            fontFamily = BangersFontFamily,
                            fontSize = 18.sp,
                            color = ComicTokens.SolidBlack
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSimpleMode) {
                            "Get daily habit analysis, sustainable routines, and thoughtful guidance powered by Google Gemini."
                        } else {
                            "Get daily tactical debriefs, habit analysis, and power surges powered by Google Gemini."
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ComicTokens.SolidBlack.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ComicButton(
                        text = "ENABLE IN SETTINGS",
                        onClick = onEnableClick,
                        variant = ComicButtonVariant.PRIMARY
                    )
                }
            }
        }
        ThemeMode.Light -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF6366F1))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSimpleMode) "AI Habit Coach Available" else "AI Habit Insights Available",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSimpleMode) {
                            "Receive personalized habit guidance, rhythm tracking, and consistency analysis. Enable anytime in Settings."
                        } else {
                            "Receive personalized habit guidance and consistency analysis. Enable anytime in Settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PixelButton(
                        text = "Enable in Settings",
                        onClick = onEnableClick,
                        variant = PixelButtonVariant.YELLOW
                    )
                }
            }
        }
        else -> {
            PixelCard(
                variant = PixelPanelVariant.BEIGE,
                modifier = modifier.fillMaxWidth(),
                contentPadding = 14.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isSimpleMode) "🌱" else "✦", color = Color(0xFFFBBF24), fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSimpleMode) "HABIT COACH INSIGHTS" else "QUESTMASTER AI INTEL",
                            style = PixelTypography.titleSmall,
                            color = com.pixelquest.app.ui.theme.inkOnPanel(PixelTheme.colors.primary),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSimpleMode) {
                            "Receive personalized habit guidance and consistency analysis powered by Gemini AI."
                        } else {
                            "Receive tactical debriefs and streak coaching powered by Gemini AI."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = PixelTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PixelButton(
                        text = "ENABLE IN SETTINGS",
                        onClick = onEnableClick,
                        variant = PixelButtonVariant.YELLOW,
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }
    }
}
