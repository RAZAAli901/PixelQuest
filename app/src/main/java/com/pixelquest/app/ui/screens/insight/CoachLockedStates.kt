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

// The AI Coach when it can't be used yet: switched off in Settings, or nobody is signed in.

@Composable
fun AiInsightDisabledState(
    state: AiInsightUiState.Disabled,
    themeMode: ThemeMode,
    onNavigateToSettings: (() -> Unit)?,
    modifier: Modifier = Modifier
) = AiInsightLockedState(
    themeMode = themeMode,
    icon = "🔒",
    heading = when (themeMode) {
        ThemeMode.Comic -> "AI INSIGHTS ARE SLEEPING!"
        ThemeMode.Light -> "AI Insights Disabled"
        else -> "[AI COACH DORMANT]"
    },
    message = state.message,
    buttonText = when (themeMode) {
        ThemeMode.Comic -> "ACTIVATE IN SETTINGS"
        ThemeMode.Light -> "Open Settings"
        else -> "ENABLE IN SETTINGS"
    },
    onClick = onNavigateToSettings,
    modifier = modifier
)

/** Nobody is signed in: the AI Coach is for signed-in players, so the card leads to Account. */
@Composable
fun AiInsightSignInRequiredState(
    state: AiInsightUiState.SignInRequired,
    themeMode: ThemeMode,
    onNavigateToAccount: (() -> Unit)?,
    modifier: Modifier = Modifier
) = AiInsightLockedState(
    themeMode = themeMode,
    icon = "🔐",
    heading = when (themeMode) {
        ThemeMode.Comic -> "SIGN IN TO UNLOCK!"
        ThemeMode.Light -> "Sign in to use the AI Coach"
        else -> "[SIGN-IN REQUIRED]"
    },
    message = state.message,
    buttonText = when (themeMode) {
        ThemeMode.Comic -> "SIGN IN"
        ThemeMode.Light -> "Sign in"
        else -> "SIGN IN"
    },
    onClick = onNavigateToAccount,
    modifier = modifier
)

@Composable
internal fun AiInsightLockedState(
    themeMode: ThemeMode,
    icon: String,
    heading: String,
    message: String,
    buttonText: String,
    onClick: (() -> Unit)?,
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
                    Text(text = icon, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = heading,
                        fontFamily = BangersFontFamily,
                        fontSize = 20.sp,
                        color = ComicTokens.SolidBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = ComicTokens.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (onClick != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ComicButton(
                            text = buttonText,
                            onClick = onClick,
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
                    Text(text = icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    if (onClick != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PixelButton(
                            text = buttonText,
                            onClick = onClick,
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
                    Text(text = icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = heading,
                        style = PixelTypography.titleMedium,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTheme.colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (onClick != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PixelButton(
                            text = buttonText,
                            onClick = onClick,
                            variant = PixelButtonVariant.YELLOW,
                            modifier = Modifier.height(38.dp)
                        )
                    }
                }
            }
        }
    }
}
