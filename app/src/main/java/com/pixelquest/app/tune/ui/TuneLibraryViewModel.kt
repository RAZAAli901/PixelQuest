package com.pixelquest.app.tune.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.tune.data.TuneRepository
import com.pixelquest.app.tune.model.TuneItem
import com.pixelquest.app.tune.model.TuneTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TuneUiItem(
    val tune: TuneItem,
    val isDownloaded: Boolean,
    val isDownloading: Boolean,
    val isSelected: Boolean,
    val isPlayingPreview: Boolean
)

data class TuneLibraryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tunes: List<TuneUiItem> = emptyList(),
    val totalCacheSizeBytes: Long = 0L,
    val isDefaultSelected: Boolean = false
)

@HiltViewModel
class TuneLibraryViewModel @Inject constructor(
    private val tuneRepository: TuneRepository
) : ViewModel() {

    private val _rawTunes = MutableStateFlow<List<TuneItem>>(emptyList())
    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _previewingId = MutableStateFlow<String?>(null)
    private val _libraryType = MutableStateFlow(TuneTarget.QUEST)

    val selectedTuneId: StateFlow<String?> = combine(
        _libraryType,
        tuneRepository.questTuneIdFlow,
        tuneRepository.namazTuneIdFlow
    ) { type, questId, namazId ->
        when (type) {
            TuneTarget.QUEST -> questId
            TuneTarget.NAMAZ -> namazId
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val baseUiInfo = combine(
        _rawTunes,
        _downloadingIds,
        selectedTuneId,
        _previewingId
    ) { tunes, downloading, selectedId, previewId ->
        Tuple4(tunes, downloading, selectedId, previewId)
    }

    val uiState: StateFlow<TuneLibraryUiState> = combine(
        baseUiInfo,
        _isLoading,
        _errorMessage,
        _libraryType
    ) { (tunes, downloading, selectedId, previewId), loading, error, type ->
        val filtered = tunes.filter { type.category in it.categories }

        val uiItems = filtered.map { tune ->
            TuneUiItem(
                tune = tune,
                isDownloaded = tuneRepository.isTuneCached(tune.id),
                isDownloading = downloading.contains(tune.id),
                isSelected = tune.id == selectedId,
                isPlayingPreview = tune.id == previewId
            )
        }

        TuneLibraryUiState(
            isLoading = loading,
            errorMessage = error,
            tunes = uiItems,
            totalCacheSizeBytes = tuneRepository.getTotalCacheSizeBytes(),
            isDefaultSelected = selectedId == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TuneLibraryUiState())

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    fun initialize(type: TuneTarget) {
        _libraryType.value = type
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = tuneRepository.fetchCatalog()
            result.onSuccess { list ->
                _rawTunes.value = list
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to load tunes"
            }
            _isLoading.value = false
        }
    }

    fun selectDefault() {
        viewModelScope.launch {
            tuneRepository.selectDefault(_libraryType.value)
        }
    }

    fun selectTune(tune: TuneItem) {
        viewModelScope.launch {
            if (!tuneRepository.isTuneCached(tune.id)) {
                _downloadingIds.value = _downloadingIds.value + tune.id
                val success = try {
                    tuneRepository.select(_libraryType.value, tune)
                } finally {
                    _downloadingIds.value = _downloadingIds.value - tune.id
                }
                if (!success) {
                    _errorMessage.value = "Failed to download ${tune.name}"
                    return@launch
                }
            } else if (!tuneRepository.select(_libraryType.value, tune)) {
                _errorMessage.value = "Tune is no longer available"
            }
        }
    }

    fun removeTune(tune: TuneItem) {
        viewModelScope.launch {
            // Cannot remove currently selected tune
            if (tune.id == selectedTuneId.value) return@launch
            tuneRepository.removeCachedTune(tune.id)
            // Trigger recalculation of state
            _rawTunes.value = ArrayList(_rawTunes.value)
        }
    }

    fun setPreviewingTune(tuneId: String?) {
        _previewingId.value = tuneId
    }
}
