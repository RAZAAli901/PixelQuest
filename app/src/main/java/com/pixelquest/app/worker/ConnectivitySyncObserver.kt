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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors device network connectivity state transitions.
 * When network connectivity is restored on an authenticated, opted-in device,
 * this observer immediately schedules a retry of pending profile syncs
 * rather than waiting for the next gameplay trigger event.
 */
@Singleton
class ConnectivitySyncObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler,
    private val userProfileRepository: UserProfileRepository
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch {
                val profile = userProfileRepository.getProfile().first()
                val isLinked = !profile?.supabaseUserId.isNullOrBlank()
                val isOptedIn = profile?.leaderboardOptIn == true
                if (isLinked && isOptedIn) {
                    syncScheduler.scheduleProfileSync(debounceMs = 500L)
                }
            }
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
        } catch (_: Exception) {
            // Guard against unregister errors
        }
    }
}
