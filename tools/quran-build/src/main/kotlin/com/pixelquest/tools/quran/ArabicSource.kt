package com.pixelquest.tools.quran

import org.w3c.dom.Comment
import java.io.File

/** One ayah, as its source gives it: its number in its surah, and its text, untouched. */
data class SourceAyah(val number: Int, val text: String)

/**
 * One surah's ayahs, in the source's order.
 *
 * @param bismillah the Bismillah Tanzil gives on its first ayah (the `bismillah` attribute), apart
 *   from the ayah's text; null where it has none ([Bismillah]).
 */
data class SourceSurah(val number: Int, val ayahs: List<SourceAyah>, val bismillah: String? = null)

/**
 * Tanzil's Quran text: its surahs, and its copyright notice, which must go with every copy and every
 * file made from it (licenses/tanzil-quran-text.txt).
 */
data class ArabicText(val notice: String, val surahs: List<SourceSurah>)

/**
 * Reads Tanzil's XML Quran text (`quran-uthmani.xml`): `<sura index>` elements holding
 * `<aya index text>` ones. Each text is the attribute's value exactly, never trimmed or changed.
 */
object ArabicSource {
    fun read(file: File): ArabicText {
        val doc = Xml.read(file)
        val notice = (0 until doc.childNodes.length).map { doc.childNodes.item(it) }
            .filterIsInstance<Comment>()
            .firstOrNull()?.data?.trim('\r', '\n', '\t', ' ')
            ?: throw QuranBuildException("${file.name} has no copyright notice")
        val root = doc.documentElement
        if (root.tagName != "quran") throw QuranBuildException("${file.name}'s root is <${root.tagName}>, not <quran>")
        val surahs = Xml.children(root, "sura").map { sura ->
            val ayahs = Xml.children(sura, "aya")
            val number = Xml.int(sura, "index")
            ayahs.drop(1).firstOrNull { it.hasAttribute("bismillah") }?.let {
                throw QuranBuildException("$number:${it.getAttribute("index")} has a Bismillah, but only a first ayah may")
            }
            SourceSurah(
                number = number,
                ayahs = ayahs.map { aya ->
                    if (!aya.hasAttribute("text")) throw QuranBuildException("an ayah of surah $number has no text")
                    SourceAyah(Xml.int(aya, "index"), aya.getAttribute("text"))
                },
                bismillah = ayahs.firstOrNull()?.takeIf { it.hasAttribute("bismillah") }?.getAttribute("bismillah")
            )
        }
        return ArabicText(notice, surahs)
    }
}
