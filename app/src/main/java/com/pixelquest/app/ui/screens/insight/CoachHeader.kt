package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

// The AI Coach screen's header in each theme: the coach's avatar, its name and a status.

/** Pixel: the Questmaster, or the Habit Coach in Simple Mode. */
@Composable
fun PixelCoachHeader(isSimpleMode: Boolean) {
    val colors = PixelTheme.colors
    // Retro Arcade Questmaster / Habit Coach Header Banner
    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = CoachDimens.Header.PixelPadding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar frame: Wizard/Sage in gamified mode, Nature sprout in simple mode
            Box(
                modifier = Modifier
                    .size(CoachDimens.Header.PixelAvatar)
                    .background(if (isSimpleMode) Color(0xFF064E3B) else Color(0xFF2E1065))
                    .border(2.dp, if (isSimpleMode) Color(0xFF10B981) else Color(0xFFFBBF24)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isSimpleMode) "🌱" else "🧙", fontSize = CoachDimens.Header.PixelAvatarEmoji)
            }

            Spacer(modifier = Modifier.width(CoachDimens.Header.PixelAvatarGap))

            Text(
                text = if (isSimpleMode) "HABIT COACH" else "QUESTMASTER COACH",
                style = PixelTypography.titleSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(CoachDimens.Header.ChipGap))
            // The status, inline: it was a second line under the title.
            Row(
                modifier = Modifier
                    .border(1.dp, Color(0xFF10B981))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.coach_status_online),
                    style = PixelTypography.labelSmall,
                    color = Color(0xFF10B981),
                    fontSize = CoachDimens.Header.StatusText,
                    maxLines = 1
                )
            }
        }
    }
}

/** Comic: the oracle's panel with its issue chip. */
@Composable
fun ComicCoachHeader() {
    // Comic Coach Header Banner
    ComicPanel(
        variant = ComicPanelVariant.BURNT_ORANGE,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = CoachDimens.Header.ComicPadding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Comic Hero Oracle Avatar
            Box(
                modifier = Modifier
                    .size(CoachDimens.Header.ComicAvatar)
                    .clip(CircleShape)
                    .background(ComicTokens.PanelSurface)
                    .border(2.5.dp, ComicTokens.SolidBlack, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💥", fontSize = CoachDimens.Header.ComicAvatarEmoji)
            }

            Spacer(modifier = Modifier.width(CoachDimens.Header.AvatarGap))

            Text(
                text = "COACH ORACLE SPEAKS!",
                fontFamily = BangersFontFamily,
                fontSize = CoachDimens.Header.ComicTitle,
                letterSpacing = 0.8.sp,
                color = ComicTokens.SolidBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(CoachDimens.Header.ChipGap))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ComicTokens.GoldAccent)
                    .border(2.dp, ComicTokens.SolidBlack, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ISSUE #25",
                    fontFamily = BangersFontFamily,
                    fontSize = CoachDimens.Header.ComicChip,
                    color = ComicTokens.SolidBlack
                )
            }
        }
    }
}

/** Light: a clean card with the coach's sprout. */
@Composable
fun LightCoachHeader() {
    val colors = PixelTheme.colors
    val typography = MaterialTheme.typography
    // Clean Daytime Coach Header Banner
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(CoachDimens.Header.LightPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern clean coach avatar frame
            Box(
                modifier = Modifier
                    .size(CoachDimens.Header.LightAvatar)
                    .clip(CircleShape)
                    .background(Color(0xFFEEF2FF))
                    .border(1.5.dp, Color(0xFF6366F1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🌱", fontSize = CoachDimens.Header.LightAvatarEmoji)
            }

            Spacer(modifier = Modifier.width(CoachDimens.Header.AvatarGap))

            Text(
                text = "Habit Insights & Coaching",
                style = typography.titleSmall,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(CoachDimens.Header.ChipGap))
            Text(
                text = stringResource(R.string.coach_status_light),
                style = typography.labelSmall,
                color = Color(0xFF4F46E5),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEEF2FF))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
