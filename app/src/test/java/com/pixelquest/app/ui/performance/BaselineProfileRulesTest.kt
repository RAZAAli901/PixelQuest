package com.pixelquest.app.ui.performance

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates baseline profile rules and integrity for ART AOT pre-compilation.
 */
class BaselineProfileRulesTest {

    private fun findBaselineProfileFile(): File {
        val candidates = listOf(
            File("src/main/baseline-prof.txt"),
            File("app/src/main/baseline-prof.txt"),
            File("../app/src/main/baseline-prof.txt"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("baseline-prof.txt not found in expected locations")
    }

    @Test
    fun baselineProfile_existsAndNotEmpty() {
        val file = findBaselineProfileFile()
        assertTrue("baseline-prof.txt must exist", file.exists())
        assertTrue("baseline-prof.txt must not be empty", file.length() > 0)
    }

    @Test
    fun baselineProfile_containsStartupAndScreenRules() {
        val content = findBaselineProfileFile().readText()

        // Critical entry points
        assertTrue("Must include MainActivity rule", content.contains("MainActivity"))
        assertTrue("Must include Application rule", content.contains("PixelQuestApplication"))
        assertTrue("Must include NavGraph rule", content.contains("PqNavGraph"))
        assertTrue("Must include HomeScreen rule", content.contains("HomeScreen"))
        assertTrue("Must include Theme rule", content.contains("PqTheme"))
        assertTrue("Must include Database rule", content.contains("PixelQuestDatabase"))

        // Profile rule prefixes
        assertTrue("Must have Hot/Startup/PostStartup (HSP) rules", content.contains("HSPLcom/pixelquest/app/"))
        assertTrue("Must have class descriptor (L) rules", content.contains("Lcom/pixelquest/app/"))
    }
}
