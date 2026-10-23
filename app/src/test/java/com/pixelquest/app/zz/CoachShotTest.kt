package com.pixelquest.app.zz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.CoachCard
import com.pixelquest.app.ui.components.CoachUi
import com.pixelquest.app.ui.screens.today.AiTeaser
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class CoachShotTest : ScratchShot() {
    private fun content() = @androidx.compose.runtime.Composable {
        PixelQuestTheme {
            Column(Modifier.fillMaxSize().background(PqColors.Ground).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CoachCard(CoachUi(true, "You finished 4 of 5 quests three days running, and the gym ones slip on Fridays. Keep the morning ones first.", "8:48 AM", "~6h"), {}, {})
                AiTeaser(isSimpleMode = false, needsSignIn = true, onUnlock = {})
                AiTeaser(isSimpleMode = true, needsSignIn = false, onUnlock = {})
            }
        }
    }

    @Test fun comic() = shot("coach", PqSkin.Comic, content())
    @Test fun pixel() = shot("coach", PqSkin.Pixel, content())
}
