package com.pixelquest.app.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.PixelAvatarDisplay
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelSurfaceBorder
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.ui.components.PixelAvatarDisplay
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelSurfaceBorder
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTextWhite

@Composable
fun PixelLeaderboardRow(
    rank: Int,
    displayName: String,
    statLabel: String,
    statValue: String,
    modifier: Modifier = Modifier,
    isCurrentUser: Boolean = false,
    avatarId: String? = null
) {
    // Rank tier visual styling reusing Day 7's AvatarTier system
    val tier = when (rank) {
        1 -> AvatarTier.GOLD
        2 -> AvatarTier.SILVER
        3 -> AvatarTier.BRONZE
        else -> null
    }

    val tierColor = if (tier != null) Color(tier.borderColor) else PixelSurfaceBorder
    val rankTextColor = if (tier != null) Color(tier.borderColor) else PixelTextMuted

    val borderColor = if (isCurrentUser) PixelGold else tierColor
    val backgroundColor = when {
        isCurrentUser -> Color(0xFF262640)
        rank == 1 -> Color(0xFF2A2416)
        rank == 2 -> Color(0xFF22242B)
        rank == 3 -> Color(0xFF271F1B)
        else -> PixelSurfaceDark
    }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (isCurrentUser || rank <= 3) 2.dp else 1.dp,
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (rank <= 3) tierColor.copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (rank <= 3) tierColor else PixelSurfaceBorder, RoundedCornerShape(4.dp)),
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .then(
                            if (tier != null) Modifier.border(1.5.dp, tierColor, RoundedCornerShape(4.dp))
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
                            color = PixelTextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(PixelGold)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "YOU",
                                    fontSize = 7.sp,
                                    color = Color.Black,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
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
                    color = if (rank <= 3) tierColor else PixelCyan,
                    fontSize = 12.sp
                )
                Text(
                    text = statLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelTextMuted,
                    fontSize = 7.sp
                )
            }
        }
    }
}
