package com.pixelquest.app.integration

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CiBuildWorkflowTest {

    @Test
    fun verifyCiWorkflow_buildsCleanDebugApk() {
        val workflowFile = File("../.github/workflows/build.yml")
        val altWorkflowFile = File(".github/workflows/build.yml")

        val targetFile = if (workflowFile.exists()) workflowFile else altWorkflowFile
        assertTrue("build.yml workflow file must exist", targetFile.exists())

        val content = targetFile.readText()
        // The build step runs several tasks in one Gradle call (e.g. "./gradlew compileDebugKotlin assembleDebug ...").
        val gradleRuns = content.lines().filter { it.contains("./gradlew ") }
        assertTrue("Workflow must run assembleDebug", gradleRuns.any { it.contains(" assembleDebug") })
        assertTrue("Workflow must upload debug APK artifact", content.contains("pixelquest-debug-apk"))
    }

    @Test
    fun verifyCiWorkflow_runsTheUnitSuite_withoutLiveGemini() {
        val targetFile = listOf(File("../.github/workflows/build.yml"), File(".github/workflows/build.yml")).first { it.exists() }
        val content = targetFile.readText()

        val gradleRuns = content.lines().filter { it.contains("./gradlew ") }
        assertTrue("Workflow must run the unit tests", gradleRuns.any { it.contains(" testDebugUnitTest") })
        // Live Gemini calls spend the developer's quota; CI must never opt in (comments may mention it).
        val activeLines = content.lines().filterNot { it.trim().startsWith("#") }
        assertTrue("CI must not opt in to live Gemini calls", activeLines.none { it.contains("PIXELQUEST_LIVE_GEMINI") })
    }

    @Test
    fun noWorkflow_givesTheGeminiKeyToABuild_andCiApksUseTheProxy() {
        for (name in listOf("build.yml", "release.yml")) {
            val workflow = listOf(File("../.github/workflows/$name"), File(".github/workflows/$name")).first { it.exists() }
            val activeLines = workflow.readLines().filterNot { it.trim().startsWith("#") }

            assertTrue("$name must not read the Gemini key secret", activeLines.none { it.contains("GEMINI_API_KEY") })
            assertTrue("$name must build with the proxy", activeLines.any { it.contains("GEMINI_VIA_PROXY=true") })
        }
    }

    @Test
    fun releaseWorkflow_saysInTheNotesWhenSigningOrSupabaseSecretsAreMissing() {
        val workflow = listOf(File("../.github/workflows/release.yml"), File(".github/workflows/release.yml")).first { it.exists() }
        val content = workflow.readText()

        assertTrue(content.contains("name: Check Release Secrets"))
        assertTrue("A missing keystore is flagged", content.contains("-z \"${'$'}KEYSTORE_BASE64\""))
        assertTrue("Missing Supabase settings are flagged", content.contains("-z \"${'$'}SUPABASE_URL\""))
        assertTrue("The warnings lead the release notes", content.contains("cat RELEASE_WARNINGS.md > RELEASE_NOTES.md"))
    }

    @Test
    fun releaseWorkflow_failsUnlessSignedWithTheReleaseKey() {
        val workflow = listOf(File("../.github/workflows/release.yml"), File(".github/workflows/release.yml")).first { it.exists() }
        val content = workflow.readText()
        assertTrue(content.contains("name: Verify Release Signature"))
        assertTrue(content.contains("app/release-signing-cert.sha256"))
        // The check comes after the build and before anything is published.
        assertTrue(content.indexOf("name: Verify Release Signature") > content.indexOf("name: Build Debug & Release APKs"))
        assertTrue(content.indexOf("name: Verify Release Signature") < content.indexOf("name: Create GitHub Release"))

        val certFile = listOf(File("release-signing-cert.sha256"), File("app/release-signing-cert.sha256")).first { it.exists() }
        assertTrue("A lower-case SHA-256 hex digest", Regex("^[0-9a-f]{64}$").matches(certFile.readText().trim()))
    }

    @Test
    fun verifyCiWorkflow_testsTheEdgeFunctions() {
        val targetFile = listOf(File("../.github/workflows/build.yml"), File(".github/workflows/build.yml")).first { it.exists() }
        val activeLines = targetFile.readLines().filterNot { it.trim().startsWith("#") }

        assertTrue("CI must run the Edge Function tests", activeLines.any { it.contains("node --test") && it.contains("supabase/functions") })
    }
}
