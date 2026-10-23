package com.pixelquest.app.zz

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.Modifier
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PqDangerButton
import com.pixelquest.app.ui.components.PqSegmented
import com.pixelquest.app.ui.screens.settings.SettingLink
import com.pixelquest.app.ui.screens.settings.SettingNote
import com.pixelquest.app.ui.screens.settings.SettingSwitch
import com.pixelquest.app.ui.screens.settings.SettingsScreenScaffold
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class SettingsShotTest : ScratchShot() {
    private fun content() = @androidx.compose.runtime.Composable {
        PixelQuestTheme {
            SettingsScreenScaffold(
                onNavigateBack = {},
                accountSection = {
                    PixelTextField("Raza", {}, label = "Hero name", modifier = Modifier.fillMaxWidth())
                    SettingLink(Icons.Rounded.Tune, "Difficulty", {}, value = "Medium")
                    SettingLink(Icons.Rounded.Cloud, "Cloud & leaderboard", {})
                },
                notificationsSection = {
                    SettingSwitch("Notifications", "Reminders and nudges from PixelQuest", true, {})
                    SettingSwitch("Reminder sound", "Off, reminders arrive silently", false, {})
                },
                appearanceSection = {
                    PqSegmented(listOf("Comic", "Pixel"), 0, {})
                    SettingSwitch("Sound effects", null, true, {})
                },
                dangerZoneSection = {
                    SettingNote("Wipes every quest, your XP, level and streaks on this phone. It asks twice first.")
                    PqDangerButton("Reset all progress", {})
                }
            )
        }
    }

    @Test fun comic() = shot("settings", PqSkin.Comic, content())
    @Test fun pixel() = shot("settings", PqSkin.Pixel, content())
}
