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
}
