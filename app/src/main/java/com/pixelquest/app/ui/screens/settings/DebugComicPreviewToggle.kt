package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.BuildConfig
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 37: Debug-only Comic mode preview toggle clearly gated behind [BuildConfig.DEBUG].
 * In release builds or non-debug environments, this composable emits nothing, ensuring
 * Comic mode remains 100% gated from real users per Day 20's architectural decision.
 */
@Composable
fun DebugComicPreviewToggle(
    currentTheme: ThemeMode,
    onTogglePreview: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!BuildConfig.DEBUG) {
        return
    }

    val isComicActive = currentTheme == ThemeMode.Comic

    PixelCard(
        modifier = modifier.fillMaxWidth(),
        variant = PixelPanelVariant.BEIGE,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🛠 DEBUG: PREVIEW COMIC MODE",
                    style = PixelTypography.labelLarge,
                    color = PixelTheme.colors.primary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Temporary QA toggle to inspect buttons, cards, dialogs & forms",
                    style = PixelTypography.bodySmall,
                    color = PixelTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = isComicActive,
                onCheckedChange = { isChecked ->
                    onTogglePreview(isChecked)
                }
            )
        }
    }
}
