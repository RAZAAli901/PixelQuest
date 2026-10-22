package com.pixelquest.app.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/** S034 on a real device: the Keystore key seals and opens, and never leaves the Keystore. */
@RunWith(AndroidJUnit4::class)
class KeystoreCipherInstrumentedTest {

    @Test
    fun sealsAndOpens_withADifferentIvEachTime() {
        val cipher = KeystoreCipher(alias = "pixelquest_session_key_test")
        val plain = "{\"access_token\":\"abc\",\"refresh_token\":\"def\"}".toByteArray()
        val a = cipher.seal(plain)
        val b = cipher.seal(plain)
        assertFalse("not readable", String(a).contains("access_token"))
        assertFalse("a fresh IV each time", a.contentEquals(b))
        assertArrayEquals(plain, cipher.open(a))
        assertArrayEquals(plain, cipher.open(b))
    }

    @Test
    fun theKey_livesInTheKeystore_andCantBeExported() {
        KeystoreCipher(alias = "pixelquest_session_key_test").seal(byteArrayOf(1))
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertTrue(store.containsAlias("pixelquest_session_key_test"))
        val key = store.getKey("pixelquest_session_key_test", null)
        assertTrue("no key material leaves the Keystore", key.encoded == null)
    }
}
