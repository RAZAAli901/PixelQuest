package com.pixelquest.app.integration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The repository is public: the build script must not carry passwords or keys of its own. */
class BuildScriptSecretsTest {

    private val script = (File("build.gradle.kts").takeIf { it.exists() } ?: File("app/build.gradle.kts")).readText()

    @Test
    fun signingPasswords_comeFromTheEnvironmentOrLocalProperties_withNoDefault() {
        assertFalse("No default keystore password", script.contains("pixelquest123"))
        assertFalse(Regex("""storePassword\s*=\s*"""").containsMatchIn(script))
        assertFalse(Regex("""keyPassword\s*=\s*"""").containsMatchIn(script))
        assertTrue(script.contains("secret(\"KEYSTORE_PASSWORD\")"))
        assertTrue(script.contains("secret(\"KEY_PASSWORD\")"))
    }
}
