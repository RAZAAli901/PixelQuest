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
 * Step 9: Compose Preview comparing daily progress ring rendering across all three themes:
 * Pixel (Dark Retro), Light (Daylight), and Comic (Paper/Ink Vector with Starburst).
 */
@Composable
private fun ProgressRingShowcaseColumn(title: String) {
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

        // 1. In-Progress State (50% of target)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "IN PROGRESS (50% / 100%)",
                style = PixelTypography.bodySmall,
                color = PixelTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            PixelDailyProgressRing(
                progress = 0.5f,
                targetThreshold = 1.0f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. Goal Met / Perfect Day State (100% of target)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PERFECT DAY (100% / 100%)",
                style = PixelTypography.bodySmall,
                color = PixelTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            PixelDailyProgressRing(
                progress = 1.0f,
                targetThreshold = 1.0f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Progress Ring 3-Theme Comparison Preview", widthDp = 1080, heightDp = 520)
@Composable
fun ThemeProgressRingComparisonPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Pixel Mode (Dark Retro 8-Bit)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                ProgressRingShowcaseColumn(title = "PIXEL (DARK RETRO)")
            }
        }

        // 2. Light Mode (Daylight)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                ProgressRingShowcaseColumn(title = "LIGHT (DAYLIGHT)")
            }
        }

        // 3. Comic Mode (Ink / Vector Starburst)
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                ProgressRingShowcaseColumn(title = "COMIC (INK/VECTOR)")
            }
        }
    }
}
