package com.pixelquest.app.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ZeroFlakinessPolicyTest {

    @Test
    fun zeroFlakinessPolicy_detectsThreadSleep() {
        val dummyCode = """
            class SampleTest {
                fun testSomething() {
                    Thread.sleep(100)
                }
            }
        """.trimIndent()

        val violations = ZeroFlakinessPolicy.scanContent(File("SampleTest.kt"), dummyCode)
        assertEquals(1, violations.size)
        assertEquals(FlakinessViolationType.THREAD_SLEEP, violations.first().type)
        assertEquals(3, violations.first().line)
    }

    @Test
    fun zeroFlakinessPolicy_detectsRealNetwork() {
        val dummyCode = """
            class SampleNetworkTest {
                fun testSomething() {
                    val socket = java.net.Socket("127.0.0.1", 8080)
                }
            }
        """.trimIndent()

        val violations = ZeroFlakinessPolicy.scanContent(File("SampleNetworkTest.kt"), dummyCode)
        assertEquals(1, violations.size)
        assertEquals(FlakinessViolationType.REAL_NETWORK, violations.first().type)
        assertEquals(3, violations.first().line)
    }

    @Test
    fun zeroFlakinessPolicy_ignoresComments() {
        val dummyCode = """
            // Thread.sleep(100)
            /* java.net.Socket() */
            class SampleTest
        """.trimIndent()

        val violations = ZeroFlakinessPolicy.scanContent(File("SampleTest.kt"), dummyCode)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun zeroFlakinessPolicy_enforcesNoSleepOrRealNetworkInNewTests() {
        val projectRoot = File(".").canonicalFile
        val testDirs = listOf(
            File(projectRoot, "src/test"),
            File(projectRoot, "src/androidTest"),
            File(projectRoot, "src/sharedTest"),
            File(projectRoot, "app/src/test"),
            File(projectRoot, "app/src/androidTest"),
            File(projectRoot, "app/src/sharedTest"),
        ).filter { it.exists() }

        val allViolations = testDirs.flatMap { dir ->
            ZeroFlakinessPolicy.scanDirectory(dir, excludeLegacyAllowlist = true)
        }

        val violationSummary = allViolations.joinToString("\n") {
            "  ${it.file.name}:${it.line} - ${it.message}: ${it.lineContent}"
        }

        assertTrue(
            "Found ${allViolations.size} flakiness violation(s) in tests! Zero-flakiness policy prohibits Thread.sleep, real delays, and real network calls in tests:\n$violationSummary",
            allViolations.isEmpty(),
        )
    }
}
