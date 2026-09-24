package com.pixelquest.app.ui.screens.leveling

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 34: Compose Preview for the comic level-up celebration.
 */
@Preview(name = "Comic Level Up Celebration - Level 5", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
fun ComicLevelUpCelebrationPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        Box(modifier = Modifier.fillMaxSize()) {
            ComicLevelUpCelebration(
                level = 5,
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "Comic Level Up Celebration - Milestone Level 10", showBackground = true, widthDp = 380, heightDp = 640)
@Composable
fun ComicLevelUpMilestonePreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        Box(modifier = Modifier.fillMaxSize()) {
            ComicLevelUpCelebration(
                level = 10,
                onDismiss = {}
            )
        }
    }
}
