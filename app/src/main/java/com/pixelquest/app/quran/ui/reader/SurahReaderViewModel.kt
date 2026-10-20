package com.pixelquest.app.quran.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.quran.data.BookmarkRepository
import com.pixelquest.app.quran.data.LastRead
import com.pixelquest.app.quran.data.LastReadStore
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.data.ReadingSettingsStore
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Surah
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
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
 * @param target the ayah to bring into view once it's loaded: the one the reader was opened at, or
 *   where the player had scrolled to before a rotation; null for the surah's start.
 * @param flash the ayah to mark for a moment once it's in view: the one the reader was opened at
 *   (from a search, a bookmark, Continue reading), so the eye finds it. Not after a rotation.
 * @param settings the sizes, spacing and display mode the player reads with.
 * @param previous the surah before this one, and [next] the one after: null at either end.
 * @param translator who made the translation being read, for its credit when an ayah is shared.
 * @param bookmarked the surah's bookmarked ayahs, by number.
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
    val translator: String = "",
    val bookmarked: Set<Int> = emptySet(),
    val flash: Int? = null
) {
    val displayMode: DisplayMode get() = settings.displayMode
}

/**
 * One surah in the reader: its ayahs a page at a time as the player scrolls (QuranRepository.PAGE_SIZE),
 * where they've scrolled to, and how each ayah is shown. Opened at an ayah, it loads the pages up to
 * it at once. Where the player settles is kept as the last place read, for "Continue reading".
 */
@HiltViewModel
class SurahReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: QuranRepository,
    private val settingsStore: ReadingSettingsStore,
    private val bookmarks: BookmarkRepository,
    private val lastRead: LastReadStore
) : ViewModel() {

    /** The surah this reader shows ([ARG_SURAH] in its route). */
    val surahNumber: Int = savedStateHandle.get<Int>(ARG_SURAH) ?: 1

    private val _state = MutableStateFlow(
        run {
            val scrolled = savedStateHandle.get<Int>(KEY_SCROLLED)
            val opened = savedStateHandle.get<Int>(ARG_AYAH)?.takeIf { it >= 1 }
            // Back after a rotation: where they'd scrolled to. Else the ayah the reader was opened at, marked.
            ReaderUiState(target = scrolled ?: opened, flash = opened.takeIf { scrolled == null })
        }
    )
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var nextPage = 0

    /** The ayah first in view, as the player scrolls. */
    private val reading = MutableStateFlow<Int?>(null)

    init {
        // How the player reads, as last kept on this phone.
        viewModelScope.launch {
            settingsStore.settings.collect { settings -> _state.update { it.copy(settings = settings) } }
        }
        // The last place read: once the player settles, not at every ayah a fling passes.
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            reading.filterNotNull().debounce(KEEP_PLACE_AFTER_MS).collect { ayah -> lastRead.save(LastRead(surahNumber, ayah)) }
        }
        viewModelScope.launch {
            bookmarks.bookmarkedIn(surahNumber).collect { marked -> _state.update { it.copy(bookmarked = marked) } }
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

    /** Bookmarks [ayah] of this surah, or removes its bookmark. */
    fun toggleBookmark(ayah: Int) {
        viewModelScope.launch { bookmarks.toggle(surahNumber, ayah) }
    }

    /**
     * The player is reading [ayah] (the first in view): kept, so a rotation brings them back to it,
     * and, once they settle there, as the last place read.
     */
    fun onScrolled(ayah: Int) {
        savedStateHandle[KEY_SCROLLED] = ayah
        reading.value = ayah
    }

    /** The reader has brought [target][ReaderUiState.target] into view: it's done with it. */
    fun onTargetShown() {
        _state.update { it.copy(target = null) }
    }

    /** The [flash][ReaderUiState.flash] has been shown: it fades. */
    fun onFlashShown() {
        _state.update { it.copy(flash = null) }
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

        /** How long the player stays at an ayah before it's kept as the last place read. */
        const val KEEP_PLACE_AFTER_MS = 500L
    }
}
