package com.pixelquest.app.testing

import com.pixelquest.app.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNetworkMonitor(
    initialOnline: Boolean = true,
) : NetworkMonitor {

    private val onlineState = MutableStateFlow(initialOnline)
    override val isOnline: Flow<Boolean> = onlineState.asStateFlow()

    override fun isCurrentlyOnline(): Boolean = onlineState.value

    fun setOnline(online: Boolean) {
        onlineState.value = online
    }

    fun simulateNetworkDrop() {
        setOnline(false)
    }

    fun simulateNetworkRestored() {
        setOnline(true)
    }
}
