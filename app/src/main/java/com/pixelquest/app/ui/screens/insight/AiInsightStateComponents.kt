package com.pixelquest.app.ui.screens.insight

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.ComicButton
import com.pixelquest.app.ui.components.ComicButtonVariant
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.components.ComicProgressBar
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 21: Theme-dispatching state UI components for:
 * Loading, RateLimited, NotEnoughData, Disabled, and Error states.
 */

@Composable
fun AiInsightLoadingState(
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    when (themeMode) {
        ThemeMode.Comic -> {
            ComicPanel(
                variant = ComicPanelVariant.SKY_BLUE,
                modifier = modifier.fillMaxWidth(),
                contentPadding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 40.sp,
                        modifier = Modifier.rotate(rotation)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "POW! COMPUTING INSIGHTS...",
                        fontFamily = BangersFontFamily,
                        fontSize = 22.sp,
                        color = ComicTokens.SolidBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HQ is synthesizing your habit trajectory!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ComicTokens.TextSecondary,
                        textAlign = TextAlign.Center
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
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFF6366F1),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Generating Habit Insights...",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Consulting Gemini AI to evaluate recent consistency",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        else -> {
            PixelCard(
                variant = PixelPanelVariant.BEIGE,
                modifier = modifier.fillMaxWidth(),
                contentPadding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏳",
                        fontSize = 32.sp,
                        modifier = Modifier.rotate(rotation)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CONSULTING SAGE ORACLE...",
                        style = PixelTypography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SYS.TELEMETRY: SCANNING RECENT QUEST LOGS",
                        style = PixelTypography.labelSmall,
                        color = Color(0xFF10B981),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AiInsightRateLimitedState(
    state: AiInsightUiState.RateLimited,
    themeMode: ThemeMode,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = (state.retryAfterSeconds + 3599) / 3600

    Column(modifier = modifier.fillMaxWidth()) {
        when (themeMode) {
            ThemeMode.Comic -> {
                ComicPanel(
                    variant = ComicPanelVariant.BURNT_ORANGE,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 18.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🛑", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HQ COOLDOWN ACTIVE!",
                                fontFamily = BangersFontFamily,
                                fontSize = 20.sp,
                                color = ComicTokens.SolidBlack
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ComicTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Next transmission opens in ~$hours hour(s)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ComicTokens.TextSecondary
                        )
                    }
                }
            }
            ThemeMode.Light -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD97706))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rate Limit Cooldown",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
            else -> {
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "[RATE LIMIT ENGAGED]",
                            style = PixelTypography.titleMedium,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "COOLDOWN REMAINING: ~${hours}H",
                            style = PixelTypography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // If a prior insight exists, display it as fallback reading!
        if (state.lastInsight != null) {
            Spacer(modifier = Modifier.height(16.dp))
            AiInsightCard(
                insight = state.lastInsight,
                isCached = true,
                canRefresh = false,
                remainingCooldownSeconds = state.retryAfterSeconds,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
fun AiInsightNotEnoughDataState(
    state: AiInsightUiState.NotEnoughData,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    when (themeMode) {
        ThemeMode.Comic -> {
            ComicPanel(
                variant = ComicPanelVariant.LAVENDER,
                modifier = modifier.fillMaxWidth(),
                contentPadding = 20.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "MORE MISSIONS REQUIRED!",
                        fontFamily = BangersFontFamily,
                        fontSize = 20.sp,
                        color = ComicTokens.SolidBlack
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ComicTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    ComicProgressBar(
                        progress = state.progressRatio,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.daysLogged} / ${state.minimumRequiredDays} Days Logged",
                        fontFamily = BangersFontFamily,
                        fontSize = 12.sp,
                        color = ComicTokens.SolidBlack
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "💡 ${state.encouragingTip}",
                        fontSize = 11.sp,
                        color = ComicTokens.TextSecondary
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
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Building Habit Momentum",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PixelProgressBar(
                        progress = state.progressRatio,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.daysLogged} of ${state.minimumRequiredDays} Days Logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🌱 ${state.encouragingTip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF059669)
                    )
                }
            }
        }
        else -> {
            PixelCard(
                variant = PixelPanelVariant.BEIGE,
                modifier = modifier.fillMaxWidth(),
                contentPadding = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "[GATHERING QUEST HISTORY]",
                        style = PixelTypography.titleMedium,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PixelProgressBar(
                        progress = state.progressRatio,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PROGRESS: ${state.daysLogged}/${state.minimumRequiredDays} DAYS LOGGED",
                        style = PixelTypography.labelSmall,
                        color = Color(0xFF60A5FA),
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "► TIP: ${state.encouragingTip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFDE68A)
                    )
                }
            }
        }
    }
}

@Composable
fun AiInsightDisabledState(
    state: AiInsightUiState.Disabled,
    themeMode: ThemeMode,
    onNavigateToSettings: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    when (themeMode) {
        ThemeMode.Comic -> {
            ComicPanel(
                variant = ComicPanelVariant.SURFACE,
                modifier = modifier.fillMaxWidth(),
                contentPadding = 20.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🔒", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI INSIGHTS ARE SLEEPING!",
                        fontFamily = BangersFontFamily,
                        fontSize = 20.sp,
                        color = ComicTokens.SolidBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        fontSize = 12.sp,
                        color = ComicTokens.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (onNavigateToSettings != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ComicButton(
                            text = "ACTIVATE IN SETTINGS",
                            onClick = onNavigateToSettings,
                            variant = ComicButtonVariant.PRIMARY
                        )
                    }
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
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔒", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI Insights Disabled",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    if (onNavigateToSettings != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PixelButton(
                            text = "Open Settings",
                            onClick = onNavigateToSettings,
                            variant = PixelButtonVariant.YELLOW,
                            modifier = Modifier.height(38.dp)
                        )
                    }
                }
            }
        }
        else -> {
            PixelCard(
                variant = PixelPanelVariant.BEIGE,
                modifier = modifier.fillMaxWidth(),
                contentPadding = 20.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🔒", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "[AI COACH DORMANT]",
                        style = PixelTypography.titleMedium,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTheme.colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (onNavigateToSettings != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PixelButton(
                            text = "ENABLE IN SETTINGS",
                            onClick = onNavigateToSettings,
                            variant = PixelButtonVariant.YELLOW,
                            modifier = Modifier.height(38.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The error card's heading. A build without AI set up, or a used-up day, isn't a glitch, so those get
 * their own headings; other failures keep each theme's original one.
 */
internal fun aiErrorHeading(message: String, themeMode: ThemeMode): String {
    val notSetUp = message == com.pixelquest.app.domain.ai.AiErrorCopy.NOT_CONFIGURED
    val usedUp = message == com.pixelquest.app.domain.ai.AiErrorCopy.DAILY_LIMIT
    return when (themeMode) {
        ThemeMode.Comic -> when {
            notSetUp -> "COACH NOT SET UP"
            usedUp -> "THAT'S ALL FOR TODAY!"
            else -> "COMMUNICATION GLITCH!"
        }
        ThemeMode.Light -> when {
            notSetUp -> "AI Coach not set up"
            usedUp -> "Today's insights are used up"
            else -> "Insight Generation Error"
        }
        else -> when {
            notSetUp -> "[COACH OFFLINE]"
            usedUp -> "[DAILY LIMIT REACHED]"
            else -> "[TRANSMISSION FAILED]"
        }
    }
}

@Composable
fun AiInsightErrorState(
    state: AiInsightUiState.Error,
    themeMode: ThemeMode,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (themeMode) {
            ThemeMode.Comic -> {
                ComicPanel(
                    variant = ComicPanelVariant.BURNT_ORANGE,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 18.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = aiErrorHeading(state.message, ThemeMode.Comic),
                            fontFamily = BangersFontFamily,
                            fontSize = 20.sp,
                            color = ComicTokens.SolidBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            fontSize = 12.sp,
                            color = ComicTokens.TextPrimary
                        )
                        if (state.canRetry) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ComicButton(
                                text = "RETRY TRANSMISSION",
                                onClick = onRetry,
                                variant = ComicButtonVariant.PRIMARY
                            )
                        }
                    }
                }
            }
            ThemeMode.Light -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF2F2))
                        .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = aiErrorHeading(state.message, ThemeMode.Light),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D)
                        )
                        if (state.canRetry) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PixelButton(
                                text = "Retry",
                                onClick = onRetry,
                                variant = PixelButtonVariant.YELLOW,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            }
            else -> {
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = aiErrorHeading(state.message, themeMode),
                            style = PixelTypography.titleMedium,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelTheme.colors.onSurface
                        )
                        if (state.canRetry) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PixelButton(
                                text = "RETRY",
                                onClick = onRetry,
                                variant = PixelButtonVariant.YELLOW,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            }
        }

        if (state.fallbackInsight != null) {
            Spacer(modifier = Modifier.height(16.dp))
            AiInsightCard(
                insight = state.fallbackInsight,
                isCached = true,
                canRefresh = true,
                remainingCooldownSeconds = 0L,
                onRefresh = onRetry
            )
        }
    }
}

/**
 * Step 31: Graceful UI component rendered when user reaches the hard daily or monthly API cap.
 * Dispatches natively across Comic, Light, and Pixel themes.
 */
@Composable
fun AiInsightCapReachedState(
    state: AiInsightUiState.CapReached,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (themeMode) {
            ThemeMode.Comic -> {
                ComicPanel(
                    variant = ComicPanelVariant.BURNT_ORANGE,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 20.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "⚡", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.isMonthly) "MONTHLY POWER LIMIT REACHED!" else "DAILY INSIGHT LIMIT REACHED!",
                            fontFamily = BangersFontFamily,
                            fontSize = 19.sp,
                            color = ComicTokens.SolidBlack,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 12.sp,
                            color = ComicTokens.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Rest up and recharge your habit momentum!",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = ComicTokens.SolidBlack.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            ThemeMode.Light -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🛡️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.isMonthly) "Monthly Insight Limit Reached" else "Daily Insight Limit Reached",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 18.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.isMonthly) "[MONTHLY CEILING HIT]" else "[DAILY CEILING HIT]",
                                style = PixelTypography.titleMedium,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "🛡️ SAFEGUARD",
                                style = PixelTypography.labelSmall,
                                color = Color(0xFFF59E0B),
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SYS.SAFEGUARD // API USAGE CAP ACTIVE",
                            style = PixelTypography.labelSmall,
                            color = Color(0xFF60A5FA),
                            fontSize = 9.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "► Momentum is preserved. Your next tactical debrief will be ready after midnight.",
                            style = PixelTypography.bodySmall,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }
        }

        if (state.lastInsight != null) {
            Spacer(modifier = Modifier.height(16.dp))
            AiInsightCard(
                insight = state.lastInsight,
                isCached = true,
                canRefresh = false,
                remainingCooldownSeconds = 0L,
                onRefresh = {}
            )
        }
    }
}

