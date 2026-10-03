package com.pixelquest.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.theme.ThemeMode

// Previews live in *Preview.kt files, which the hard-coded colour audit skips.
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF12121E)
@Composable
fun ThemeSelectionCardPreview() {
    com.pixelquest.app.ui.theme.PixelQuestTheme {
        ThemeSelectionCard(
            currentTheme = ThemeMode.Pixel,
            onThemeSelected = {}
        )
    }
}
