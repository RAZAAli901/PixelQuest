package com.pixelquest.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.Pixel
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    /**
     * One-tap Comic switch for the Home screen: turns Comic on, or switches back to the theme
     * that was active before Comic was chosen.
     */
    fun toggleComicMode() {
        viewModelScope.launch {
            val current = settingsRepository.themeMode.first()
            if (current == ThemeMode.Comic) {
                settingsRepository.setThemeMode(settingsRepository.getThemeModeBeforeComic())
            } else {
                settingsRepository.setComicModeHighlightSeen(true)
                settingsRepository.setThemeMode(ThemeMode.Comic)
            }
        }
    }
}
