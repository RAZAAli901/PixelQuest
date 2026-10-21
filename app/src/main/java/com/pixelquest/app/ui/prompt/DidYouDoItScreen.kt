package com.pixelquest.app.ui.prompt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import kotlinx.coroutines.delay

/**
 * The full-screen "did you do it?" at a quest's time: the quest, and Yes or Not yet. In the game
 * (the copy has its quest icon) a quest tile heads it; Simple Mode asks plainly. It closes itself
 * after [timeoutMillis].
 */
@Composable
fun DidYouDoItScreen(
    taskId: Long,
    taskName: String,
    onDismiss: () -> Unit,
    onYesClick: () -> Unit = {},
    onNoClick: () -> Unit = {},
    isSimpleMode: Boolean = false,
    timeoutMillis: Long = 2 * 60 * 60 * 1000L, // 2 hours window
    modifier: Modifier = Modifier
) {
    val copy = TaskPromptCopyVariants.resolve(isSimpleMode)
    val soundManager = LocalSoundManager.current
    LaunchedEffect(taskId) {
        delay(timeoutMillis)
        onDismiss()
    }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .padding(PqDimens.ScreenPad),
        contentAlignment = Alignment.Center
    ) {
        PqCard(Modifier.fillMaxWidth(), shadow = PqDimens.ShadowLg, radius = 18.dp) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                // The game's copy carries its quest icon; v2 draws it as a tile, not the emoji.
                if (copy.iconEmoji != null) CategoryTile(Icons.Rounded.Bolt, PqColors.Gold, size = 56.dp, iconSize = 32.dp, radius = 14.dp)
                Text(text = copy.headerTitle, style = PqType.Title, textAlign = TextAlign.Center)
                Text(text = copy.questionPrompt, style = PqType.Meta, textAlign = TextAlign.Center)
                Text(text = taskName, style = PqType.CardTitle, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    PqButton(
                        text = copy.dismissButtonText,
                        onClick = {
                            com.pixelquest.app.ui.haptics.PixelHaptics.performWarning(haptic)
                            soundManager?.playTaskMissedSound()
                            onNoClick()
                            onDismiss()
                        },
                        style = PqButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    PqButton(
                        text = copy.confirmButtonText,
                        onClick = {
                            com.pixelquest.app.ui.haptics.PixelHaptics.performSuccessPattern(haptic)
                            soundManager?.playTaskCompleteSound()
                            onYesClick()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(name = "Did You Do It Prompt - Comic Mode", showBackground = true)
@Composable
fun DidYouDoItScreenComicPreview() {
    com.pixelquest.app.ui.theme.PixelQuestTheme(themeMode = com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        DidYouDoItScreen(
            taskId = 1,
            taskName = "Defeat the Morning Workout",
            onDismiss = {},
            isSimpleMode = false
        )
    }
}

