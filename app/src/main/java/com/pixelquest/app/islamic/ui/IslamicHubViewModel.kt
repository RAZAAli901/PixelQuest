package com.pixelquest.app.islamic.ui

import com.pixelquest.app.ui.navigation.TabSharing
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.islamic.IslamicRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * What the Islamic hub shows.
 *
 * @param items the registered items that are on, in the hub's order.
 * @param loaded false until the registry first answers, so the hub doesn't flash empty.
 */
data class IslamicHubUiState(
    val items: List<IslamicItem> = emptyList(),
    val isSimpleMode: Boolean = false,
    val loaded: Boolean = false
)

/** The hub's cards: every item in the registry that's on, each with its current badge. */
@HiltViewModel
class IslamicHubViewModel @Inject constructor(
    registry: IslamicRegistry,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val state: StateFlow<IslamicHubUiState> = combine(registry.items, settingsRepository.simpleModeEnabled) { items, simple ->
        IslamicHubUiState(items = items.filter { it.enabled }, isSimpleMode = simple, loaded = true)
    }.stateIn(viewModelScope, TabSharing, IslamicHubUiState())
}
