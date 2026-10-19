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

    private fun rpcBody(function: String) = bodyOf(requestTo("/rest/v1/rpc/$function")).toString()

    @Test
    fun creatingAParty_sendsItsRules_andReadsItsId() = runBlocking {
        answers["/rest/v1/rpc/create_party"] = ok("\"e33a08c7-d2b7-4848-a83d-ba292e7d1a21\"")

        val id = source.createParty("  Friday circle ", 12, 100_000)

        assertEquals(SupabaseResult.Success("e33a08c7-d2b7-4848-a83d-ba292e7d1a21"), id)
        assertEquals("""{"name":"Friday circle","max_members":12,"milestone":100000}""", rpcBody("create_party"))
    }

    @Test
    fun theOtherCalls_sendWhatTheirFunctionsTake() = runBlocking {
        answers["/rest/v1/rpc/invite_member"] = ok("""{"status":"already_member"}""")
        answers["/rest/v1/rpc/respond_invite"] = ok("""{"status":"active"}""")
        answers["/rest/v1/rpc/leave_party"] = ok("""{"status":"left"}""")
        answers["/rest/v1/rpc/remove_member"] = ok("""{"status":"removed"}""")
        answers["/rest/v1/rpc/set_milestone"] = ok("""{"milestone":5000,"milestone_set_at":"2026-10-19T10:00:00+00:00"}""")

        assertEquals(SupabaseResult.Success(InviteOutcome.ALREADY_MEMBER), source.invite("p1", "aria_k"))
        assertEquals(SupabaseResult.Success(MemberStatus.ACTIVE), source.respondToInvite("p1", accept = true))
        assertEquals(SupabaseResult.Success(Unit), source.leaveParty("p1"))
        assertEquals(SupabaseResult.Success(Unit), source.removeMember("p1", "u2"))
        assertEquals(SupabaseResult.Success(5000L), source.setMilestone("p1", 5000))

        assertEquals("""{"party_id":"p1","username":"aria_k"}""", rpcBody("invite_member"))
        assertEquals("""{"party_id":"p1","accept":true}""", rpcBody("respond_invite"))
        assertEquals("""{"party_id":"p1"}""", rpcBody("leave_party"))
        assertEquals("""{"party_id":"p1","user_id":"u2"}""", rpcBody("remove_member"))
        assertEquals("""{"party_id":"p1","target":5000}""", rpcBody("set_milestone"))
    }

    @Test
    fun rename_patchesTheName_andNoRowChangedIsNotLeader() = runBlocking {
        // Row-level security: anyone but the leader changes no row, and gets no error.
        val refused = source.renameParty("p1", "  Saturday circle ")
        assertEquals(com.pixelquest.app.darood.domain.DaroodRefusal.NOT_LEADER, com.pixelquest.app.darood.domain.DaroodRefusal.of(refused))
        val patch = requestTo("/rest/v1/parties")
        assertEquals("PATCH", patch.method.value)
        assertEquals("eq.p1", query(patch, "id"))
        assertEquals("""{"name":"Saturday circle"}""", bodyOf(patch).toString())
        assertTrue(patch.headers["Prefer"].orEmpty().contains("return=representation"))

        requests.clear()
        answers["/rest/v1/parties"] = ok("""[{"id":"p1"}]""")
        assertEquals(SupabaseResult.Success(Unit), source.renameParty("p1", "Saturday circle"))
    }

    @Test
    fun handingOver_callsTransferLeadership() = runBlocking {
        answers["/rest/v1/rpc/transfer_leadership"] = ok("""{"leader_id":"u2"}""")
        assertEquals(SupabaseResult.Success(Unit), source.transferLeadership("p1", "u2"))
        assertEquals("""{"party_id":"p1","user_id":"u2"}""", rpcBody("transfer_leadership"))

        answers["/rest/v1/rpc/transfer_leadership"] = HttpStatusCode.BadRequest to """{"code":"23514","details":null,"hint":null,"message":"not_a_member"}"""
        assertEquals(com.pixelquest.app.darood.domain.DaroodRefusal.NOT_A_MEMBER, com.pixelquest.app.darood.domain.DaroodRefusal.of(source.transferLeadership("p1", "u3")))
    }

    @Test
    fun theServersRefusals_areReadByName() = runBlocking {
        answers["/rest/v1/rpc/invite_member"] = HttpStatusCode.BadRequest to
            """{"code":"23514","details":"This party holds 2 people, invitations included.","hint":null,"message":"party_full"}"""
        answers["/rest/v1/rpc/set_milestone"] = HttpStatusCode.Forbidden to """{"code":"42501","details":null,"hint":null,"message":"not_leader"}"""
        answers["/rest/v1/rpc/create_party"] = HttpStatusCode.BadRequest to
            """{"code":"23514","details":null,"hint":null,"message":"new row for relation \"parties\" violates check constraint \"party_name_length\""}"""

        assertEquals(com.pixelquest.app.darood.domain.DaroodRefusal.PARTY_FULL, com.pixelquest.app.darood.domain.DaroodRefusal.of(source.invite("p1", "aria_k")))
        assertEquals(com.pixelquest.app.darood.domain.DaroodRefusal.NOT_LEADER, com.pixelquest.app.darood.domain.DaroodRefusal.of(source.setMilestone("p1", 10)))
        assertEquals(com.pixelquest.app.darood.domain.DaroodRefusal.INVALID, com.pixelquest.app.darood.domain.DaroodRefusal.of(source.createParty("x", 10, null)))
        assertEquals(null, com.pixelquest.app.darood.domain.DaroodRefusal.of(SupabaseResult.NetworkError(java.io.IOException("offline"))))
    }

    @Test
    fun anEntry_isSentWithItsDay_andComesBackAsStored() = runBlocking {
        answers["/rest/v1/rpc/add_darood_entry"] = ok(
            """{"id":42,"party_id":"p1","user_id":"me","amount":300,"local_date":"2026-10-19","tz":"Asia/Karachi","created_at":"2026-10-19T15:00:00+00:00","client_id":"c-1"}"""
        )

        val entry = (source.addEntry("p1", 300, java.time.LocalDate.of(2026, 10, 19), "Asia/Karachi", "c-1") as SupabaseResult.Success).data

        assertEquals(42L, entry.id)
        assertEquals("me", entry.userId)
        assertEquals("c-1", entry.clientId)
        assertEquals(
            """{"party_id":"p1","amount":300,"local_date":"2026-10-19","tz":"Asia/Karachi","client_id":"c-1"}""",
            bodyOf(requestTo("/rest/v1/rpc/add_darood_entry")).toString()
        )
    }

    @Test
    fun entries_comeNewestFirst_fiftyAPage_andOlderPagesGoBackFromAnId() = runBlocking {
        answers["/rest/v1/darood_entries"] = ok(
            """[{"id":9,"party_id":"p1","user_id":"u2","amount":33,"local_date":"2026-10-19","tz":"UTC","created_at":"2026-10-19T10:00:00+00:00"},
                {"id":8,"party_id":"p1","user_id":null,"amount":11,"local_date":"2026-10-18","tz":"UTC","created_at":"2026-10-18T10:00:00+00:00"}]"""
        )

        val newest = (source.fetchEntries("p1") as SupabaseResult.Success).data
        source.fetchEntries("p1", beforeId = 8)

        assertEquals(listOf(9L, 8L), newest.map { it.id })
        val (first, older) = requests
        assertEquals("eq.p1", query(first, "party_id"))
        assertEquals("id.desc.nullslast", query(first, "order"))
        assertEquals("50", query(first, "limit"))
        assertEquals(null, query(first, "id"))
        assertEquals("lt.8", query(older, "id"))
    }

    @Test
    fun stats_forSeveralParties_inOneRequest() = runBlocking {
        answers["/rest/v1/party_stats"] = ok(
            """[{"party_id":"p1","total":663,"entry_count":4,"member_count":2,"last_entry_date":"2026-10-19"}]"""
        )

        val stats = (source.fetchStats(listOf("p1", "p2")) as SupabaseResult.Success).data

        assertEquals(663L, stats.single().total)
        assertEquals("in.(p1,p2)", query(requestTo("/rest/v1/party_stats"), "party_id"))
        assertEquals(SupabaseResult.Success(emptyList<com.pixelquest.app.darood.domain.PartyStats>()), source.fetchStats(emptyList()))
        assertEquals("no ids, no request", 1, requests.size)
    }

    @Test
    fun memberStats_largestFirst() = runBlocking {
        answers["/rest/v1/party_member_stats"] = ok(
            """[{"party_id":"p1","user_id":"u1","username":"aria_k","total":350,"active_days":2,"last_entry_date":"2026-10-19"},
                {"party_id":"p1","user_id":null,"username":null,"total":50,"active_days":1,"last_entry_date":"2026-10-17"}]"""
        )

        val members = (source.fetchMemberStats("p1") as SupabaseResult.Success).data

        assertEquals(listOf(350L, 50L), members.map { it.total })
        val request = requestTo("/rest/v1/party_member_stats")
        assertEquals("eq.p1", query(request, "party_id"))
        assertEquals("total.desc.nullslast", query(request, "order"))
    }
}
