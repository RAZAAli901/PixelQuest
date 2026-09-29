package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode

@Preview(name = "Comic Snackbar - Standard Notice", showBackground = true)
@Composable
fun ComicSnackbarStandardPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.pixelquest.app.ui.theme.ComicTokens.PaperBackground)
                .padding(16.dp)
        ) {
            ComicSnackbar(
                message = "2 quest(s) missed today! Restart your streak now!"
            )
        }
    }
}

@Preview(name = "Comic Snackbar - With Action", showBackground = true)
@Composable
fun ComicSnackbarWithActionPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.pixelquest.app.ui.theme.ComicTokens.PaperBackground)
                .padding(16.dp)
        ) {
            ComicSnackbar(
                message = "Quest target reached!",
                actionLabel = "VIEW",
                onActionClick = {}
            )
        }
    }
}

@Preview(name = "Snackbar Comparison - Pixel vs Light vs Comic", showBackground = true)
@Composable
fun SnackbarMultiThemeComparisonPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pixel Theme Snackbar
        PixelQuestTheme(themeMode = ThemeMode.Pixel) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "PIXEL THEME:", color = com.pixelquest.app.ui.theme.PixelTheme.colors.primary)
                PixelSnackbar(message = "Pixel quest missed today!")
            }
        }

        // Light Theme Snackbar
        PixelQuestTheme(themeMode = ThemeMode.Light) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "LIGHT THEME:", color = com.pixelquest.app.ui.theme.PixelTheme.colors.primary)
                PixelSnackbar(message = "Light mode quest missed today!")
            }
        }

        // Comic Theme Snackbar
        PixelQuestTheme(themeMode = ThemeMode.Comic) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "COMIC THEME:", color = com.pixelquest.app.ui.theme.PixelTheme.colors.primary)
                PixelSnackbar(
                    message = "Comic quest missed today!",
                    actionLabel = "RETRY",
                    onActionClick = {}
                )
            }
        }
    }
}
