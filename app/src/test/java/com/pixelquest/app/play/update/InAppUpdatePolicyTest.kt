package com.pixelquest.app.play.update

import org.junit.Assert.assertEquals
import org.junit.Test

class InAppUpdatePolicyTest {
    @Test
    fun normalReleaseDoesNotInterruptPlayer() {
        assertEquals(InAppUpdatePolicy.Type.NONE, InAppUpdatePolicy.chooseType(0, 0, true, true))
    }

    @Test
    fun staleOrImportantReleaseUsesFlexibleUpdate() {
        assertEquals(InAppUpdatePolicy.Type.FLEXIBLE, InAppUpdatePolicy.chooseType(3, 0, true, true))
        assertEquals(InAppUpdatePolicy.Type.FLEXIBLE, InAppUpdatePolicy.chooseType(0, 7, true, true))
    }

    @Test
    fun urgentReleaseUsesImmediateWhenAllowed() {
        assertEquals(InAppUpdatePolicy.Type.IMMEDIATE, InAppUpdatePolicy.chooseType(4, 0, true, true))
        assertEquals(InAppUpdatePolicy.Type.FLEXIBLE, InAppUpdatePolicy.chooseType(4, 0, true, false))
    }
}
