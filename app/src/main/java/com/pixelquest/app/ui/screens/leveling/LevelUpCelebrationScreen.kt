package com.pixelquest.app.ui.screens.leveling

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.pixelquest.app.ui.components.PixelButton
import androidx.compose.ui.tooling.preview.Preview
import com.pixelquest.app.ui.theme.PixelQuestTheme

@Composable
fun LevelUpCelebrationScreen(
    level: Int,
    onDismiss: () -> Unit
) {
    val colors = PixelTheme.colors
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        )
    )

    val soundManager = com.pixelquest.app.audio.LocalSoundManager.current
    LaunchedEffect(Unit) {
        soundManager?.playLevelUpSound()
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = com.pixelquest.app.ui.navigation.PixelTransitions.LevelUpEnter,
            exit = com.pixelquest.app.ui.navigation.PixelTransitions.LevelUpExit
        ) {
            PixelCard(
                variant = PixelPanelVariant.BORDER,
                modifier = Modifier
                    .padding(24.dp)
                    .scale(scale)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🎉 LEVEL UP! 🎉",
                        style = PixelTypography.headlineMedium,
                        color = colors.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "YOU ARE NOW LEVEL",
                        style = PixelTypography.bodyLarge,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$level",
                        style = PixelTypography.displayLarge.copy(fontSize = 48.sp),
                        color = colors.gold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    PixelButton(
                        text = "CONTINUE",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LevelUpCelebrationScreenPreview() {
    PixelQuestTheme {
        LevelUpCelebrationScreen(
            level = 5,
            onDismiss = {}
        )
    }
}
