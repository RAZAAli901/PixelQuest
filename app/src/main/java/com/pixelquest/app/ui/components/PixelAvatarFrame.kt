package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.AvatarTierCalculator
import com.pixelquest.app.ui.theme.PixelSurfaceDark

@Composable
fun PixelAvatarFrame(
    avatarId: String,
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val tier = AvatarTierCalculator.calculateTier(level)
    val borderColor = Color(tier.borderColor)
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(PixelSurfaceDark)
                .border(3.dp, borderColor, shape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            PixelAvatarDisplay(
                avatarId = avatarId,
                size = size
            )
        }

        // Tier Badge Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(borderColor)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = tier.badgeEmoji,
                fontSize = 12.sp
            )
        }
    }
}
