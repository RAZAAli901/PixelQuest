package com.pixelquest.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase's messages to this phone (docs/chess-design.md): a new token is registered for the
 * signed-in account. Each push carries only a kind and ids; what it says is built on the phone
 * (ChessPushNotifier).
 */
@AndroidEntryPoint
class PushService : FirebaseMessagingService() {
    @Inject lateinit var registration: PushRegistration

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { registration.onNewToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) = ChessPushNotifier.show(applicationContext, message.data)
}
