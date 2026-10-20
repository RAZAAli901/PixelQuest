package com.pixelquest.tools.quran

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * The Quran file the app bundles (`quran_ar.json`): the Arabic text, each surah's details and
 * Bismillah, and the juz. It carries Tanzil's notices, since its terms require them in every file
 * made from the text.
 *
 * @param version changes whenever any source file does, so the app knows to reload its copy.
 */
@Serializable
data class QuranFile(
    val version: String,
    val source: String,
    val notice: String,
    val metadataNotice: String,
    val surahs: List<QuranFileSurah>,
    val juzs: List<QuranFileJuz>
)

/** One surah: its details, its Bismillah (null for Al-Fatihah and At-Tawbah), and its ayahs' text in order. */
@Serializable
data class QuranFileSurah(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishMeaning: String,
    val revelation: String,
    val revelationOrder: Int,
    val juz: Int,
    val bismillah: String?,
    val ayahs: List<String>
)

@Serializable
data class QuranFileJuz(val number: Int, val surah: Int, val ayah: Int)

/**
 * A translation file the app bundles (`quran_<language>.json`), one per translation so another can
 * be added as a file. It carries the translation's notice and its terms.
 */
@Serializable
data class TranslationFile(
    val version: String,
    val id: String,
    val name: String,
    val translator: String,
    val language: String,
    val lastUpdate: String,
    val source: String,
    val notice: String,
    val terms: String,
    val surahs: List<TranslationFileSurah>
)

@Serializable
data class TranslationFileSurah(val number: Int, val ayahs: List<String>)

/**
 * Writing the app's files, gzipped, then reading them back to check every ayah came through
 * unchanged. The APK deflates its assets anyway, so gzip saves little there; it keeps these files
 * small in the repository (sizes in docs/quran-data.md).
 */
object Output {
    const val QURAN = "quran_ar.json.gz"
    const val TRANSLATION = "quran_en.json.gz"

    /** Where Tanzil publishes the text and its updates: its terms ask for the link. */
    const val TANZIL = "Tanzil Project, https://tanzil.net"

    const val TRANSLATION_TERMS = "Tanzil's translations are for non-commercial use only; other use needs the permission " +
        "of the translator or the publisher (https://tanzil.net/trans/)."

    private val json = Json { encodeDefaults = true }

    /** A version for the data, from the source files' checksums: the same sources always give the same one. */
    fun version(files: SourceFiles): String =
        MessageDigest.getInstance("SHA-256")
            .digest(files.all.joinToString("\n") { "${it.name}=${Checksums.sha256(it)}" }.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)

    fun quran(version: String, arabic: ArabicText, metadata: Metadata, joined: List<JoinedSurah>) = QuranFile(
        version = version,
        source = TANZIL,
        notice = arabic.notice,
        metadataNotice = metadata.notice,
        surahs = joined.zip(metadata.surahs).map { (surah, info) ->
            QuranFileSurah(
                number = surah.number,
                arabicName = info.arabicName,
                englishName = info.englishName,
                englishMeaning = info.englishMeaning,
                revelation = info.revelation,
                revelationOrder = info.revelationOrder,
                juz = info.juz,
                bismillah = surah.bismillah,
                ayahs = surah.ayahs.map { it.arabic }
            )
        },
        juzs = metadata.juzs.map { QuranFileJuz(it.number, it.surah, it.ayah) }
    )

    fun translation(version: String, translation: TranslationText, joined: List<JoinedSurah>) = TranslationFile(
        version = version,
        id = translation.id,
        name = translation.name,
        translator = translation.translator,
        language = translation.language,
        lastUpdate = translation.lastUpdate,
        source = TANZIL,
        notice = translation.notice,
        terms = TRANSLATION_TERMS,
        surahs = joined.map { surah -> TranslationFileSurah(surah.number, surah.ayahs.map { it.translation }) }
    )

    /** Writes both files to [folder]; then reads them back, and fails unless every ayah is [joined]'s, exactly. */
    fun write(folder: File, quran: QuranFile, translation: TranslationFile, joined: List<JoinedSurah>): List<File> {
        folder.mkdirs()
        val quranFile = File(folder, QURAN).apply { writeGzip(this, json.encodeToString(quran) + "\n") }
        val translationFile = File(folder, TRANSLATION).apply { writeGzip(this, json.encodeToString(translation) + "\n") }

        val readQuran = json.decodeFromString<QuranFile>(readGzip(quranFile))
        val readTranslation = json.decodeFromString<TranslationFile>(readGzip(translationFile))
        val arabicBack = readQuran.surahs.map { it.number to it.ayahs }
        val translationBack = readTranslation.surahs.map { it.number to it.ayahs }
        if (arabicBack != joined.map { s -> s.number to s.ayahs.map { it.arabic } }) {
            throw QuranBuildException("$QURAN doesn't read back as the source's text")
        }
        if (translationBack != joined.map { s -> s.number to s.ayahs.map { it.translation } }) {
            throw QuranBuildException("$TRANSLATION doesn't read back as the source's text")
        }
        if (readQuran.surahs.map { it.bismillah } != joined.map { it.bismillah }) {
            throw QuranBuildException("$QURAN's Bismillahs don't read back as the source's")
        }
        return listOf(quranFile, translationFile)
    }

    /** [text] as UTF-8, gzipped at the best compression. */
    fun writeGzip(file: File, text: String) {
        file.outputStream().use { out -> BestGzip(out).use { it.write(text.toByteArray(Charsets.UTF_8)) } }
    }

    fun readGzip(file: File): String = GZIPInputStream(file.inputStream()).use { it.readBytes().toString(Charsets.UTF_8) }

    private class BestGzip(out: OutputStream) : GZIPOutputStream(out) {
        init {
            def.setLevel(Deflater.BEST_COMPRESSION)
        }
    }
}
