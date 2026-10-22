package com.pixelquest.app.security

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * S044: the app has no WebView, so there's no JavaScript bridge or file access to misuse. Links
 * open in the phone's browser (the Quran's sources page) and files in a viewer app.
 */
class NoWebViewTest {

    private val main = listOf(File("src/main"), File("app/src/main")).first { it.exists() }

    @Test
    fun noWebViewAnywhere() {
        val found = main.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml") }
            .filter { file -> Regex("""\bWebView\b|setJavaScriptEnabled|javaScriptEnabled|addJavascriptInterface""").containsMatchIn(file.readText()) }
            .map { it.name }.toList()
        assertEquals(emptyList<String>(), found)
    }
}
