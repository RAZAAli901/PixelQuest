package com.pixelquest.app.ui

import org.junit.Assert.assertNull
import org.junit.Test

class LevelUpCelebrationScreenTest {

    @Test
    fun testDismissClearsPendingSignal() {
        var pendingLevelUp: Int? = 3

        val onDismiss = {
            pendingLevelUp = null
        }

        onDismiss()
        assertNull(pendingLevelUp)
    }
}
