package com.pixelquest.app.namaz

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** A rough position: prayer times need no more than the city. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** The phone's position for the Namaz Tracker (docs/namaz-design.md, Location). */
interface LocationProvider {

    /** Whether the player has allowed the app to read its (coarse) location. */
    fun hasPermission(): Boolean

    /** The phone's rough position, or null without permission, with location off, or with no fix. */
    suspend fun currentLocation(): GeoPoint?
}

/**
 * Android's own LocationManager: the app doesn't include Google Play location services. Takes a
 * recent last-known fix, or on Android 11+ asks the network provider for a current one.
 */
@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    override fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission") // checked by hasPermission()
    override suspend fun currentLocation(): GeoPoint? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val lastKnown = PROVIDERS
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        if (lastKnown != null && System.currentTimeMillis() - lastKnown.time <= MAX_AGE_MILLIS) return lastKnown.toGeoPoint()

        val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        ) {
            withTimeoutOrNull(FIX_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val cancel = CancellationSignal()
                    continuation.invokeOnCancellation { cancel.cancel() }
                    manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, cancel, context.mainExecutor) { location ->
                        continuation.resume(location)
                    }
                }
            }
        } else {
            null
        }
        return (current ?: lastKnown)?.toGeoPoint()
    }

    private fun Location.toGeoPoint() = GeoPoint(latitude, longitude)

    private companion object {
        val PROVIDERS = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)

        /** A last-known fix younger than a day is close enough for prayer times. */
        const val MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L
        const val FIX_TIMEOUT_MILLIS = 10_000L
    }
}
