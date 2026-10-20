package com.pixelquest.app.quran.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The sources the About screen shows: read from the bundle itself, and the same as the repository's licence files. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuranSourcesTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val sources = QuranSources.read(AndroidQuranAssetSource(context))!!

    private fun repoFile(path: String) = listOf(File("../$path"), File(path)).first { it.exists() }

    @Test
    fun tanzilsNotice_isTheFilesOwn_whole() {
        assertTrue(sources.notice.startsWith("# PLEASE DO NOT REMOVE OR CHANGE THIS COPYRIGHT BLOCK"))
        assertTrue(sources.notice.contains("CHANGING IT IS NOT ALLOWED"))
        assertTrue("licenses/tanzil-quran-text.txt holds the same notice", repoFile("licenses/tanzil-quran-text.txt").readText().contains(sources.notice))
    }

    @Test
    fun theMetadataNotice_namesItsLicence() {
        assertTrue(sources.metadataNotice.contains("Tanzil Quran Metadata"))
        assertTrue(sources.metadataNotice.contains("cc-by"))
    }

    @Test
    fun theVersion_isTheBundles() {
        val head = AndroidQuranAssetSource(context).head(QuranImporter.QURAN_FILE, 100)
        assertTrue(head.contains(sources.version))
    }

    @Test
    fun theDownloadDate_isTheOneRecorded() {
        assertTrue(repoFile("licenses/README.md").readText().contains("downloaded on **${QuranSources.DOWNLOADED}**"))
    }

    @Test
    fun aBundleWithoutThem_hasNone() {
        assertNull(QuranSources.read { """{"version":"v","source":"x"}""" })
        assertEquals("a \"quoted\" line\nand more", QuranSources.read { """{"version":"v","notice":"a \"quoted\" line\nand more","metadataNotice":"m"}""" }!!.notice)
    }
}
