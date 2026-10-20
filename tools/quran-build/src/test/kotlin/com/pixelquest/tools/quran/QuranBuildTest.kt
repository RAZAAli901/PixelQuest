package com.pixelquest.tools.quran

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The build reads Tanzil's three files, and refuses to go on without them. */
class QuranBuildTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val source = File("data/quran-source")

    @Test
    fun theRealSources_areRead() {
        QuranBuild(source, temp.newFolder("out")).run()
    }

    @Test
    fun aMissingFile_stopsTheBuild_namingIt() {
        val partial = temp.newFolder("partial")
        File(source, "quran-uthmani.xml").copyTo(File(partial, "quran-uthmani.xml"))

        try {
            QuranBuild(partial, temp.newFolder("out")).run()
            fail("built without the translation")
        } catch (e: QuranBuildException) {
            assertTrue(e.message!!, "en.sahih.txt" in e.message!!)
        }
    }

    @Test
    fun damagedUtf8_isRefused() {
        val damaged = temp.newFile("damaged.txt").apply { writeBytes(byteArrayOf(0x31, 0x7C, 0x31, 0x7C, 0xC3.toByte(), 0x28)) }
        try {
            Text.lines(damaged)
            fail("read damaged UTF-8")
        } catch (e: QuranBuildException) {
            assertTrue(e.message!!, "UTF-8" in e.message!!)
        }
    }

    @Test
    fun aDoctype_isRefused() {
        val evil = temp.newFile("evil.xml").apply {
            writeText("<?xml version=\"1.0\"?><!DOCTYPE q [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><q>&x;</q>")
        }
        try {
            Xml.read(evil)
            fail("read a DOCTYPE")
        } catch (e: QuranBuildException) {
            // Refused, as it should be: Tanzil's files have none.
        }
    }
}
