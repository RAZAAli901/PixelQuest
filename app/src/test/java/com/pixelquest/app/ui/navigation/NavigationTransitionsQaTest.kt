package com.pixelquest.app.ui.navigation

import org.junit.Assert.assertNotNull
import org.junit.Test

class NavigationTransitionsQaTest {

    @Test
    fun verifyPixelTransitions_specCompleteness() {
        assertNotNull(PixelTransitions.Enter)
        assertNotNull(PixelTransitions.Exit)
        assertNotNull(PixelTransitions.PopEnter)
        assertNotNull(PixelTransitions.PopExit)
        assertNotNull(PixelTransitions.ModalEnter)
        assertNotNull(PixelTransitions.ModalExit)
        assertNotNull(PixelTransitions.ModalPopEnter)
        assertNotNull(PixelTransitions.ModalPopExit)
        assertNotNull(PixelTransitions.LevelUpEnter)
        assertNotNull(PixelTransitions.LevelUpExit)
    }

    @Test
    fun verifyNavHostRoutes_allRoutesDefined() {
        val routes = listOf(
            Screen.Splash.route,
            Screen.Home.route,
            Screen.Tasks.route,
            Screen.Stats.route,
            Screen.Profile.route,
            Screen.AvatarSelection.route,
            Screen.DifficultySelection.route,
            Screen.LevelHistory.route,
            Screen.TaskHistory.route,
            Screen.CreateTask.route,
            Screen.Onboarding.route,
            Screen.Settings.route
        )
        routes.forEach { route ->
            assertNotNull(route)
        }
    }
}
