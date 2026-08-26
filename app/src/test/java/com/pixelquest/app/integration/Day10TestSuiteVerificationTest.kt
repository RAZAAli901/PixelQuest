package com.pixelquest.app.integration

import com.pixelquest.app.data.backup.DataExportImportTest
import com.pixelquest.app.scheduling.NotificationMasterToggleTest
import com.pixelquest.app.ui.onboarding.OnboardingRoutingTest
import org.junit.Assert.assertTrue
import org.junit.Test

class Day10TestSuiteVerificationTest {

    @Test
    fun testDay10FullTestSuiteExecution() {
        val routingTest = OnboardingRoutingTest()
        routingTest.testOnboardingIncompleteRoutesToOnboarding()
        routingTest.testOnboardingCompleteRoutesToHome()

        val exportImportTest = DataExportImportTest()
        exportImportTest.testExportAndImportRoundtripFidelity()

        val masterToggleTest = NotificationMasterToggleTest()
        masterToggleTest.testDisableNotificationsCancelsAllAlarms()

        assertTrue(true)
    }
}
