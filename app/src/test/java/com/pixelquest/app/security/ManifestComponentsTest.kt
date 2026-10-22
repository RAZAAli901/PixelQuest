package com.pixelquest.app.security

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * S040, S042: of the app's own components, only the launcher activity is exported. Receivers that
 * take the system's broadcasts (boot, update, time) needn't be: system broadcasts reach
 * non-exported receivers, and other apps can't.
 */
class ManifestComponentsTest {

    private val manifest = listOf(File("src/main/AndroidManifest.xml"), File("app/src/main/AndroidManifest.xml")).first { it.exists() }
    private val ns = "http://schemas.android.com/apk/res/android"

    @Test
    fun onlyTheLauncherIsExported() {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(manifest)
        val exported = listOf("activity", "service", "receiver", "provider").flatMap { tag ->
            val nodes = doc.getElementsByTagName(tag)
            (0 until nodes.length).map { nodes.item(it) as Element }
        }.filter { it.getAttributeNS(ns, "exported") == "true" }.map { it.getAttributeNS(ns, "name") }
        assertEquals(listOf(".MainActivity"), exported)
    }
}
