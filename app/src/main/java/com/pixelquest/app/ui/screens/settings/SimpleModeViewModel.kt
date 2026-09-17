package com.pixelquest.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimpleModeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val simpleModeEnabled: StateFlow<Boolean> = settingsRepository.simpleModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setSimpleModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSimpleModeEnabled(enabled)
        }
    }
}
