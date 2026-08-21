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

@Composable
fun LevelUpCelebrationScreen(
    level: Int,
    onDismiss: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        PixelCard(
            variant = PixelPanelVariant.YELLOW,
            modifier = Modifier.padding(24.dp)
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
            }
        }
    }
}
