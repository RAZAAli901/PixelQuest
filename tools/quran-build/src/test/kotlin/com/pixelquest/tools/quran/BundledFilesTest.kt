package com.pixelquest.tools.quran

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * The files the app bundles (app/src/main/assets/quran) are exactly what Tanzil's files build, and
 * whole: 114 surahs and 6,236 ayahs in each. ChecksumsTest guards the sources; this guards what's
 * made from them, so an edit to a bundled file (one letter of one ayah) fails CI too.
 */
class BundledFilesTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val bundled = File("app/src/main/assets/quran")

    private fun text(file: File) = GZIPInputStream(file.inputStream()).use { it.readBytes().toString(Charsets.UTF_8) }

    @Test
    fun theBundledFiles_areWhatTanzilsFilesBuild() {
        val built = temp.newFolder("built")
        QuranBuild(File("data/quran-source"), built).run()
        for (name in listOf(Output.QURAN, Output.TRANSLATION)) {
            // The text inside, compared whole: gzip's own header may differ from one run to the next.
            assertEquals("$name is what the sources build", text(File(built, name)), text(File(bundled, name)))
        }
    }

    @Test
    fun theBundledFiles_holdEverySurahAndAyah() {
        for (name in listOf(Output.QURAN, Output.TRANSLATION)) {
            val surahs = Json.parseToJsonElement(text(File(bundled, name))).jsonObject.getValue("surahs").jsonArray
            assertEquals(name, 114, surahs.size)
            assertEquals(name, (1..114).toList(), surahs.map { it.jsonObject.getValue("number").jsonPrimitive.int })
            assertEquals(name, 6_236, surahs.sumOf { it.jsonObject.getValue("ayahs").jsonArray.size })
        }
    }

    @Test
    fun oneLetterChangedInABundledFile_isCaught() {
        val built = temp.newFolder("built")
        QuranBuild(File("data/quran-source"), built).run()
        val edited = text(File(bundled, Output.TRANSLATION)).replaceFirst("Merciful", "Mercifull")
        assertNotEquals("the edit took", text(File(bundled, Output.TRANSLATION)), edited)
        // The comparison theBundledFiles_areWhatTanzilsFilesBuild makes, on the edited text: it differs.
        assertNotEquals(text(File(built, Output.TRANSLATION)), edited)
    }
}
