package com.pixelquest.app.testing

import android.app.Application
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CoverageRulesConfigTest {

    @Test
    fun coverageRulesConfig_containsExpectedThresholdsPerLayer() {
        val rootDir = File(".").canonicalFile
        val configFile = listOf(
            File(rootDir, "config/coverage-rules.json"),
            File(rootDir, "../config/coverage-rules.json"),
        ).firstOrNull { it.exists() }

        assertNotNull("config/coverage-rules.json must exist in project root", configFile)
        val json = JSONObject(configFile!!.readText())
        assertTrue("Config must contain 'layers' object", json.has("layers"))

        val layers = json.getJSONObject("layers")

        // Domain layer: 80%
        val domain = layers.getJSONObject("domain")
        assertEquals(80, domain.getInt("minLineCoverage"))
        assertEquals("com.pixelquest.app.domain.*", domain.getString("packagePattern"))

        // Data layer: 70%
        val data = layers.getJSONObject("data")
        assertEquals(70, data.getInt("minLineCoverage"))
        assertEquals("com.pixelquest.app.data.*", data.getString("packagePattern"))

        // ViewModels layer: 80%
        val viewmodels = layers.getJSONObject("viewmodels")
        assertEquals(80, viewmodels.getInt("minLineCoverage"))
        assertEquals("*ViewModel*", viewmodels.getString("classPattern"))

        // UI layer: 50%
        val ui = layers.getJSONObject("ui")
        assertEquals(50, ui.getInt("minLineCoverage"))
        assertEquals("com.pixelquest.app.ui.*", ui.getString("packagePattern"))
    }
}
