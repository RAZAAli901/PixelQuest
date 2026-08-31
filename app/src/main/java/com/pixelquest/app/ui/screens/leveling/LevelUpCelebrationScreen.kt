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
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelQuestTypography
import com.pixelquest.app.ui.theme.PixelTextWhite

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import com.pixelquest.app.ui.components.PixelButton
import androidx.compose.ui.tooling.preview.Preview
import com.pixelquest.app.ui.theme.PixelQuestTheme

import androidx.compose.runtime.LaunchedEffect

import com.pixelquest.app.audio.LocalSoundManager

@Composable
fun LevelUpCelebrationScreen(
    level: Int,
    onDismiss: () -> Unit = {}
) {
    val soundManager = LocalSoundManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LaunchedEffect(level) {
        soundManager?.playLevelUpSound()
        com.pixelquest.app.ui.haptics.PixelHaptics.performSuccessPattern(haptic)
    }

    val transition = rememberInfiniteTransition(label = "level_up_bounce")
    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    var visible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = com.pixelquest.app.ui.navigation.PixelTransitions.LevelUpEnter,
            exit = com.pixelquest.app.ui.navigation.PixelTransitions.LevelUpExit
        ) {
            PixelCard(
                variant = PixelPanelVariant.YELLOW,
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
                    style = PixelQuestTypography.headlineMedium,
                    color = PixelGold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "YOU ARE NOW LEVEL",
                    style = PixelQuestTypography.bodyLarge,
                    color = PixelTextWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$level",
                    style = PixelQuestTypography.displayLarge.copy(fontSize = 48.sp),
                    color = PixelGold
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
