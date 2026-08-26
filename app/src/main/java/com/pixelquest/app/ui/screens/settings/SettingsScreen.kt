package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToDifficulty: () -> Unit = {},
    onNavigateToAvatar: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    SettingsScreenScaffold(
        appearanceSection = {
            val soundText = if (state.isSoundEnabled) "🔊 SFX: ON" else "🔇 SFX: OFF"
            PixelButton(
                text = soundText,
                onClick = { viewModel.toggleSound(!state.isSoundEnabled) },
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth()
            )
            val crtText = if (state.isCrtEnabled) "📺 CRT FILTER: ON" else "📺 CRT FILTER: OFF"
            PixelButton(
                text = crtText,
                onClick = { viewModel.toggleCrt(!state.isCrtEnabled) },
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
