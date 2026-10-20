package com.pixelquest.tools.quran

import java.io.File

/**
 * A translation as Tanzil gives it: who made it, its notice (the `#` lines, kept whole), and each
 * ayah's text by surah and ayah number, untouched.
 *
 * @param id Tanzil's id for it, such as `en.sahih`.
 */
data class TranslationText(
    val id: String,
    val name: String,
    val translator: String,
    val language: String,
    val lastUpdate: String,
    val notice: String,
    val ayahs: Map<Pair<Int, Int>, String>
)

/**
 * Reads a Tanzil translation in its "Text (with aya numbers)" format: one ayah a line,
 * `surah|ayah|text`, then its notice in lines starting with `#`. The text is everything after the
 * second `|`, exactly: never trimmed or changed.
 */
object TranslationSource {
    fun read(file: File): TranslationText {
        val lines = Text.lines(file)
        val notice = lines.filter { it.startsWith("#") }
        val ayahs = LinkedHashMap<Pair<Int, Int>, String>()
        lines.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
            val parts = line.split('|', limit = 3)
            val surah = parts.getOrNull(0)?.toIntOrNull()
            val ayah = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size != 3 || surah == null || ayah == null) throw QuranBuildException("${file.name}: not surah|ayah|text: ${line.take(40)}")
            if (ayahs.put(surah to ayah, parts[2]) != null) throw QuranBuildException("${file.name}: $surah:$ayah twice")
        }
        fun field(name: String) = notice.firstNotNullOfOrNull { Regex("^#\\s+$name:\\s*(.+?)\\s*$").find(it)?.groupValues?.get(1) }
            ?: throw QuranBuildException("${file.name}'s notice has no $name")
        return TranslationText(
            id = field("ID"),
            name = field("Name"),
            translator = field("Translator"),
            language = field("Language"),
            lastUpdate = field("Last Update"),
            notice = notice.joinToString("\n"),
            ayahs = ayahs
        )
    }
}

/** One ayah with its translation. */
data class JoinedAyah(val number: Int, val arabic: String, val translation: String)

/** One surah's ayahs, each with its translation. */
data class JoinedSurah(val number: Int, val ayahs: List<JoinedAyah>)

/** The Arabic and a translation, ayah by ayah. */
object Join {
    /** Each Arabic ayah with its translation; fails if either has an ayah the other doesn't. */
    fun of(arabic: ArabicText, translation: TranslationText): List<JoinedSurah> {
        val arabicKeys = arabic.surahs.flatMap { s -> s.ayahs.map { s.number to it.number } }.toSet()
        val extra = translation.ayahs.keys - arabicKeys
        if (extra.isNotEmpty()) throw QuranBuildException("${translation.id} has ayahs the Arabic doesn't: ${extra.take(5)}")
        return arabic.surahs.map { surah ->
            JoinedSurah(surah.number, surah.ayahs.map { ayah ->
                val text = translation.ayahs[surah.number to ayah.number]
                    ?: throw QuranBuildException("${translation.id} has no ${surah.number}:${ayah.number}")
                JoinedAyah(ayah.number, ayah.text, text)
            })
        }
    }
}
