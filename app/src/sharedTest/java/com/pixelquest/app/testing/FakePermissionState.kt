package com.pixelquest.app.testing

enum class FakePermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
}

class FakePermissionState(
    val permission: String,
    initialStatus: FakePermissionStatus = FakePermissionStatus.DENIED,
) {
    var status: FakePermissionStatus = initialStatus
        private set

    var requestCount: Int = 0
        private set

    val isGranted: Boolean
        get() = status == FakePermissionStatus.GRANTED

    val shouldShowRationale: Boolean
        get() = status == FakePermissionStatus.DENIED

    val isPermanentlyDenied: Boolean
        get() = status == FakePermissionStatus.PERMANENTLY_DENIED

    fun grant() {
        status = FakePermissionStatus.GRANTED
    }

    fun deny(permanently: Boolean = false) {
        status = if (permanently) FakePermissionStatus.PERMANENTLY_DENIED else FakePermissionStatus.DENIED
    }

    fun launchPermissionRequest(resultingStatus: FakePermissionStatus? = null) {
        requestCount++
        if (resultingStatus != null) {
            status = resultingStatus
        }
    }
}

class FakePermissionManager {
    private val permissions = mutableMapOf<String, FakePermissionState>()

    fun getOrCreate(permission: String, initialStatus: FakePermissionStatus = FakePermissionStatus.DENIED): FakePermissionState = permissions.getOrPut(permission) { FakePermissionState(permission, initialStatus) }

    fun setPermission(permission: String, status: FakePermissionStatus) {
        getOrCreate(permission).let {
            when (status) {
                FakePermissionStatus.GRANTED -> it.grant()
                FakePermissionStatus.DENIED -> it.deny(permanently = false)
                FakePermissionStatus.PERMANENTLY_DENIED -> it.deny(permanently = true)
            }
        }
    }

    fun isGranted(permission: String): Boolean = permissions[permission]?.isGranted ?: false

    fun getRequestCount(permission: String): Int = permissions[permission]?.requestCount ?: 0
}
