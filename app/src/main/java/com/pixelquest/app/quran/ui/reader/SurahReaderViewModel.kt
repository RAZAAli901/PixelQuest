package com.pixelquest.app.quran.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.data.ReadingSettingsStore
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Surah
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the reader shows of each ayah. */
enum class DisplayMode {
    /** The Arabic alone. */
    ARABIC,

    /** The translation alone. */
    TRANSLATION,

    /** The Arabic, with its translation below: the reader's default. */
    BOTH
}

/**
 * The reader's state.
 *
 * @param ayahs the ayahs loaded so far, from the surah's first, in order.
 * @param hasMore whether the surah has ayahs not loaded yet.
 * @param target the ayah to bring into view (and flash) once it's loaded: the one the reader was
 *   opened at, or where the player had scrolled to before a rotation; null for the surah's start.
 * @param settings the sizes, spacing and display mode the player reads with.
 * @param previous the surah before this one, and [next] the one after: null at either end.
 * @param translator who made the translation being read, for its credit when an ayah is shared.
 */
data class ReaderUiState(
    val surah: Surah? = null,
    val previous: Surah? = null,
    val next: Surah? = null,
    val ayahs: List<Ayah> = emptyList(),
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val target: Int? = null,
    val settings: ReadingSettings = ReadingSettings(),
    val translator: String = ""
) {
    val displayMode: DisplayMode get() = settings.displayMode
}

/**
 * One surah in the reader: its ayahs a page at a time as the player scrolls (QuranRepository.PAGE_SIZE),
 * where they've scrolled to, and how each ayah is shown. Opened at an ayah, it loads the pages up to
 * it at once.
 */
@HiltViewModel
class SurahReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: QuranRepository,
    private val settingsStore: ReadingSettingsStore
) : ViewModel() {

    /** The surah this reader shows ([ARG_SURAH] in its route). */
    val surahNumber: Int = savedStateHandle.get<Int>(ARG_SURAH) ?: 1

    private val _state = MutableStateFlow(
        ReaderUiState(
            // Back after a rotation: where they'd scrolled to. Else the ayah the reader was opened at.
            target = savedStateHandle.get<Int>(KEY_SCROLLED) ?: savedStateHandle.get<Int>(ARG_AYAH)?.takeIf { it > 1 }
        )
    )
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var nextPage = 0

    init {
        // How the player reads, as last kept on this phone.
        viewModelScope.launch {
            settingsStore.settings.collect { settings -> _state.update { it.copy(settings = settings) } }
        }
        // The translation on the phone, for its credit (there's one at a time: TranslationSwitcher).
        viewModelScope.launch {
            repository.translations().collect { list -> _state.update { it.copy(translator = list.firstOrNull()?.translator.orEmpty()) } }
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    surah = repository.surah(surahNumber),
                    previous = repository.surah(surahNumber - 1),
                    next = repository.surah(surahNumber + 1)
                )
            }
            val target = _state.value.target
            // Up to the target at once, so it can be scrolled to; else the first page.
            do {
                loadPage()
            } while (target != null && _state.value.hasMore && _state.value.ayahs.size < target)
        }
    }

    /** Loads the next page, unless one is loading or the surah is all here: call it near the list's end. */
    fun loadMore() {
        if (_state.value.loading || !_state.value.hasMore) return
        viewModelScope.launch { loadPage() }
    }

    private suspend fun loadPage() {
        _state.update { it.copy(loading = true) }
        val page = repository.ayahs(surahNumber, nextPage++, QuranRepository.PAGE_SIZE)
        _state.update { current ->
            val ayahs = current.ayahs + page
            val count = current.surah?.ayahCount
            current.copy(
                ayahs = ayahs,
                hasMore = page.size == QuranRepository.PAGE_SIZE && (count == null || ayahs.size < count),
                loading = false
            )
        }
    }

    /** The player is reading [ayah] (the first in view): kept, so a rotation brings them back to it. */
    fun onScrolled(ayah: Int) {
        savedStateHandle[KEY_SCROLLED] = ayah
    }

    /** The reader has brought [target][ReaderUiState.target] into view: it's done with it. */
    fun onTargetShown() {
        _state.update { it.copy(target = null) }
    }

    fun setDisplayMode(mode: DisplayMode) {
        updateSettings(_state.value.settings.copy(displayMode = mode))
    }

    /** The player changed how they read (the reading settings sheet): shown at once, and kept. */
    fun updateSettings(settings: ReadingSettings) {
        _state.update { it.copy(settings = settings) }
        viewModelScope.launch { settingsStore.save(settings) }
    }

    companion object {
        /** The route's arguments: the surah, and the ayah to open it at (optional). */
        const val ARG_SURAH = "surah"
        const val ARG_AYAH = "ayah"

        private const val KEY_SCROLLED = "reader_scrolled"
    }
}
