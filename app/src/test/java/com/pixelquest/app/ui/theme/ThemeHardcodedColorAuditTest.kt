package com.pixelquest.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Step 31: Lint check asserting no hardcoded dark-mode-only colors remain in any screen composable.
 * Audits for forbidden literal dark tokens and hex codes bypassing the theme system.
 */
class ThemeHardcodedColorAuditTest {

    private val forbiddenTokens = listOf(
        "PixelBackgroundDark",
        "PixelSurfaceDark",
        "PixelSurfaceVariantDark",
        "0xFF12121E",
        "0xFF1A1A2E",
        "0xFF252538",
        "0xFF0D0D15"
    )

    @Test
    fun screenComposables_haveNoHardcodedDarkTokens() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val screensDir = File(projectDir, "src/main/java/com/pixelquest/app/ui/screens")
        val candidateDir = if (screensDir.exists()) screensDir else File(projectDir, "app/src/main/java/com/pixelquest/app/ui/screens")

        assertTrue("Screens directory must exist: ${candidateDir.absolutePath}", candidateDir.exists())

        val violations = mutableListOf<String>()

        candidateDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.name.endsWith("Preview.kt") && !it.name.endsWith("Previews.kt") }
            .forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    // Allow comment lines documenting audits or migrations
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        for (token in forbiddenTokens) {
                            if (line.contains(token)) {
                                violations.add("${file.name}:${index + 1} contains forbidden dark token '$token' -> $trimmed")
                            }
                        }
                    }
                }
            }

        assertTrue(
            "Found ${violations.size} hardcoded dark token violations in screen composables:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
