package com.pixelquest.app.darood.data

import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.testing.FakeUsernameCache
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The username shown is the signed-in account's own, never one kept from another account. */
class MyUsernameTest {

    private val auth = FakeAuthRepository()
    private val cache = FakeUsernameCache()
    private val myUsername = MyUsername(auth, cache)
    private val aria = AuthUser(id = "aria-id", email = null, displayName = null)
    private val bram = AuthUser(id = "bram-id", email = null, displayName = null)

    @Test
    fun signedIn_showsTheAccountsName() = runBlocking {
        auth.userFlow.value = aria
        cache.save("aria-id", Username.parse("aria_k")!!)

        assertEquals(Username.parse("aria_k"), myUsername.username.first())
    }

    @Test
    fun signedOut_showsNone() = runBlocking {
        cache.save("aria-id", Username.parse("aria_k")!!)

        assertNull(myUsername.username.first())
    }

    @Test
    fun anotherAccountsName_isNeverShown() = runBlocking {
        cache.save("aria-id", Username.parse("aria_k")!!)
        auth.userFlow.value = bram

        assertNull(myUsername.username.first())
    }
}
