package com.pixelquest.app.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.ui.components.PixelAvatarDisplay
import com.pixelquest.app.ui.theme.PixelTheme

@Composable
fun PixelLeaderboardRow(
    rank: Int,
    displayName: String,
    statLabel: String,
    statValue: String,
    modifier: Modifier = Modifier,
    isCurrentUser: Boolean = false,
    avatarId: String? = null,
    onReportClicked: ((displayName: String) -> Unit)? = null
) {
    val colors = PixelTheme.colors
    val activeMode = PixelTheme.mode
    val isLight = activeMode == com.pixelquest.app.ui.theme.ThemeMode.Light
    val isComic = activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic

    // Rank tier visual styling reusing Day 7's AvatarTier system
    val tier = when (rank) {
        1 -> AvatarTier.GOLD
        2 -> AvatarTier.SILVER
        3 -> AvatarTier.BRONZE
        else -> null
    }

    val tierColor = if (tier != null) {
        if (isComic) {
            when (tier) {
                AvatarTier.GOLD -> Color(0xFFB45309) // High-contrast amber-gold for text
                AvatarTier.SILVER -> Color(0xFF334155) // High-contrast slate for silver text
                AvatarTier.BRONZE -> Color(0xFF9A3412) // High-contrast dark bronze for text
            }
        } else if (isLight) {
            when (tier) {
                AvatarTier.GOLD -> colors.gold
                AvatarTier.SILVER -> Color(0xFF475569)
                AvatarTier.BRONZE -> Color(0xFF9A4F10)
            }
        } else {
            Color(tier.borderColor)
        }
    } else colors.pixelBorder

    val rankTextColor = if (tier != null) tierColor else colors.onSurfaceVariant

    // Subtle highlight pulse animation when current user's row is visible
    val pulseAlpha = if (isCurrentUser && !com.pixelquest.app.ui.theme.LocalReduceMotion.current) {
        val transition = rememberInfiniteTransition(label = "currentUserPulse")
        val alphaState = transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        alphaState.value
    } else {
        1.0f
    }

    val borderColor = if (isComic) {
        if (isCurrentUser) com.pixelquest.app.ui.theme.ComicTokens.CoralRed else com.pixelquest.app.ui.theme.ComicTokens.SolidBlack
    } else if (isCurrentUser) {
        colors.primary.copy(alpha = pulseAlpha)
    } else {
        tierColor
    }

    val backgroundColor = when {
        isCurrentUser -> if (isComic) Color(0xFFFFFBEB) else if (isLight) Color(0xFFFEF3C7) else Color(0xFF262640)
        rank == 1 -> if (isComic) Color(0xFFFEF3C7) else if (isLight) Color(0xFFFEF9C3) else Color(0xFF2A2416)
        rank == 2 -> if (isComic) Color(0xFFF0F9FF) else if (isLight) Color(0xFFF1F5F9) else Color(0xFF22242B)
        rank == 3 -> if (isComic) Color(0xFFFFEDD5) else if (isLight) Color(0xFFFFEDD5) else Color(0xFF271F1B)
        else -> colors.surface
    }
    val shape = if (isComic) RoundedCornerShape(com.pixelquest.app.ui.theme.ComicShapeTokens.RadiusDefault) else RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (isComic) 2.dp else (if (isCurrentUser || rank <= 3) 2.dp else 1.dp),
                color = borderColor,
                shape = shape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left section: Rank + Avatar + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Rank Box
                val rankBoxBg = if (isComic) {
                    when (tier) {
                        AvatarTier.GOLD -> com.pixelquest.app.ui.theme.ComicTokens.GoldAccent.copy(alpha = 0.35f)
                        AvatarTier.SILVER -> com.pixelquest.app.ui.theme.ComicTokens.SkyBlue.copy(alpha = 0.35f)
                        AvatarTier.BRONZE -> com.pixelquest.app.ui.theme.ComicTokens.BurntOrange.copy(alpha = 0.35f)
                        null -> Color.Transparent
                    }
                } else (if (rank <= 3) tierColor.copy(alpha = 0.2f) else Color.Transparent)

                val rankBoxBorder = if (isComic) {
                    com.pixelquest.app.ui.theme.ComicTokens.SolidBlack
                } else {
                    if (rank <= 3) tierColor else colors.pixelBorder
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(rankBoxBg)
                        .border(1.dp, rankBoxBorder, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (tier) {
                            AvatarTier.GOLD -> "${AvatarTier.GOLD.badgeEmoji}1"
                            AvatarTier.SILVER -> "${AvatarTier.SILVER.badgeEmoji}2"
                            AvatarTier.BRONZE -> "${AvatarTier.BRONZE.badgeEmoji}3"
                            null -> "#$rank"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = rankTextColor,
                        fontSize = if (tier != null) 9.sp else 8.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar Icon with tier border for top 3
                val avatarBorder = if (isComic) com.pixelquest.app.ui.theme.ComicTokens.SolidBlack else tierColor
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .then(
                            if (tier != null) Modifier.border(1.5.dp, avatarBorder, RoundedCornerShape(4.dp))
                            else Modifier
                        )
                ) {
                    PixelAvatarDisplay(
                        avatarId = avatarId ?: "avatar_hero",
                        size = 32.dp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Display Name + (YOU) badge
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.primary)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "YOU",
                                    fontSize = 7.sp,
                                    color = colors.onPrimary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        } else if (onReportClicked != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🚩",
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .clickable { onReportClicked(displayName) }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right section: Stat Value + Label
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = statValue,
                    style = MaterialTheme.typography.titleMedium,
                    color = com.pixelquest.app.ui.theme.inkOnPanel(if (rank <= 3) tierColor else colors.secondary),
                    fontSize = 12.sp
                )
                Text(
                    text = statLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    fontSize = 7.sp
                )
            }
        }
    }
}
