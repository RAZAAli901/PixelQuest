package com.pixelquest.app.ui.screens.difficulty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DifficultyUiState(
    val currentLevel: DifficultyLevel = DifficultyLevel.MEDIUM,
    val pendingLevel: DifficultyLevel? = null,
    val showWarningDialog: Boolean = false
)

@HiltViewModel
class DifficultyViewModel @Inject constructor(
    private val difficultySettingsRepository: DifficultySettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DifficultyUiState())
    val uiState: StateFlow<DifficultyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            difficultySettingsRepository.getCurrentDifficulty().collectLatest { settings ->
                if (settings != null) {
                    _uiState.value = _uiState.value.copy(currentLevel = settings.difficultyLevel)
                }
            }
        }
    }

    fun onDifficultyClicked(level: DifficultyLevel) {
        if (level == _uiState.value.currentLevel) return
        _uiState.value = _uiState.value.copy(pendingLevel = level, showWarningDialog = true)
    }

    fun dismissWarningDialog() {
        _uiState.value = _uiState.value.copy(pendingLevel = null, showWarningDialog = false)
    }

    fun confirmDifficultyChange() {
        val target = _uiState.value.pendingLevel ?: return
        viewModelScope.launch {
            val newSettings = DifficultySettingsEntity(
                id = 1,
                difficultyLevel = target,
                perfectDayThreshold = DifficultyMode.getPerfectDayThreshold(target),
                daysRequiredPerLevel = DifficultyMode.getDaysRequiredPerLevel(target)
            )
            difficultySettingsRepository.insertSettings(newSettings)
            _uiState.value = _uiState.value.copy(
                currentLevel = target,
                pendingLevel = null,
                showWarningDialog = false
            )
        }
    }
}
