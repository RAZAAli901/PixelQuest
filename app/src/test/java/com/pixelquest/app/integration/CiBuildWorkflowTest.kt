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
}
