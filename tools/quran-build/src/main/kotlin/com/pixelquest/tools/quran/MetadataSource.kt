package com.pixelquest.tools.quran

import java.io.File

/**
 * One surah's details, from Tanzil's metadata.
 *
 * @param englishName its name in English letters (Tanzil's `tname`, such as Al-Faatiha).
 * @param englishMeaning what its name means (Tanzil's `ename`, such as The Opening).
 * @param revelation where it was revealed: `Meccan` or `Medinan`, as Tanzil writes it.
 * @param revelationOrder its place in the order of revelation.
 * @param juz the juz its first ayah is in.
 */
data class SurahInfo(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishMeaning: String,
    val revelation: String,
    val ayahCount: Int,
    val revelationOrder: Int,
    val juz: Int
)

/** Where a juz starts. */
data class JuzStart(val number: Int, val surah: Int, val ayah: Int)

/**
 * Tanzil's metadata: the surahs and the juz, with its copyright line (its root element's
 * `copyright`, `license` and `version`), which goes with every file made from it.
 */
data class Metadata(val notice: String, val surahs: List<SurahInfo>, val juzs: List<JuzStart>)

/** Reads Tanzil's `quran-data.xml`: `<suras>` of `<sura>`, and `<juzs>` of `<juz sura aya>`. */
object MetadataSource {
    fun read(file: File): Metadata {
        val root = Xml.read(file).documentElement
        if (root.tagName != "quran") throw QuranBuildException("${file.name}'s root is <${root.tagName}>, not <quran>")
        val notice = "Tanzil Quran Metadata, version ${root.getAttribute("version")}. " +
            "Copyright ${root.getAttribute("copyright")}. License: ${root.getAttribute("license")}."
        val juzs = section(root, "juzs").let { Xml.children(it, "juz") }.map {
            JuzStart(Xml.int(it, "index"), Xml.int(it, "sura"), Xml.int(it, "aya"))
        }
        val surahs = section(root, "suras").let { Xml.children(it, "sura") }.map { sura ->
            val number = Xml.int(sura, "index")
            SurahInfo(
                number = number,
                arabicName = text(sura, "name"),
                englishName = text(sura, "tname"),
                englishMeaning = text(sura, "ename"),
                revelation = text(sura, "type"),
                ayahCount = Xml.int(sura, "ayas"),
                revelationOrder = Xml.int(sura, "order"),
                juz = juzOf(number, 1, juzs)
            )
        }
        return Metadata(notice, surahs, juzs)
    }

    /** The juz that [surah]:[ayah] is in: the last to start at or before it. */
    fun juzOf(surah: Int, ayah: Int, juzs: List<JuzStart>): Int =
        juzs.lastOrNull { it.surah < surah || (it.surah == surah && it.ayah <= ayah) }?.number
            ?: throw QuranBuildException("no juz starts at or before $surah:$ayah")

    private fun section(root: org.w3c.dom.Element, tag: String) =
        Xml.children(root, tag).singleOrNull() ?: throw QuranBuildException("the metadata has no single <$tag>")

    private fun text(element: org.w3c.dom.Element, name: String): String =
        element.getAttribute(name).ifEmpty { throw QuranBuildException("surah ${element.getAttribute("index")} has no $name") }
}
