package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * Step 4: Compose Preview comparing progress bar rendering across all three themes:
 * Pixel (Dark Retro 8-bit), Light (Daylight Emerald), and Comic (Paper/Ink Vector).
 */
@Composable
private fun ProgressBarShowcaseColumn(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        val states = listOf(
            "0% Empty" to 0.0f,
            "25% Quarter" to 0.25f,
            "50% Halfway" to 0.5f,
            "75% Three-Quarters" to 0.75f,
            "100% Complete" to 1.0f
        )

        states.forEach { (label, progress) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = PixelTypography.bodySmall,
                    color = PixelTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                PixelProgressBar(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(name = "Progress Bar 3-Theme Comparison Preview", widthDp = 1080, heightDp = 520)
@Composable
fun ThemeProgressBarComparisonPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Pixel Mode (Dark Retro 8-Bit)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                ProgressBarShowcaseColumn(title = "PIXEL (DARK RETRO)")
            }
        }

        // 2. Light Mode (Daylight)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                ProgressBarShowcaseColumn(title = "LIGHT (DAYLIGHT)")
            }
        }

        // 3. Comic Mode (Ink / Vector)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                ProgressBarShowcaseColumn(title = "COMIC (INK/VECTOR)")
            }
        }
    }
}
