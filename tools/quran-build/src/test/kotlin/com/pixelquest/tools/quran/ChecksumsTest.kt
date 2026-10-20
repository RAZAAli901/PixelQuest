package com.pixelquest.tools.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Each source file is Tanzil's, byte for byte: any edit, even one mark, fails the build. */
class ChecksumsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val source = SourceFiles(File("data/quran-source"))

    @Test
    fun theSourceFiles_haveTheirRecordedChecksums() {
        Checksums.check(source.all)
    }

    @Test
    fun theRecordedChecksums_areTheOnesLicensesLists() {
        val listed = File("licenses/README.md").readText()
        Checksums.recorded.forEach { (name, sha) -> assertTrue(name, "| `$name` |" in listed && sha in listed) }
    }

    @Test
    fun oneChangedByte_failsTheBuild_namingTheFile() {
        val copy = temp.newFolder("source")
        source.all.forEach { it.copyTo(File(copy, it.name)) }
        val translation = File(copy, "en.sahih.txt")
        val bytes = translation.readBytes()
        // A full stop becomes a comma in one ayah: the kind of slip that must never reach the app.
        val at = bytes.indexOf('.'.code.toByte())
        bytes[at] = ','.code.toByte()
        translation.writeBytes(bytes)

        try {
            QuranBuild(copy, temp.newFolder("out")).run()
            fail("built from an edited translation")
        } catch (e: QuranBuildException) {
            assertTrue(e.message!!, "en.sahih.txt isn't Tanzil's file" in e.message!!)
        }
    }

    @Test
    fun lineEndingsCount_too() {
        // A checkout that turned LF into CRLF would change the bytes, and fail; .gitattributes stops it.
        val copy = temp.newFile("quran-uthmani.xml")
        copy.writeText(File("data/quran-source/quran-uthmani.xml").readText().replace("\n", "\r\n"))
        try {
            Checksums.check(listOf(copy))
            fail("passed with CRLF")
        } catch (e: QuranBuildException) {
            assertEquals(true, "quran-uthmani.xml" in e.message!!)
        }
    }
}
