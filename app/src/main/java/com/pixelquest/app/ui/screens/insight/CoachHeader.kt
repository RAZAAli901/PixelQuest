package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.sizeIn
import com.pixelquest.app.ui.theme.ThemeMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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

// The AI Coach screen's header in each theme: the coach's avatar, its name, a status and the refresh.

/**
 * The header's request for a fresh insight. [remainingSeconds] is the wait before one is allowed
 * (the 6-hour limit); while it runs the button shows the hours left instead.
 */
data class CoachRefreshAction(
    val enabled: Boolean,
    val remainingSeconds: Long,
    val onClick: () -> Unit
)

/** "↻ NEW", or "⏳ 3H" while recharging: what was a full-width REFRESH / COOLDOWN button (C13). */
@Composable
fun CoachRefreshButton(action: CoachRefreshAction, themeMode: ThemeMode, modifier: Modifier = Modifier) {
    val hours = (action.remainingSeconds + 3599) / 3600
    val label = if (action.enabled || hours <= 0L) {
        stringResource(R.string.coach_refresh)
    } else {
        stringResource(R.string.coach_cooldown_hours, hours)
    }
    val icon = if (action.enabled || hours <= 0L) "↻" else "⏳"
    val text = "$icon " + if (themeMode == ThemeMode.Light) label else label.uppercase()
    val description = if (action.enabled) {
        stringResource(R.string.coach_refresh_description)
    } else {
        stringResource(R.string.coach_cooldown_description, hours)
    }
    val shape = RoundedCornerShape(if (themeMode == ThemeMode.Pixel || themeMode == ThemeMode.System) 0.dp else 6.dp)
    val (fill, ink, edge) = when (themeMode) {
        ThemeMode.Comic -> Triple(if (action.enabled) ComicTokens.GoldAccent else ComicTokens.PanelSurface, ComicTokens.SolidBlack, ComicTokens.SolidBlack)
        ThemeMode.Light -> Triple(if (action.enabled) Color(0xFFEEF2FF) else Color(0xFFF1F5F9), if (action.enabled) Color(0xFF4F46E5) else Color(0xFF64748B), Color(0xFFC7D2FE))
        else -> Triple(Color.Transparent, if (action.enabled) Color(0xFFFBBF24) else Color(0xFF94A3B8), if (action.enabled) Color(0xFFFBBF24) else Color(0xFF475569))
    }
    Box(
        modifier = modifier
            .sizeIn(minWidth = CoachDimens.MinTouchTarget, minHeight = CoachDimens.MinTouchTarget)
            .clickable(enabled = action.enabled, role = Role.Button, onClickLabel = description, onClick = action.onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = when (themeMode) {
                ThemeMode.Comic -> MaterialTheme.typography.labelLarge.copy(fontFamily = BangersFontFamily)
                ThemeMode.Light -> MaterialTheme.typography.labelLarge
                else -> PixelTypography.labelSmall
            },
            color = ink,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .clip(shape)
                .background(fill, shape)
                .border(if (themeMode == ThemeMode.Comic) 2.dp else 1.dp, edge, shape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** Pixel: the Questmaster, or the Habit Coach in Simple Mode. */
@Composable
fun PixelCoachHeader(isSimpleMode: Boolean, refresh: CoachRefreshAction? = null) {
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
                    .size(CoachDimens.Header.Avatar)
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
            if (refresh != null) {
                Spacer(modifier = Modifier.width(CoachDimens.Header.ChipGap))
                CoachRefreshButton(refresh, ThemeMode.Pixel)
            }
        }
    }
}

/** Comic: the oracle's panel with its issue chip. */
@Composable
fun ComicCoachHeader(refresh: CoachRefreshAction? = null) {
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
                    .size(CoachDimens.Header.Avatar)
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
            if (refresh != null) {
                Spacer(modifier = Modifier.width(CoachDimens.Header.ChipGap))
                CoachRefreshButton(refresh, ThemeMode.Comic)
            }
        }
    }
}

/** Light: a clean card with the coach's sprout. */
@Composable
fun LightCoachHeader(refresh: CoachRefreshAction? = null) {
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
                    .size(CoachDimens.Header.Avatar)
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
            if (refresh != null) {
                Spacer(modifier = Modifier.width(CoachDimens.Header.ChipGap))
                CoachRefreshButton(refresh, ThemeMode.Light)
            }
        }
    }
}
