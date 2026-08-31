package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelQuestTypography
import com.pixelquest.app.ui.theme.PixelSurface

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.pixelquest.app.ui.theme.PixelGold

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue

@Composable
fun PixelXpBar(
    currentProgress: Int,
    maxProgress: Int,
    modifier: Modifier = Modifier,
    level: Int = 1
) {
    val targetFraction = if (maxProgress > 0) (currentProgress.toFloat() / maxProgress).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "xp_bar_animation"
    )

    val percentage = (targetFraction * 100).toInt()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .androidx.compose.ui.semantics.semantics(mergeDescendants = true) {
                contentDescription = "Level $level XP progress: $currentProgress of $maxProgress days ($percentage percent)"
            }
    ) {
        // Level Badge
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PixelGold)
                .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "LVL $level",
                style = PixelQuestTypography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Progress Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PixelSurface)
                .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PixelGreen)
            )
            Text(
                text = "$currentProgress / $maxProgress DAYS",
                style = PixelQuestTypography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
