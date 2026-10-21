package com.pixelquest.app.push

import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.notification.NotificationChannels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A chess push becomes our own text and a safe route, or nothing (J123). */
class ChessPushNotifierTest {
    private val id = "6f1c2d3e-4a5b-4c6d-8e9f-0a1b2c3d4e5f"

    @Test
    fun eachKind_opensItsScreen_withOurOwnText() {
        val challenge = ChessPushNotifier.shownFor(mapOf("kind" to "challenge_received", "challenge_id" to id, "party_id" to id))!!
        assertEquals(R.string.chess_push_challenge_title, challenge.title)
        assertEquals("chess/challenge/$id", challenge.route)
        assertEquals("chess/game/$id", ChessPushNotifier.shownFor(mapOf("kind" to "challenge_accepted", "game_id" to id))!!.route)
        assertEquals(R.string.chess_push_draw_text, ChessPushNotifier.shownFor(mapOf("kind" to "draw_offered", "game_id" to id))!!.text)
        assertTrue(MainActivity.isOpenable(challenge.route))
    }

    @Test
    fun anythingElse_isDropped() {
        assertNull(ChessPushNotifier.shownFor(mapOf("kind" to "challenge_received", "challenge_id" to "../settings")))
        assertNull(ChessPushNotifier.shownFor(mapOf("kind" to "hello", "game_id" to id)))
        assertNull(ChessPushNotifier.shownFor(emptyMap()))
        assertTrue(!MainActivity.isOpenable("chess/game/$id/x"))
    }

    @Test
    fun itsChannel_isOneThePlayerCanTune() {
        assertTrue(NotificationChannels.Spec.CHESS in NotificationChannels.userFacing)
    }
}
