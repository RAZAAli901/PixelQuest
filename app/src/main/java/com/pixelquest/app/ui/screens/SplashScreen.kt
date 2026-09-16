package com.pixelquest.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelAvatarFrame
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelProgressBar

@Composable
fun SplashScreen(
    onSplashTimeout: () -> Unit = {}
) {
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500)
        )
        onSplashTimeout()
    }

    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelCard(
            modifier = Modifier.fillMaxWidth(0.92f),
            variant = PixelPanelVariant.BORDER,
            contentPadding = 24.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PIXELQUEST",
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                PixelAvatarFrame(
                    avatarId = "avatar_hero",
                    level = 10,
                    size = 80.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "8-BIT HABIT TRACKER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.secondary
                )
                Spacer(modifier = Modifier.height(36.dp))
                Text(
                    text = "INITIALIZING...",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                PixelProgressBar(
                    progress = progressAnim.value,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Splash Screen - Dark", showBackground = true)
@Composable
private fun SplashScreenDarkPreview() {
    com.pixelquest.app.ui.theme.PixelQuestTheme(themeMode = com.pixelquest.app.ui.theme.ThemeMode.Pixel) {
        SplashScreen()
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Splash Screen - Light", showBackground = true)
@Composable
private fun SplashScreenLightPreview() {
    com.pixelquest.app.ui.theme.PixelQuestTheme(themeMode = com.pixelquest.app.ui.theme.ThemeMode.Light) {
        SplashScreen()
    }
}
