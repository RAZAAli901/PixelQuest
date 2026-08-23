package com.pixelquest.app.ui.prompt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography
import kotlinx.coroutines.delay

import com.pixelquest.app.audio.LocalSoundManager

@Composable
fun DidYouDoItScreen(
    taskId: Long,
    taskName: String,
    onDismiss: () -> Unit,
    onYesClick: () -> Unit = {},
    onNoClick: () -> Unit = {},
    timeoutMillis: Long = 2 * 60 * 60 * 1000L, // 2 hours window
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current
    LaunchedEffect(taskId) {
        delay(timeoutMillis)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PixelBackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        PixelCard(
            variant = PixelPanelVariant.BEIGE,
            contentPadding = 24.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚔️",
                    style = PixelTypography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DID YOU DO IT?",
                    style = PixelTypography.displaySmall,
                    color = PixelGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = taskName,
                    style = PixelTypography.titleMedium,
                    color = PixelTextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PixelButton(
                        text = "NOT YET",
                        onClick = {
                            onNoClick()
                            onDismiss()
                        },
                        variant = PixelButtonVariant.BLUE,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    PixelButton(
                        text = "YES!",
                        onClick = {
                            soundManager?.playTaskCompleteSound()
                            onYesClick()
                            onDismiss()
                        },
                        variant = PixelButtonVariant.YELLOW,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
