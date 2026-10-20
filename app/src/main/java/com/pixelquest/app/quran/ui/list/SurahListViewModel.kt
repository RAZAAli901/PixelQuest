package com.pixelquest.app.quran.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.domain.Surah
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.Normalizer
import javax.inject.Inject

/**
 * The surah list: all 114, or those matching what the player types.
 *
 * @param loaded false until the surahs first arrive, so the list doesn't flash "no matches".
 * @param ayahCounts every surah's ayah count, whatever the search: for "Go to surah and ayah".
 */
data class SurahListUiState(
    val query: String = "",
    val surahs: List<Surah> = emptyList(),
    val loaded: Boolean = false,
    val ayahCounts: Map<Int, Int> = emptyMap()
)

/** The surah list, with search as you type over the surahs' numbers and names. */
@HiltViewModel
class SurahListViewModel @Inject constructor(repository: QuranRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<SurahListUiState> = combine(repository.surahs(), query.asStateFlow()) { surahs, q ->
        SurahListUiState(
            query = q,
            surahs = SurahSearch.filter(surahs, q),
            loaded = surahs.isNotEmpty(),
            ayahCounts = surahs.associate { it.number to it.ayahCount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurahListUiState())

    fun search(text: String) {
        query.value = text
    }
}

/** Matching what the player types against the surahs, as they type it. */
object SurahSearch {
    /**
     * The surahs [query] finds, in order: all of them when it's blank. Digits find the surahs whose
     * number starts with them ("11" finds 11 and 110 to 114). Words find a surah by its English name
     * however it's spelt nearly (Fatiha finds Al-Faatiha: case, hyphens and doubled letters don't
     * matter), by its meaning (cow finds The Cow), or by its Arabic name, marks or none.
     */
    fun filter(surahs: List<Surah>, query: String): List<Surah> {
        val q = query.trim()
        if (q.isEmpty()) return surahs
        if (q.all { it.isDigit() }) {
            val digits = q.map { Character.getNumericValue(it) }.joinToString("")
            return surahs.filter { it.number.toString().startsWith(digits) }
        }
        val latin = latin(q)
        val arabic = arabic(q)
        return surahs.filter { surah ->
            (latin.isNotEmpty() && (latin in latin(surah.englishName) || latin in latin(surah.englishMeaning))) ||
                (arabic.isNotEmpty() && arabic in arabic(surah.arabicName))
        }
    }

    /** Latin letters only, lowercase, without accents, a run of one letter counted once. */
    internal fun latin(text: String): String {
        val plain = Normalizer.normalize(text, Normalizer.Form.NFD).lowercase().filter { it in 'a'..'z' }
        return plain.fold(StringBuilder()) { out, c -> if (out.lastOrNull() != c) out.append(c); out }.toString()
    }

    /** Arabic letters only: the marks (harakat, the small signs, tatweel) left out. */
    internal fun arabic(text: String): String =
        text.filter { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC && Character.getType(it) == Character.OTHER_LETTER.toInt() && it != 'ـ' }
}
