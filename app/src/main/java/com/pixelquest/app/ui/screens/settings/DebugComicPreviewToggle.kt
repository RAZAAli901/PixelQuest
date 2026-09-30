package com.pixelquest.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 28 (Day 23): Retired debug preview toggle.
 * Comic mode is now fully unlocked for real users via ThemeSelectionCard in the normal Settings flow.
 * Retained as a deprecated no-op for debug workflow compatibility.
 */
@Deprecated(
    message = "Retired in Day 23: Comic mode is now unlocked for real users through the normal Settings flow",
    level = DeprecationLevel.WARNING
)
@Composable
fun DebugComicPreviewToggle(
    currentTheme: ThemeMode,
    onTogglePreview: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Retired in Day 23: Real users now access Comic mode directly via ThemeSelectionCard.
    // Emits nothing to eliminate debug artifacts from the UI across all environments.
}
