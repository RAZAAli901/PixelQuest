package com.pixelquest.app.push

import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** The push token follows the account: registered at sign-in and on a new token, ended at sign-out. */
class PushRegistrationTest {
    private val registered = mutableListOf<String>()
    private var deleted = 0
    private var token: String? = "token-1"
    private val tokens = object : PushTokens {
        override suspend fun current() = token
        override suspend fun delete() { deleted++ }
    }
    private val remote = object : PushTokenRemote {
        override suspend fun register(token: String): SupabaseResult<Unit> { registered += token; return SupabaseResult.Success(Unit) }
    }
    private val registration = PushRegistration(java.lang.reflect.Proxy.newProxyInstance(
        AuthRepository::class.java.classLoader, arrayOf(AuthRepository::class.java)
    ) { _, _, _ -> null } as AuthRepository, tokens, remote)

    @Test
    fun signedIn_registers_newTokensFollow_signedOut_ends(): Unit = runBlocking {
        registration.onNewToken("before")
        assertEquals("nobody to register for", emptyList<String>(), registered)
        registration.onAccount("aria")
        registration.onNewToken("token-2")
        assertEquals(listOf("token-1", "token-2"), registered)
        registration.onAccount(null)
        assertEquals(1, deleted)
        registration.onNewToken("token-3")
        assertEquals(listOf("token-1", "token-2"), registered)
    }

    @Test
    fun pushNotSetUp_nothingIsSent(): Unit = runBlocking {
        token = null
        registration.onAccount("aria")
        assertEquals(emptyList<String>(), registered)
    }
}
