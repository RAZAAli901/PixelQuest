package com.pixelquest.app.auth

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S034: the sign-in session is stored sealed, moved in once from the old plain store, and dropped
 * (not crashed on) when it can't be opened. The real Keystore cipher is checked on a device by
 * KeystoreCipherInstrumentedTest; here a stand-in cipher shows the manager's logic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class KeystoreSessionManagerTest {

    /** Reversible, and visibly not the plain text. */
    private class FlipCipher : SessionCipher {
        var broken = false
        override fun seal(plain: ByteArray) = byteArrayOf(7) + plain.map { (it.toInt() xor 0x5A).toByte() }
        override fun open(sealed: ByteArray): ByteArray {
            check(!broken) { "the key is gone" }
            return sealed.drop(1).map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val prefs = context.getSharedPreferences("test_session_${System.nanoTime()}", Context.MODE_PRIVATE)
    private val session = UserSession(accessToken = "header.payload-access-token.sig", refreshToken = "refresh-token-value", expiresIn = 3600, tokenType = "bearer", user = null)

    @Test
    fun aSavedSession_isSealed_andLoadsBack() = runBlocking {
        val manager = KeystoreSessionManager(prefs, FlipCipher(), legacy = null)
        manager.saveSession(session)
        val stored = prefs.all.values.joinToString()
        assertFalse("the access token isn't stored readable", "payload-access-token" in stored)
        assertFalse("nor the refresh token", "refresh-token-value" in stored)
        val loaded = manager.loadSession()!!
        assertEquals(session.accessToken, loaded.accessToken)
        assertEquals(session.refreshToken, loaded.refreshToken)
    }

    @Test
    fun aPlainSessionFromBefore_isMovedIn_andTheOldCopyDeleted() = runBlocking {
        val old = MemorySessionManager().apply { saveSession(session) }
        val manager = KeystoreSessionManager(prefs, FlipCipher(), legacy = old)
        assertEquals("still signed in after the update", session.accessToken, manager.loadSession()?.accessToken)
        assertNull("the plain copy is gone", old.loadSession())
        assertTrue("and the sealed one is there", prefs.all.isNotEmpty())
        assertEquals(session.accessToken, KeystoreSessionManager(prefs, FlipCipher(), legacy = null).loadSession()?.accessToken)
    }

    @Test
    fun aSessionThatCantBeOpened_isDropped_notACrash() = runBlocking {
        val cipher = FlipCipher()
        val manager = KeystoreSessionManager(prefs, cipher, legacy = null)
        manager.saveSession(session)
        cipher.broken = true
        assertNull("signed out: sign in again", manager.loadSession())
        assertTrue("the unreadable copy is removed", prefs.all.isEmpty())
    }

    @Test
    fun signingOut_deletesBothCopies() = runBlocking {
        val old = MemorySessionManager().apply { saveSession(session) }
        val manager = KeystoreSessionManager(prefs, FlipCipher(), legacy = old)
        manager.saveSession(session)
        manager.deleteSession()
        assertTrue(prefs.all.isEmpty())
        assertNull(old.loadSession())
        assertNull(manager.loadSession())
    }
}
