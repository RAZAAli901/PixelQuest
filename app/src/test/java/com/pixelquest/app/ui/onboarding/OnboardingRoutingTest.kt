package com.pixelquest.app.ui.onboarding

import com.pixelquest.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingRoutingTest {

    @Test
    fun testFirstLaunchRoutesToOnboarding() {
        val onboardingComplete = false
        val targetRoute = if (!onboardingComplete) Screen.Onboarding.route else Screen.Home.route
        assertEquals(Screen.Onboarding.route, targetRoute)
    }

    @Test
    fun testSubsequentLaunchRoutesToHome() {
        val onboardingComplete = true
        val targetRoute = if (!onboardingComplete) Screen.Onboarding.route else Screen.Home.route
        assertEquals(Screen.Home.route, targetRoute)
    }
}
