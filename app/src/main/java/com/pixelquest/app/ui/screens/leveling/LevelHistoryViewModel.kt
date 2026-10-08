package com.pixelquest.app.ui.screens.leveling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.domain.repository.LevelHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LevelHistoryViewModel @Inject constructor(
    private val levelHistoryRepository: LevelHistoryRepository
) : ViewModel() {

    /** Null until the history has loaded, so the screen doesn't flash "no level-ups yet" first. */
    val history: StateFlow<List<LevelHistoryEntity>?> = levelHistoryRepository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
