package com.pixelquest.app.quran.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.quran.data.ArabicSearch
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.list.SurahSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
 * @param ayahs the ayah a reference names ("2:255"), then those whose translation has the words,
 *   or, for Arabic typed, whose Arabic has them.
 * @param capped there were more ayahs than [QuranRepository.SEARCH_LIMIT]: only the first are here.
 * @param arabic the search was in the Arabic: the results show it.
 */
data class QuranSearchUiState(
    val query: String = "",
    val surahs: List<Surah> = emptyList(),
    val ayahs: List<AyahResult> = emptyList(),
    val capped: Boolean = false,
    val arabic: Boolean = false,
) {
    val isEmpty: Boolean get() = surahs.isEmpty() && ayahs.isEmpty()
}

/**
 * Searching the whole Quran as the player types: surahs by number or name (as the surah list
 * does), an ayah by its reference, and ayahs by the words of their translation (full-text, on the
 * phone) or, typed in Arabic, by their Arabic, marks or none (ArabicSearch). A newer search
 * replaces one still running.
 */
@HiltViewModel
class QuranSearchViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _query = savedStateHandle.getStateFlow(KEY_QUERY, "")

    /** What's in the search field, at once: the results follow it. */
    val query: StateFlow<String> = _query

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<QuranSearchUiState> = combine(_query, repository.surahs()) { q, surahs -> q to surahs }
        .mapLatest { (q, surahs) -> if (q.isBlank()) QuranSearchUiState(query = q) else search(q, surahs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranSearchUiState())

    fun search(text: String) {
        savedStateHandle[KEY_QUERY] = com.pixelquest.app.domain.validation.InputRules.typed(text, com.pixelquest.app.domain.validation.InputRules.Field.SEARCH)
    }

    private suspend fun search(q: String, surahs: List<Surah>): QuranSearchUiState {
        val names = surahs.associate { it.number to it.englishName }
        val named = reference(q)?.let { (surah, ayah) -> repository.ayah(surah, ayah) }
        val arabic = ArabicSearch.hasArabic(q)
        val found = if (arabic) {
            repository.searchArabic(q, QuranRepository.SEARCH_LIMIT)
        } else {
            repository.searchTranslation(q, QuranRepository.SEARCH_LIMIT)
        }
        val ayahs = (listOfNotNull(named) + found).distinctBy { it.id }
        return QuranSearchUiState(
            query = q,
            surahs = SurahSearch.filter(surahs, q),
            ayahs = ayahs.map { AyahResult(it, names[it.surah].orEmpty()) },
            capped = found.size >= QuranRepository.SEARCH_LIMIT,
            arabic = arabic,
        )
    }

    companion object {
        private const val KEY_QUERY = "quran_search_query"
        private val REFERENCE = Regex("""^\s*(\d{1,3})\s*[:.]\s*(\d{1,3})\s*$""")

        /** The surah and ayah [text] names as a reference ("2:255", "2.255"); null if it isn't one. */
        internal fun reference(text: String): Pair<Int, Int>? =
            REFERENCE.matchEntire(text)?.destructured?.let { (s, a) -> s.toInt() to a.toInt() }
    }
}
