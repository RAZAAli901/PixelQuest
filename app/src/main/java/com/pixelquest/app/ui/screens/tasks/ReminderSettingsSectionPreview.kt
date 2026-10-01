package com.pixelquest.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

@Composable
private fun ReminderSectionSample(mode: ThemeMode, enabled: Boolean = true) {
    PixelQuestTheme(themeMode = mode) {
        Box(modifier = Modifier.background(PixelTheme.colors.background).padding(16.dp)) {
            ReminderSettingsSection(
                reminderEnabled = enabled,
                leadMinutes = 15,
                style = ReminderStyle.PROMPT,
                onReminderEnabledChanged = {},
                onLeadSelected = {},
                onStyleSelected = {}
            )
        }
    }
}

@Preview(name = "Reminder section - Pixel", widthDp = 360)
@Composable
fun ReminderSettingsSectionPixelPreview() = ReminderSectionSample(ThemeMode.Pixel)

@Preview(name = "Reminder section - Light", widthDp = 360)
@Composable
fun ReminderSettingsSectionLightPreview() = ReminderSectionSample(ThemeMode.Light)

@Preview(name = "Reminder section - Comic", widthDp = 360)
@Composable
fun ReminderSettingsSectionComicPreview() = ReminderSectionSample(ThemeMode.Comic)

@Preview(name = "Reminder section - Off", widthDp = 360)
@Composable
fun ReminderSettingsSectionOffPreview() = ReminderSectionSample(ThemeMode.Pixel, enabled = false)
