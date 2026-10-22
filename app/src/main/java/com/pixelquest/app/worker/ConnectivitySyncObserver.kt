package com.pixelquest.app.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivitySyncObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler,
    private val userProfileRepository: UserProfileRepository,
    private val pendingChoice: com.pixelquest.app.data.repository.PendingLeaderboardChoice? = null,
    private val pendingProgressReset: com.pixelquest.app.data.repository.PendingProgressReset? = null,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch { onBackOnline() }
        }
    }

    /**
     * Back online: sync if there's anything to send. A phone off the board with a leave still
     * waiting used to get no sync here, so its player stayed public until the next completed quest.
     */
    internal suspend fun onBackOnline() {
        val profile = userProfileRepository.getProfile().first()
        val accountId = profile?.supabaseUserId?.ifBlank { null } ?: return
        val choiceWaiting = pendingChoice?.pendingFor(accountId) != null
        val resetWaiting = pendingProgressReset?.isPendingFor(accountId) == true
        if (profile.leaderboardOptIn || choiceWaiting || resetWaiting) {
            syncScheduler.scheduleProfileSync(debounceMs = 500L)
        }
    }

    fun startObserving() {
        if (isRegistered || connectivityManager == null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
        } catch (_: Exception) {
            // Guard against SecurityException or device limits
        }
    }

    fun stopObserving() {
        if (!isRegistered || connectivityManager == null) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isRegistered = false
            scope.coroutineContext.cancelChildren()
        } catch (_: Exception) {
            // Guard against unregister errors
        }
    }
}
