package com.pixelquest.app.auth

import android.app.Application
import com.pixelquest.app.data.remote.SupabaseResult
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What the email-code sign-in actually sends to Supabase Auth: a code request that may create the
 * account, then the code with type "email", after which the session is the signed-in account.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EmailCodeRequestsTest {

    private val userId = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b"
    private val requests = mutableListOf<HttpRequestData>()
    private var verifyStatus = HttpStatusCode.OK

    private val session = """
        {"access_token":"access-1","token_type":"bearer","expires_in":3600,"refresh_token":"refresh-1",
         "user":{"id":"$userId","aud":"authenticated","role":"authenticated","email":"hero@pixelquest.test",
                 "app_metadata":{"provider":"email"},"user_metadata":{}}}
    """.trimIndent()

    private val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
        httpEngine = MockEngine { request ->
            requests += request
            val path = request.url.encodedPath
            when {
                path.endsWith("/otp") -> respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                path.endsWith("/verify") && verifyStatus == HttpStatusCode.OK ->
                    respond(session, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                path.endsWith("/verify") -> respond(
                    """{"code":403,"error_code":"otp_expired","msg":"Token has expired or is invalid"}""",
                    verifyStatus, headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("{}", HttpStatusCode.NotFound)
            }
        }
        install(Auth) {
            sessionManager = MemorySessionManager()
            codeVerifierCache = MemoryCodeVerifierCache()
            autoLoadFromStorage = false
            alwaysAutoRefresh = false
            enableLifecycleCallbacks = false
        }
        install(Postgrest)
    }
    private val repository = AuthRepositoryImpl(supabase.auth, supabase.postgrest)

    private fun bodyOf(request: HttpRequestData) = Json.parseToJsonElement(
        when (val body = request.body) {
            is TextContent -> body.text
            is OutgoingContent.ByteArrayContent -> String(body.bytes())
            else -> error("unexpected body $body")
        }
    ).jsonObject

    @Test
    fun askingForACode_postsTheAddress_andMayCreateTheAccount() = runBlocking {
        val result = repository.sendEmailCode("hero@pixelquest.test")

        assertEquals(SupabaseResult.Success(Unit), result)
        val request = requests.single()
        assertTrue(request.url.encodedPath.endsWith("/auth/v1/otp"))
        val body = bodyOf(request)
        assertEquals("hero@pixelquest.test", body["email"]!!.jsonPrimitive.content)
        assertEquals("true", body["create_user"]!!.jsonPrimitive.content)
    }

    @Test
    fun theCode_isVerifiedAsAnEmailCode_andSignsIn() = runBlocking {
        val result = repository.verifyEmailCode("hero@pixelquest.test", "123456")

        assertEquals(SupabaseResult.Success(AuthUser(userId, "hero@pixelquest.test", null)), result)
        val body = bodyOf(requests.single())
        assertEquals("email", body["type"]!!.jsonPrimitive.content)
        assertEquals("123456", body["token"]!!.jsonPrimitive.content)
        assertEquals("access-1", supabase.auth.currentAccessTokenOrNull())
    }

    @Test
    fun anExpiredCode_comesBackAsItsErrorCode() = runBlocking {
        verifyStatus = HttpStatusCode.Forbidden

        val result = repository.verifyEmailCode("hero@pixelquest.test", "000000")

        assertEquals("otp_expired", (result as SupabaseResult.ServerError).message)
        assertTrue(EmailSignIn.verifyFailure(result).contains("wrong or has expired"))
    }

    @Test
    fun theAiCoachSeesTheSignedInAccount_andItsToken() = runBlocking {
        val access = SupabaseAiAccess(supabase.auth)
        assertEquals(null, access.accessToken())

        repository.verifyEmailCode("hero@pixelquest.test", "123456")

        assertEquals(true, kotlinx.coroutines.withTimeout(5_000) { access.isSignedIn.first { it } })
        assertEquals("access-1", access.accessToken())

        repository.signOut()
        assertEquals(null, access.accessToken())
    }
}
