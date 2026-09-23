package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 9: Compose Preview comparing button rendering across all three themes side-by-side:
 * Pixel (Dark Retro), Light (Daylight), and Comic (Paper/Ink Vector).
 */
@Composable
private fun ButtonShowcaseColumn(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        PixelButton(
            text = "PRIMARY CTA",
            onClick = {},
            variant = PixelButtonVariant.YELLOW,
            modifier = Modifier.fillMaxWidth()
        )

        PixelButton(
            text = "SECONDARY ACTION",
            onClick = {},
            variant = PixelButtonVariant.BLUE,
            modifier = Modifier.fillMaxWidth()
        )

        PixelButton(
            text = "DISABLED STATE",
            onClick = {},
            variant = PixelButtonVariant.YELLOW,
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Button 3-Theme Comparison Preview", widthDp = 1080, heightDp = 480)
@Composable
fun ThemeButtonComparisonPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Pixel Mode (Dark Retro)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                ButtonShowcaseColumn(title = "PIXEL (DARK)")
            }
        }

        // 2. Light Mode (Daylight)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                ButtonShowcaseColumn(title = "LIGHT (DAYLIGHT)")
            }
        }

        // 3. Comic Mode (Ink / Vector)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                ButtonShowcaseColumn(title = "COMIC (INK/VECTOR)")
            }
        }
    }
}
