package com.pixelquest.app.quran.data

import com.pixelquest.app.data.local.TransactionRunner
import com.pixelquest.app.quran.data.local.QuranAyahEntity
import com.pixelquest.app.quran.data.local.QuranDao
import com.pixelquest.app.quran.data.local.QuranJuzEntity
import com.pixelquest.app.quran.data.local.QuranSurahEntity
import com.pixelquest.app.quran.data.local.TranslationInfoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** The bundled files aren't whole, or don't agree: nothing was written. */
class QuranImportException(message: String) : Exception(message)

/** What a load wrote: the bundle's [version], and its counts. */
data class QuranImport(val version: String, val surahs: Int, val ayahs: Int)

/** Where loading the Quran is, for the one-time "Preparing the Quran" message. */
sealed interface QuranLoadState {
    /** No load has run since the app started. */
    data object Idle : QuranLoadState

    /** Loading: [progress] from 0 to 1, as the surahs are written. */
    data class Loading(val progress: Float) : QuranLoadState

    data class Done(val import: QuranImport) : QuranLoadState

    /** The bundle was wrong, and nothing was written. */
    data object Failed : QuranLoadState
}

/**
 * Loads the Quran from the app's bundled files into the database (docs/quran-data.md), all in one
 * transaction: the surahs, then every ayah with its translation, then the translation's details,
 * whose version marks the load done. A failure leaves the database as it was.
 *
 * The text goes in exactly as the files hold it; they're checked first (the standard count, and the
 * translation matching the Arabic ayah for ayah). Beside each ayah goes its key for Arabic search
 * (ArabicSearch), made from it, never shown.
 */
@Singleton
class QuranImporter(
    private val assets: QuranAssetSource,
    private val dao: QuranDao,
    private val transactions: TransactionRunner,
    /** The translation file to load: the player's choice ([TranslationSwitcher]). */
    private val translationFile: () -> String
) {
    @Inject
    constructor(assets: QuranAssetSource, dao: QuranDao, transactions: TransactionRunner, switcher: TranslationSwitcher) :
        this(assets, dao, transactions, { switcher.selected().file })

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow<QuranLoadState>(QuranLoadState.Idle)

    /** The load's progress, for the reader's "Preparing the Quran" message. */
    val state: StateFlow<QuranLoadState> = _state.asStateFlow()

    /**
     * Loads the bundle unless the phone already holds it: the same version, complete (all 114
     * surahs and 6,236 ayahs). So it's safe to run at every start, and a newer bundle (a new Tanzil
     * release, or another translation) replaces the old one, in one transaction.
     *
     * @return what was loaded, or null when nothing needed to be.
     */
    suspend fun importIfNeeded(): QuranImport? {
        val file = translationFile()
        val head = assets.head(file, HEAD_CHARS)
        val version = Regex("\"version\":\"([0-9a-f]+)\"").find(head)?.groupValues?.get(1)
            ?: throw QuranImportException("$file has no version at its start")
        val id = Regex("\"id\":\"([^\"]+)\"").find(head)?.groupValues?.get(1)
            ?: throw QuranImportException("$file has no id at its start")
        // Complete means this bundle, with this translation in every ayah: a newly chosen one isn't yet.
        // And every ayah with its search key: a database updated to version 16 has none yet.
        val held = dao.getTranslation(id)
        val complete = held?.dataVersion == version && dao.surahCount() == SURAHS &&
            dao.ayahCount() == AYAHS && dao.ayahCountIn(id) == AYAHS && dao.juzCount() == JUZS &&
            dao.ayahCountWithoutPlain() == 0
        return if (complete) null else import()
    }

    suspend fun import(): QuranImport =
        try {
            load().also { _state.value = QuranLoadState.Done(it) }
        } catch (e: QuranImportException) {
            _state.value = QuranLoadState.Failed
            throw e
        }

    private suspend fun load(): QuranImport {
        _state.value = QuranLoadState.Loading(0f)
        val quran = json.decodeFromString<QuranFile>(assets.read(QURAN_FILE))
        val translation = json.decodeFromString<TranslationFile>(assets.read(translationFile()))
        check(quran, translation)
        _state.value = QuranLoadState.Loading(READ_SHARE)

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
                QuranAyahEntity(++id, surah.number, i + 1, arabic, translation.id, translated.ayahs[i], ArabicSearch.key(arabic))
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

        val juzs = quran.juzs.map { QuranJuzEntity(it.number, it.surah, it.ayah) }

        transactions.inTransaction {
            dao.deleteAyahs()
            dao.deleteSurahs()
            dao.deleteJuzs()
            dao.insertSurahs(surahs)
            dao.insertJuzs(juzs)
            // A surah at a time, so the message can show how far it's got.
            ayahs.groupBy { it.surah }.values.forEachIndexed { i, surah ->
                dao.insertAyahs(surah)
                _state.value = QuranLoadState.Loading(READ_SHARE + (1 - READ_SHARE) * (i + 1) / surahs.size)
            }
            // Last: its version says the Quran on the phone is complete.
            dao.upsertTranslation(info)
        }
        return QuranImport(quran.version, surahs.size, ayahs.size)
    }

    private fun check(quran: QuranFile, translation: TranslationFile) {
        fun fail(message: String): Nothing = throw QuranImportException(message)
        if (quran.surahs.size != SURAHS) fail("$QURAN_FILE has ${quran.surahs.size} surahs, not $SURAHS")
        if (quran.surahs.sumOf { it.ayahs.size } != AYAHS) fail("$QURAN_FILE has ${quran.surahs.sumOf { it.ayahs.size }} ayahs, not $AYAHS")
        if (quran.juzs.map { it.number } != (1..JUZS).toList()) fail("$QURAN_FILE doesn't have the $JUZS juz in order")
        if (translation.version != quran.version) fail("$TRANSLATION_FILE is from bundle ${translation.version}, not ${quran.version}")
        if (translation.surahs.map { it.number to it.ayahs.size } != quran.surahs.map { it.number to it.ayahs.size }) {
            fail("$TRANSLATION_FILE's ayahs don't match the Arabic's")
        }
    }

    companion object {
        /** Its name in the APK: the repository's `quran_ar.json.gz`, unzipped by the build ([AndroidQuranAssetSource]). */
        const val QURAN_FILE = "quran_ar.json"

        /** Saheeh International's file, the default translation ([TranslationSwitcher]). */
        const val TRANSLATION_FILE = "quran_en.json"
        const val SURAHS = 114
        const val AYAHS = 6236
        const val JUZS = 30

        /** How much of a file's start holds its version and id (tools/quran-build writes them first). */
        private const val HEAD_CHARS = 200

        /** How much of the progress reading and checking the files counts for; writing is the rest. */
        private const val READ_SHARE = 0.2f
    }
}
