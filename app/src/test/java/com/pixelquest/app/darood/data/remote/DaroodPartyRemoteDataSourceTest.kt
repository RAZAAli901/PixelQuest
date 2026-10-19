package com.pixelquest.app.darood.data.remote

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.domain.MemberStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Darood Party calls against a fake Supabase: what each sends, and how the answers are read. */
class DaroodPartyRemoteDataSourceTest {

    private val requests = mutableListOf<HttpRequestData>()

    /** The answer for a path ending, e.g. "/rest/v1/parties"; the default is an empty list. */
    private val answers = mutableMapOf<String, Pair<HttpStatusCode, String>>()
    private var signedIn: String? = "me"

    private val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
        httpEngine = MockEngine { request ->
            requests += request
            val (status, body) = answers.entries.firstOrNull { request.url.encodedPath.endsWith(it.key) }?.value ?: (HttpStatusCode.OK to "[]")
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        install(Postgrest)
    }
    private val source = SupabaseDaroodPartyRemoteDataSource(supabase.postgrest) { signedIn }

    private fun ok(body: String) = HttpStatusCode.OK to body

    private fun requestTo(path: String) = requests.single { it.url.encodedPath.endsWith(path) }

    private fun query(request: HttpRequestData, name: String) = request.url.parameters[name]

    internal fun bodyOf(request: HttpRequestData): JsonElement = Json.parseToJsonElement(
        when (val body = request.body) {
            is TextContent -> body.text
            is OutgoingContent.ByteArrayContent -> String(body.bytes())
            else -> error("unexpected body $body")
        }
    )

    @Test
    fun myParties_areMyActiveAndInvitedMemberships_withTheirParties() = runBlocking {
        answers["/rest/v1/party_members"] = ok(
            """[{"party_id":"p1","user_id":"me","role":"leader","status":"active","invited_at":null,"joined_at":"2026-10-19T09:00:00+00:00"},
                {"party_id":"p2","user_id":"me","role":"member","status":"invited","invited_at":"2026-10-19T09:00:00+00:00","joined_at":null},
                {"party_id":"gone","user_id":"me","role":"member","status":"active","invited_at":null,"joined_at":null}]"""
        )
        answers["/rest/v1/parties"] = ok(
            """[{"id":"p1","name":"Friday circle","leader_id":"me","max_members":10,"milestone":null,"milestone_set_at":null,"completed_at":null,"created_at":"2026-10-19T09:00:00+00:00"},
                {"id":"p2","name":"Family","leader_id":"aunt","max_members":5,"milestone":1000,"milestone_set_at":null,"completed_at":null,"created_at":"2026-10-18T09:00:00+00:00"}]"""
        )

        val result = source.fetchMyParties()

        val parties = (result as SupabaseResult.Success).data
        assertEquals(listOf("p1" to "active", "p2" to "invited"), parties.map { it.party.id to it.membership.status })
        val members = requestTo("/rest/v1/party_members")
        assertEquals("eq.me", query(members, "user_id"))
        assertEquals("in.(active,invited)", query(members, "status"))
        assertEquals("in.(p1,p2,gone)", query(requestTo("/rest/v1/parties"), "id"))
    }

    @Test
    fun noMemberships_meansNoPartyQuery() = runBlocking {
        val result = source.fetchMyParties()

        assertEquals(SupabaseResult.Success(emptyList<RemoteParty>()), result)
        assertEquals(1, requests.size)
    }

    @Test
    fun members_comeWithTheUsernamesICanSee() = runBlocking {
        answers["/rest/v1/party_members"] = ok(
            """[{"party_id":"p1","user_id":"me","role":"leader","status":"active","invited_at":null,"joined_at":null},
                {"party_id":"p1","user_id":"u2","role":"member","status":"left","invited_at":null,"joined_at":null}]"""
        )
        answers["/rest/v1/usernames"] = ok("""[{"user_id":"me","username":"aria_k"}]""")

        val members = (source.fetchMembers("p1") as SupabaseResult.Success).data

        assertEquals(listOf("aria_k", null), members.map { it.username })
        assertEquals(MemberStatus.LEFT, members[1].status)
        assertEquals("eq.p1", query(requestTo("/rest/v1/party_members"), "party_id"))
        assertEquals("in.(me,u2)", query(requestTo("/rest/v1/usernames"), "user_id"))
    }

    @Test
    fun signedOut_nothingIsSent() = runBlocking {
        signedIn = null

        assertTrue(source.fetchMyParties() is SupabaseResult.AuthError)
        assertTrue(source.fetchMembers("p1") is SupabaseResult.AuthError)
        assertTrue(requests.isEmpty())
    }
}
