package com.pixelquest.app.quran.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

// The app's bundled Quran files (assets/quran), as tools/quran-build writes them (docs/quran-data.md).
// These mirror its QuranFile and TranslationFile; QuranImporterTest reads the real files with them.

/** `quran_ar.json`: the Arabic text, each surah's details and Bismillah, and the juz. */
@Serializable
data class QuranFile(
    val version: String,
    val source: String,
    val notice: String,
    val metadataNotice: String,
    val surahs: List<QuranFileSurah>,
    val juzs: List<QuranFileJuz>
)

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

/** `quran_<language>.json`: one translation, with its credits, notice and terms. */
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

/** Reads a bundled Quran file by name, such as `quran_ar.json`. */
fun interface QuranAssetSource {
    fun read(name: String): String

    /** The start of a file, at most [chars] long: enough for its version, without reading it all. */
    fun head(name: String, chars: Int): String = read(name).take(chars)
}

/** The files in the APK's assets/quran. */
@Singleton
class AndroidQuranAssetSource @Inject constructor(
    @ApplicationContext private val context: Context
) : QuranAssetSource {
    override fun read(name: String): String =
        context.assets.open("$FOLDER/$name").use { it.readBytes().toString(Charsets.UTF_8) }

    override fun head(name: String, chars: Int): String =
        context.assets.open("$FOLDER/$name").bufferedReader(Charsets.UTF_8).use { reader ->
            // One read may return less than asked: keep reading until full, or the file ends.
            val buffer = CharArray(chars)
            var filled = 0
            while (filled < chars) {
                val n = reader.read(buffer, filled, chars - filled)
                if (n < 0) break
                filled += n
            }
            String(buffer, 0, filled)
        }

    private companion object {
        const val FOLDER = "quran"
    }
}
