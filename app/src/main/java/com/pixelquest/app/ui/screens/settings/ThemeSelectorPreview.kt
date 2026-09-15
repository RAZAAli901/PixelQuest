package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 21: Compose Preview for the Theme Selector.
 */
@Preview(name = "Theme Selector - Pixel Mode Active", showBackground = true)
@Composable
fun ThemeSelectorPixelActivePreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        Box(
            modifier = Modifier
                .background(PixelBackgroundDark)
                .padding(16.dp)
        ) {
            ThemeSelectionCard(
                currentTheme = ThemeMode.Pixel,
                onThemeSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Theme Selector - Light Mode Active", showBackground = true)
@Composable
fun ThemeSelectorLightActivePreview() {
    PixelQuestTheme(themeMode = ThemeMode.Light) {
        Box(
            modifier = Modifier
                .background(PixelBackgroundDark)
                .padding(16.dp)
        ) {
            ThemeSelectionCard(
                currentTheme = ThemeMode.Light,
                onThemeSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Theme Selector - Follow System Active", showBackground = true)
@Composable
fun ThemeSelectorSystemActivePreview() {
    PixelQuestTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier
                .background(PixelBackgroundDark)
                .padding(16.dp)
        ) {
            ThemeSelectionCard(
                currentTheme = ThemeMode.System,
                onThemeSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

