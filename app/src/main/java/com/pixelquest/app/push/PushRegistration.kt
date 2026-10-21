package com.pixelquest.app.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.pixelquest.app.BuildConfig
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** This phone's push token (Firebase Cloud Messaging), or nothing when push isn't set up. */
interface PushTokens {
    suspend fun current(): String?

    /** Ends this token, so nothing more reaches it: on sign-out. */
    suspend fun delete()
}

/** The server's side: device_tokens, written only by its owner (20261019000097). */
interface PushTokenRemote {
    suspend fun register(token: String): SupabaseResult<Unit>
}

/**
 * Push for chess (docs/chess-design.md): the phone's token is registered for the account that's
 * signed in, at each launch and sign-in, and whenever Firebase gives a new one. Signing out ends
 * the token on the phone (the session's gone, so the server can't be asked to forget it): the next
 * account gets a new one, and send-push drops the dead one.
 */
@Singleton
class PushRegistration @Inject constructor(
    private val auth: AuthRepository,
    private val tokens: PushTokens,
    private val remote: PushTokenRemote
) {
    @Volatile private var signedIn = false

    fun start(scope: CoroutineScope): Job = scope.launch {
        auth.currentUser.map { it?.id }.distinctUntilChanged().collect { onAccount(it) }
    }

    internal suspend fun onAccount(accountId: String?) {
        signedIn = accountId != null
        if (accountId == null) {
            tokens.delete()
            return
        }
        tokens.current()?.let { remote.register(it) }
    }

    /** Firebase's new token: registered at once if someone's signed in. */
    suspend fun onNewToken(token: String) {
        if (signedIn) remote.register(token)
    }
}

/** Firebase, set up from the build's settings (FCM_*): none of them, no push. */
object PushSetup {
    val configured: Boolean
        get() = listOf(BuildConfig.FCM_API_KEY, BuildConfig.FCM_APP_ID, BuildConfig.FCM_PROJECT_ID, BuildConfig.FCM_SENDER_ID).all { it.isNotBlank() }

    fun app(context: Context): FirebaseApp? {
        if (!configured) return null
        FirebaseApp.getApps(context).firstOrNull()?.let { return it }
        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FCM_API_KEY)
            .setApplicationId(BuildConfig.FCM_APP_ID)
            .setProjectId(BuildConfig.FCM_PROJECT_ID)
            .setGcmSenderId(BuildConfig.FCM_SENDER_ID)
            .build()
        return runCatching { FirebaseApp.initializeApp(context, options) }.getOrNull()
    }
}

@Singleton
class FirebasePushTokens @Inject constructor(@ApplicationContext private val context: Context) : PushTokens {
    private fun messaging(): FirebaseMessaging? = PushSetup.app(context)?.let { runCatching { FirebaseMessaging.getInstance() }.getOrNull() }

    override suspend fun current(): String? {
        val messaging = messaging() ?: return null
        return suspendCancellableCoroutine { done ->
            messaging.token.addOnCompleteListener { task -> done.resume(if (task.isSuccessful) task.result else null) }
        }
    }

    override suspend fun delete() {
        val messaging = messaging() ?: return
        suspendCancellableCoroutine { done -> messaging.deleteToken().addOnCompleteListener { done.resume(Unit) } }
    }
}

class SupabasePushTokenRemote @Inject constructor(private val postgrest: Postgrest) : PushTokenRemote {
    override suspend fun register(token: String): SupabaseResult<Unit> = safeSupabaseCall {
        postgrest.rpc("register_device_token", buildJsonObject {
            put("token", token)
            put("platform", "android")
        })
        Unit
    }
}
