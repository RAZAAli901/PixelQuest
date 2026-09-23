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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 20: Compose Preview comparing dialog shell rendering across all three themes:
 * Pixel (Retro 9-patch), Light (Daylight amber), and Comic (Paper/Ink with Bangers & flat shadow).
 */
@Composable
private fun DialogShowcaseColumn(themeTitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = themeTitle,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        // Render the dialog content panel inside a PixelCard (which dispatches based on active theme)
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            variant = PixelPanelVariant.BORDER,
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (PixelTheme.mode == ThemeMode.Comic) {
                    Text(
                        text = "ABANDON QUEST?",
                        fontFamily = BangersFontFamily,
                        fontSize = 22.sp,
                        letterSpacing = 0.8.sp,
                        color = ComicTokens.SolidBlack
                    )
                } else {
                    Text(
                        text = "ABANDON QUEST?",
                        style = PixelTypography.titleLarge,
                        color = PixelTheme.colors.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Are you sure you want to abandon this quest? All progress will be lost.",
                    style = PixelTypography.bodyMedium,
                    color = if (PixelTheme.mode == ThemeMode.Comic) ComicTokens.SolidBlack else PixelTheme.colors.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PixelButton(
                        text = "CANCEL",
                        onClick = {},
                        variant = PixelButtonVariant.BLUE
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    PixelButton(
                        text = "DELETE",
                        onClick = {},
                        variant = PixelButtonVariant.YELLOW
                    )
                }
            }
        }
    }
}

@Preview(name = "Dialog 3-Theme Comparison Preview", widthDp = 1080, heightDp = 500)
@Composable
fun ThemeDialogComparisonPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                DialogShowcaseColumn(themeTitle = "PIXEL (DARK)")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                DialogShowcaseColumn(themeTitle = "LIGHT (DAYLIGHT)")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                DialogShowcaseColumn(themeTitle = "COMIC (INK/VECTOR)")
            }
        }
    }
}
