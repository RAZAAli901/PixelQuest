package com.pixelquest.app.quran.data

import com.pixelquest.app.quran.data.local.QuranAyahEntity
import com.pixelquest.app.quran.data.local.QuranDao
import com.pixelquest.app.quran.data.local.QuranSurahEntity
import com.pixelquest.app.quran.data.local.TranslationInfoEntity
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.domain.Translation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The Quran on the phone, for the reader (docs/quran-data.md). Everything here works offline. */
interface QuranRepository {
    /** Whether the Quran is on the phone yet (its loader runs on the first start). */
    val isReady: Flow<Boolean>

    /** All 114 surahs, in order: empty until the Quran is on the phone. */
    fun surahs(): Flow<List<Surah>>

    suspend fun surah(number: Int): Surah?

    /** A page of [surah]'s ayahs, in order: page 0 is its first [pageSize]. */
    suspend fun ayahs(surah: Int, page: Int, pageSize: Int = PAGE_SIZE): List<Ayah>

    /** [surah]'s ayahs [from] to [to], both included. */
    suspend fun ayahRange(surah: Int, from: Int, to: Int): List<Ayah>

    suspend fun ayah(surah: Int, number: Int): Ayah?

    /** Ayahs whose translation has the words in [text], in the Quran's order: at most [limit]. */
    suspend fun searchTranslation(text: String, limit: Int = SEARCH_LIMIT): List<Ayah>

    /** The translations on the phone, with their credits and terms. */
    fun translations(): Flow<List<Translation>>

    companion object {
        const val PAGE_SIZE = 50
        const val SEARCH_LIMIT = 200
    }
}

@Singleton
class QuranRepositoryImpl @Inject constructor(private val dao: QuranDao) : QuranRepository {

    override val isReady: Flow<Boolean> = dao.observeTranslations().map { it.isNotEmpty() }

    override fun surahs(): Flow<List<Surah>> = dao.observeSurahs().map { list -> list.map { it.toDomain() } }

    override suspend fun surah(number: Int): Surah? = dao.getSurah(number)?.toDomain()

    override suspend fun ayahs(surah: Int, page: Int, pageSize: Int): List<Ayah> =
        dao.ayahsOf(surah, offset = page * pageSize, limit = pageSize).map { it.toDomain() }

    override suspend fun ayahRange(surah: Int, from: Int, to: Int): List<Ayah> =
        dao.ayahRange(surah, from, to).map { it.toDomain() }

    override suspend fun ayah(surah: Int, number: Int): Ayah? = dao.getAyah(surah, number)?.toDomain()

    override suspend fun searchTranslation(text: String, limit: Int): List<Ayah> {
        val match = TranslationSearch.match(text) ?: return emptyList()
        return dao.searchTranslation(match, limit).map { it.toDomain() }
    }

    override fun translations(): Flow<List<Translation>> = dao.observeTranslations().map { list -> list.map { it.toDomain() } }
}

/** Turning what the player typed into a full-text query over the translation. */
object TranslationSearch {
    /**
     * The words of [text], each quoted so nothing in it is read as query syntax; the last one also
     * matches as the start of a word, since the player may still be typing it. All must be there.
     * Null when there are no words.
     */
    fun match(text: String): String? {
        val words = Regex("[\\p{L}\\p{N}]+").findAll(text).map { it.value }.toList()
        if (words.isEmpty()) return null
        return words.mapIndexed { i, word -> if (i == words.lastIndex) "\"$word\"*" else "\"$word\"" }.joinToString(" ")
    }
}

private fun QuranSurahEntity.toDomain() = Surah(
    number = number,
    arabicName = arabicName,
    englishName = englishName,
    englishMeaning = englishMeaning,
    revelation = Revelation.of(revelation),
    ayahCount = ayahCount,
    juz = juz,
    bismillah = bismillah
)

private fun QuranAyahEntity.toDomain() = Ayah(id, surah, ayah, arabic, translation, translationId)

private fun TranslationInfoEntity.toDomain() = Translation(id, name, translator, language, lastUpdate, licenceNote, notice)
