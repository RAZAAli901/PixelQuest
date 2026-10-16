package com.pixelquest.app.ui.screens.namaz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelConfirmDialog

/** Asks for the phone's rough location, after saying why (docs/namaz-design.md, Location). */
@Stable
class LocationPermissionRequest internal constructor(private val start: () -> Unit) {
    /** Reports straight away if it's already allowed; otherwise explains, then asks Android. */
    fun launch() = start()
}

/**
 * The Namaz Tracker's location request: a short explanation first, then Android's own prompt for
 * coarse (city-level) location. [onResult] gets whether it's allowed; "Not now" and a refusal both
 * leave the player to pick a city (or keep Lahore).
 */
@Composable
fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): LocationPermissionRequest {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    var explaining by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        currentOnResult(granted)
    }

    if (explaining) {
        PixelConfirmDialog(
            title = stringResource(R.string.namaz_location_title),
            message = stringResource(R.string.namaz_location_message),
            confirmText = stringResource(R.string.namaz_location_allow),
            dismissText = stringResource(R.string.namaz_location_not_now),
            onConfirm = {
                explaining = false
                launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            onDismiss = {
                explaining = false
                currentOnResult(false)
            }
        )
    }

    return remember(context) {
        LocationPermissionRequest {
            if (hasLocationPermission(context)) currentOnResult(true) else explaining = true
        }
    }
}

internal fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
