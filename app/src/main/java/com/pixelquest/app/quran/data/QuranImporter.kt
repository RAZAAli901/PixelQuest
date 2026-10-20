package com.pixelquest.app.quran.data

import com.pixelquest.app.data.local.TransactionRunner
import com.pixelquest.app.quran.data.local.QuranAyahEntity
import com.pixelquest.app.quran.data.local.QuranDao
import com.pixelquest.app.quran.data.local.QuranSurahEntity
import com.pixelquest.app.quran.data.local.TranslationInfoEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** The bundled files aren't whole, or don't agree: nothing was written. */
class QuranImportException(message: String) : Exception(message)

/** What a load wrote: the bundle's [version], and its counts. */
data class QuranImport(val version: String, val surahs: Int, val ayahs: Int)

/**
 * Loads the Quran from the app's bundled files into the database (docs/quran-data.md), all in one
 * transaction: the surahs, then every ayah with its translation, then the translation's details,
 * whose version marks the load done. A failure leaves the database as it was.
 *
 * The text goes in exactly as the files hold it; they're checked first (the standard count, and the
 * translation matching the Arabic ayah for ayah).
 */
@Singleton
class QuranImporter @Inject constructor(
    private val assets: QuranAssetSource,
    private val dao: QuranDao,
    private val transactions: TransactionRunner
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun import(): QuranImport {
        val quran = json.decodeFromString<QuranFile>(assets.read(QURAN_FILE))
        val translation = json.decodeFromString<TranslationFile>(assets.read(TRANSLATION_FILE))
        check(quran, translation)

        val surahs = quran.surahs.map {
            QuranSurahEntity(
                number = it.number,
                arabicName = it.arabicName,
                englishName = it.englishName,
                englishMeaning = it.englishMeaning,
                revelation = it.revelation,
                revelationOrder = it.revelationOrder,
                ayahCount = it.ayahs.size,
                juz = it.juz,
                bismillah = it.bismillah
            )
        }
        var id = 0
        val ayahs = quran.surahs.zip(translation.surahs).flatMap { (surah, translated) ->
            surah.ayahs.mapIndexed { i, arabic ->
                QuranAyahEntity(++id, surah.number, i + 1, arabic, translation.id, translated.ayahs[i])
            }
        }
        val info = TranslationInfoEntity(
            id = translation.id,
            name = translation.name,
            translator = translation.translator,
            language = translation.language,
            lastUpdate = translation.lastUpdate,
            licenceNote = translation.terms,
            notice = translation.notice,
            dataVersion = quran.version
        )

        transactions.inTransaction {
            dao.deleteAyahs()
            dao.deleteSurahs()
            dao.insertSurahs(surahs)
            dao.insertAyahs(ayahs)
            // Last: its version says the Quran on the phone is complete.
            dao.upsertTranslation(info)
        }
        return QuranImport(quran.version, surahs.size, ayahs.size)
    }

    private fun check(quran: QuranFile, translation: TranslationFile) {
        fun fail(message: String): Nothing = throw QuranImportException(message)
        if (quran.surahs.size != SURAHS) fail("$QURAN_FILE has ${quran.surahs.size} surahs, not $SURAHS")
        if (quran.surahs.sumOf { it.ayahs.size } != AYAHS) fail("$QURAN_FILE has ${quran.surahs.sumOf { it.ayahs.size }} ayahs, not $AYAHS")
        if (translation.version != quran.version) fail("$TRANSLATION_FILE is from bundle ${translation.version}, not ${quran.version}")
        if (translation.surahs.map { it.number to it.ayahs.size } != quran.surahs.map { it.number to it.ayahs.size }) {
            fail("$TRANSLATION_FILE's ayahs don't match the Arabic's")
        }
    }

    companion object {
        const val QURAN_FILE = "quran_ar.json"
        const val TRANSLATION_FILE = "quran_en.json"
        const val SURAHS = 114
        const val AYAHS = 6236
    }
}
