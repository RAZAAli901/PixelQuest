package com.pixelquest.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {

    @Test
    fun taskAnalytics_routeAndArgs() {
        assertEquals("task_analytics/42", Screen.TaskAnalytics.createRoute(42L))
        assertEquals(1, Screen.TaskAnalytics.arguments.size)
        assertEquals(Screen.TaskAnalytics.ARG_TASK_ID, Screen.TaskAnalytics.arguments[0].name)

        val handle = SavedStateHandle(mapOf(Screen.TaskAnalytics.ARG_TASK_ID to 42L))
        assertEquals(42L, Screen.TaskAnalytics.getTaskId(handle))
    }

    @Test
    fun editTask_routeAndArgs() {
        assertEquals("edit_task/105", Screen.EditTask.createRoute(105L))
        assertEquals(1, Screen.EditTask.arguments.size)
        assertEquals(Screen.EditTask.ARG_TASK_ID, Screen.EditTask.arguments[0].name)

        val handle = SavedStateHandle(mapOf(Screen.EditTask.ARG_TASK_ID to 105L))
        assertEquals(105L, Screen.EditTask.getTaskId(handle))
    }

    @Test
    fun partiesParty_routeAndArgs() {
        assertEquals("parties/party/guild-123", Screen.PartiesParty.createRoute("guild-123"))
        val handle = SavedStateHandle(mapOf(Screen.PartiesParty.ARG_PARTY_ID to "guild-123"))
        assertEquals("guild-123", Screen.PartiesParty.getPartyId(handle))
    }

    @Test
    fun partiesItem_routeAndArgs() {
        assertEquals("parties/party/p1/item/item-99", Screen.PartiesItem.createRoute("p1", "item-99"))
        val handle = SavedStateHandle(
            mapOf(
                Screen.PartiesItem.ARG_PARTY_ID to "p1",
                Screen.PartiesItem.ARG_ITEM_ID to "item-99",
            ),
        )
        val args = Screen.PartiesItem.parseArgs(handle)
        assertEquals("p1", args.partyId)
        assertEquals("item-99", args.itemId)
        assertEquals("item-99", Screen.PartiesItem.getItemId(handle))
    }

    @Test
    fun chessRoutes_routeAndArgs() {
        assertEquals("chess/challenge/c-1", Screen.ChessChallenge.createRoute("c-1"))
        assertEquals("chess/game/g-2", Screen.ChessGame.createRoute("g-2"))

        val handleGame = SavedStateHandle(mapOf(Screen.ChessGame.ARG_GAME_ID to "g-2"))
        assertEquals("g-2", Screen.ChessGame.getGameId(handleGame))

        val handleH2h = SavedStateHandle(
            mapOf(
                Screen.ChessHeadToHead.ARG_USER_ID to "u-1",
                Screen.ChessHeadToHead.ARG_NAME to "Raza",
            ),
        )
        val h2hArgs = Screen.ChessHeadToHead.parseArgs(handleH2h)
        assertEquals("u-1", h2hArgs.userId)
        assertEquals("Raza", h2hArgs.name)
    }

    @Test
    fun quranSurah_routeAndArgs() {
        assertEquals("islamic/quran/surah/2?ayah=255", Screen.QuranSurah.createRoute(2, 255))
        val handle = SavedStateHandle(
            mapOf(
                Screen.QuranSurah.ARG_SURAH to 2,
                Screen.QuranSurah.ARG_AYAH to 255,
            ),
        )
        val args = Screen.QuranSurah.parseArgs(handle)
        assertEquals(2, args.surah)
        assertEquals(255, args.ayah)
    }

    @Test
    fun routes_helperPredicates() {
        assertTrue(Routes.isParties("parties"))
        assertTrue(Routes.isParties("parties/create"))
        assertFalse(Routes.isParties("home"))

        assertTrue(Routes.isIslamic("islamic/hub"))
        assertTrue(Routes.isIslamic("islamic/quran"))
        assertFalse(Routes.isIslamic("settings"))

        assertTrue(Routes.isFullScreen(Screen.Splash.route))
        assertTrue(Routes.isFullScreen(Screen.Onboarding.route))
        assertTrue(Routes.isFullScreen(Screen.QuranSurah.route))
        assertFalse(Routes.isFullScreen(Screen.Home.route))
    }
}
