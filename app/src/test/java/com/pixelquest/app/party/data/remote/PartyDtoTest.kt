package com.pixelquest.app.party.data.remote

import com.pixelquest.app.darood.data.remote.toDomain
import com.pixelquest.app.party.domain.PartyType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A party's row, as the app's own Supabase client decodes it. The app reads every column of a party
 * (so it gets party_type where the server has it): the row may carry columns the app doesn't use, and
 * a server before 20261019000031 has no party_type at all.
 */
class PartyDtoTest {

    private val client = createSupabaseClient("https://placeholder-project.supabase.co", "placeholder-anon-key") { install(Postgrest) }

    private fun decode(json: String): PartyDto = client.defaultSerializer.decode(kotlin.reflect.typeOf<PartyDto>(), json)

    private val row = """"id":"p","name":"Physics group","leader_id":"l","max_members":6,"milestone":null,"milestone_set_at":null,"completed_at":null,"created_at":"2026-10-21T09:00:00+00:00","is_public_race":false"""

    @Test
    fun theClientIgnoresColumnsTheAppDoesntUse() {
        assertTrue(client.defaultSerializer is KotlinXSerializer)
        val party = decode("""{$row,"party_type":"study","deleted_at":null,"something_new":1}""")
        assertEquals("study", party.partyType)
        assertEquals(PartyType.STUDY, party.toDomain().type)
    }

    @Test
    fun aServerWithoutKinds_readsAsDaroodParties() {
        val party = decode("{$row}")
        assertEquals("darood", party.partyType)
        assertEquals(PartyType.DAROOD, party.toDomain().type)
    }
}
