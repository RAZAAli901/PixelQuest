package com.pixelquest.app.darood.data.remote

import com.pixelquest.app.darood.domain.RaceLine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the app reads and keeps of the race board: the view's six columns and whether the line is
 * the player's own, and nothing that could name a member or hold an entry. The server's side is
 * checked in supabase/tests/local/darood_parties.test.ts ("privacy").
 */
class RaceBoardPrivacyTest {

    private val allowed = setOf("name", "member_count", "total", "milestone", "progress", "completed_at", "mine")

    @Test
    fun theAppReadsOnlyTheBoardsOwnColumns() {
        assertEquals(allowed, (0 until RaceLineDto.serializer().descriptor.elementsCount).map { RaceLineDto.serializer().descriptor.getElementName(it) }.toSet())
    }

    @Test
    fun aRaceLine_holdsNoMemberNorId() {
        val fields = RaceLine::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }.toSet()
        assertEquals(setOf("name", "memberCount", "total", "milestone", "progress", "completedAt", "mine"), fields)
    }
}
