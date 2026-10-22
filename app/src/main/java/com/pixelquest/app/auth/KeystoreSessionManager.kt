package com.pixelquest.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The sealed session's own preferences file (never backed up: docs/security/STORAGE.md). */
const val SESSION_PREFS = "pixelquest_session"

/** supabase-kt's own store, where sessions were kept before 1.7.3; null where it can't be made. */
private fun legacySessionManager(): SessionManager? = try {
    SettingsSessionManager()
} catch (e: Exception) {
    null
}

/** Seals and opens bytes; the app's is [KeystoreCipher], tests use their own. */
interface SessionCipher {
    fun seal(plain: ByteArray): ByteArray
    fun open(sealed: ByteArray): ByteArray
}

/**
 * AES-GCM with a key that never leaves the Android Keystore (S034). The key is made the first time
 * it's needed, can only encrypt and decrypt, and isn't exportable; a sealed value is the IV followed
 * by the ciphertext.
 */
class KeystoreCipher(private val alias: String = KEY_ALIAS) : SessionCipher {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    override fun seal(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun open(sealed: ByteArray): ByteArray {
        val iv = sealed.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv)) }
        return cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
    }

    companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "pixelquest_session_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }
}

/**
 * Where supabase-kt keeps the sign-in session (S034, docs/security/STORAGE.md): sealed with a
 * Keystore key in this app's own preferences file, which no backup includes. Before, the library
 * kept the tokens in plain JSON in the default preferences.
 *
 * The first time it loads, a plain session from before is moved in and the plain copy deleted, so
 * nobody is signed out by the update. A session that can't be opened (the key was lost, say, after
 * a restore onto another phone) is dropped: the player signs in again.
 */
class KeystoreSessionManager(
    private val prefs: SharedPreferences,
    private val cipher: SessionCipher,
    private val legacy: SessionManager?
) : SessionManager {

    constructor(context: Context) : this(
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE),
        KeystoreCipher(),
        legacySessionManager()
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        val sealed = cipher.seal(json.encodeToString(UserSession.serializer(), session).toByteArray(Charsets.UTF_8))
        prefs.edit().putString(KEY, Base64.encodeToString(sealed, Base64.NO_WRAP)).commit()
        Unit
    }

    override suspend fun loadSession(): UserSession? = withContext(Dispatchers.IO) {
        val stored = prefs.getString(KEY, null)
        if (stored != null) {
            return@withContext runCatching {
                json.decodeFromString(UserSession.serializer(), String(cipher.open(Base64.decode(stored, Base64.NO_WRAP)), Charsets.UTF_8))
            }.getOrElse {
                prefs.edit().remove(KEY).commit()
                null
            }
        }
        // A plain session from before 1.7.3: moved in once, then gone from the old place.
        val old = legacy?.let { runCatching { it.loadSession() }.getOrNull() } ?: return@withContext null
        saveSession(old)
        runCatching { legacy.deleteSession() }
        old
    }

    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY).commit()
        legacy?.let { runCatching { it.deleteSession() } }
        Unit
    }

    companion object {
        const val PREFS_NAME = SESSION_PREFS
        private const val KEY = "session_sealed_v1"
    }
}
