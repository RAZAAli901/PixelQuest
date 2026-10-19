package com.pixelquest.app.darood.data

import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.data.remote.SupabaseResult
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The username calls, against a fake server: what's sent, and how the answer comes back. */
class SupabaseUsernameRepositoryTest {

    private val requests = mutableListOf<HttpRequestData>()
    private var answer = "true"
    private var status = HttpStatusCode.OK

    private val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
        httpEngine = MockEngine { request ->
            requests += request
            respond(answer, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        install(Postgrest)
    }
    private val repository = SupabaseUsernameRepository(supabase.postgrest)

    private fun bodyOf(request: HttpRequestData) = Json.parseToJsonElement(
        when (val body = request.body) {
            is TextContent -> body.text
            is OutgoingContent.ByteArrayContent -> String(body.bytes())
            else -> error("unexpected body $body")
        }
    ).jsonObject

    @Test
    fun availability_asksTheServerFunction_withTheStoredForm() = runBlocking {
        val result = repository.isUsernameAvailable(Username.parse("@Aria_K")!!)

        assertEquals(SupabaseResult.Success(true), result)
        val request = requests.single()
        assertTrue(request.url.encodedPath, request.url.encodedPath.endsWith("/rest/v1/rpc/is_username_available"))
        assertEquals("aria_k", bodyOf(request)["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun aTakenName_comesBackFalse() = runBlocking {
        answer = "false"

        assertEquals(SupabaseResult.Success(false), repository.isUsernameAvailable(Username.parse("aria_k")!!))
    }

    @Test
    fun aRefusal_isAFailure_notAnAnswer() = runBlocking {
        status = HttpStatusCode.Unauthorized
        answer = """{"code":"42501","message":"Not authenticated"}"""

        assertTrue(repository.isUsernameAvailable(Username.parse("aria_k")!!) !is SupabaseResult.Success)
    }
}
