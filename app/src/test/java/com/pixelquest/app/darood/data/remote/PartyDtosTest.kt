package com.pixelquest.app.darood.data.remote

import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** The JSON Supabase sends for Darood Parties (as seen on the local stack) reads into the domain models. */
class PartyDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aParty_withItsStats() {
        val party = json.decodeFromString<PartyDto>(
            """{"id":"e33a08c7-d2b7-4848-a83d-ba292e7d1a21","name":"Friday circle","leader_id":"4616e63d","max_members":10,
               "milestone":100000,"milestone_set_at":"2026-10-19T10:00:00.123456+00:00","completed_at":null,
               "created_at":"2026-10-19T09:59:59+00:00","deleted_at":null}"""
        )
        val stats = json.decodeFromString<PartyStatsDto>(
            """{"party_id":"e33a08c7-d2b7-4848-a83d-ba292e7d1a21","name":"Friday circle","total":663,"entry_count":4,"member_count":2,"last_entry_date":"2026-10-19"}"""
        )

        val domain = party.toDomain(stats, MemberStatus.ACTIVE)

        assertEquals("Friday circle", domain.name)
        assertEquals(Instant.parse("2026-10-19T10:00:00.123456Z"), domain.milestoneSetAt)
        assertNull(domain.completedAt)
        assertEquals(663L, domain.total)
        assertEquals(2, domain.memberCount)
        assertEquals(0.00663f, domain.progress!!, 0.00001f)
        assertEquals(LocalDate.of(2026, 10, 19), stats.toDomain().lastEntryDate)
    }

    @Test
    fun aMember_anEntry_andMemberStats() {
        val member = json.decodeFromString<PartyMemberDto>(
            """{"id":7,"party_id":"p1","user_id":"u1","role":"leader","status":"active","invited_by":"u1",
               "invited_at":"2026-10-19T09:59:59+00:00","joined_at":"2026-10-19T09:59:59+00:00","updated_at":"2026-10-19T09:59:59+00:00"}"""
        ).toDomain(username = "aria_k")
        assertEquals(PartyRole.LEADER, member.role)
        assertEquals(MemberStatus.ACTIVE, member.status)
        assertEquals("aria_k", member.username)

        val entry = json.decodeFromString<DaroodEntryDto>(
            """{"id":42,"party_id":"p1","user_id":"u1","amount":313,"local_date":"2026-10-18","tz":"Asia/Karachi","created_at":"2026-10-18T20:00:00+05:00"}"""
        ).toDomain()
        assertEquals(42L, entry.id)
        assertEquals(313, entry.amount)
        assertEquals(LocalDate.of(2026, 10, 18), entry.localDate)
        assertEquals(Instant.parse("2026-10-18T15:00:00Z"), entry.createdAt)
        assertEquals(EntryState.SYNCED, entry.state)

        val gone = json.decodeFromString<MemberStatsDto>(
            """{"party_id":"p1","user_id":null,"username":null,"role":null,"status":null,"total":50,"active_days":1,"last_entry_date":"2026-10-17"}"""
        ).toDomain()
        assertNull("an account since deleted", gone.userId)
        assertEquals(50L, gone.total)
    }

    @Test
    fun everyStatus_readsAsItsOwn() {
        assertEquals(
            listOf(MemberStatus.INVITED, MemberStatus.ACTIVE, MemberStatus.DECLINED, MemberStatus.LEFT, MemberStatus.REMOVED),
            listOf("invited", "active", "declined", "left", "removed").map(::statusOf)
        )
    }

    @Test
    fun aNewEntry_sendsOnlyWhatAMemberMay() {
        val sent = Json.encodeToString(NewDaroodEntryDto.serializer(), NewDaroodEntryDto("p1", 300, "2026-10-19", "Asia/Karachi", "c-1"))
        assertEquals("""{"party_id":"p1","amount":300,"local_date":"2026-10-19","tz":"Asia/Karachi","client_id":"c-1"}""", sent)
    }
}
