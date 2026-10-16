package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.components.ComicProgressBar
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

// The AI Coach before there's enough history for an insight (fewer than 3 days logged).

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
