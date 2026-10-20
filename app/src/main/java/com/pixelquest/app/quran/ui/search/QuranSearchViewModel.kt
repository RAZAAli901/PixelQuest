package com.pixelquest.app.quran.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.list.SurahSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** An ayah a search found, with its surah's name for its reference. */
data class AyahResult(val ayah: Ayah, val surahName: String)

/**
 * What a search found.
 *
 * @param query the search these results are for: the field's text may already be ahead of it.
 * @param surahs the surahs whose number or name matches.
 * @param ayahs the ayah a reference names ("2:255"), then those whose translation has the words.
 * @param capped there were more ayahs than [QuranRepository.SEARCH_LIMIT]: only the first are here.
 */
data class QuranSearchUiState(
    val query: String = "",
    val surahs: List<Surah> = emptyList(),
    val ayahs: List<AyahResult> = emptyList(),
    val capped: Boolean = false
) {
    val isEmpty: Boolean get() = surahs.isEmpty() && ayahs.isEmpty()
}

/**
 * Searching the whole Quran as the player types: surahs by number or name (as the surah list
 * does), an ayah by its reference, and ayahs by the words of their translation (full-text, on the
 * phone). A newer search replaces one still running.
 */
@HiltViewModel
class QuranSearchViewModel @Inject constructor(private val repository: QuranRepository) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** What's in the search field, at once: the results follow it. */
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<QuranSearchUiState> = combine(_query, repository.surahs()) { q, surahs -> q to surahs }
        .mapLatest { (q, surahs) -> if (q.isBlank()) QuranSearchUiState(query = q) else search(q, surahs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranSearchUiState())

    fun search(text: String) {
        _query.value = text
    }

    private suspend fun search(q: String, surahs: List<Surah>): QuranSearchUiState {
        val names = surahs.associate { it.number to it.englishName }
        val named = reference(q)?.let { (surah, ayah) -> repository.ayah(surah, ayah) }
        val found = repository.searchTranslation(q, QuranRepository.SEARCH_LIMIT)
        val ayahs = (listOfNotNull(named) + found).distinctBy { it.id }
        return QuranSearchUiState(
            query = q,
            surahs = SurahSearch.filter(surahs, q),
            ayahs = ayahs.map { AyahResult(it, names[it.surah].orEmpty()) },
            capped = found.size >= QuranRepository.SEARCH_LIMIT
        )
    }

    companion object {
        private val REFERENCE = Regex("""^\s*(\d{1,3})\s*[:.]\s*(\d{1,3})\s*$""")

        /** The surah and ayah [text] names as a reference ("2:255", "2.255"); null if it isn't one. */
        internal fun reference(text: String): Pair<Int, Int>? =
            REFERENCE.matchEntire(text)?.destructured?.let { (s, a) -> s.toInt() to a.toInt() }
    }
}
