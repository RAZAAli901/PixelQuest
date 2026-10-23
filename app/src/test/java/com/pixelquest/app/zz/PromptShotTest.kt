package com.pixelquest.app.zz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.notifications.NotificationsOffBanner
import com.pixelquest.app.ui.prompt.DidYouDoItScreen
import com.pixelquest.app.ui.screens.settings.ExactAlarmPrompt
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.tips.FirstRunTip
import com.pixelquest.app.ui.tips.LocalTipsStore
import com.pixelquest.app.ui.tips.MemoryTipsStore
import com.pixelquest.app.ui.tips.Tip
import org.junit.Test

class PromptShotTest : ScratchShot() {
    @Test fun prompt() = shot("did-you", PqSkin.Comic) { PixelQuestTheme { DidYouDoItScreen(1, "Morning run", {}) } }
    @Test fun promptPixel() = shot("did-you", PqSkin.Pixel) { PixelQuestTheme { DidYouDoItScreen(1, "Morning run", {}, isSimpleMode = true) } }
    @Test fun bits() = shot("banner-tip", PqSkin.Comic) {
        PixelQuestTheme {
            CompositionLocalProvider(LocalTipsStore provides MemoryTipsStore()) {
                Column(Modifier.fillMaxSize().background(PqColors.Ground).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NotificationsOffBanner(onAllow = {})
                    FirstRunTip(Tip.QURAN)
                    ExactAlarmPrompt(canScheduleExact = { false }, onAllow = {})
                }
            }
        }
    }
}
