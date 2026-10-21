package com.pixelquest.app.ui.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Notifications are asked for when the player first turns a reminder on (docs/ui-v2.md, step 6),
 * not when the app opens: then it's clear why. Whether they've been asked is kept, so the banner
 * that offers Android's settings shows only once they've said no.
 */
object NotificationAsk {
    private const val PREFS = "pixelquest_permissions"
    private const val KEY_ASKED = "asked_post_notifications"

    /** Android 13 and up still need the player's yes to post notifications. */
    fun needed(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun asked(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    fun markAsked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ASKED, true).apply()
    }
}

/** A function to call as the player turns a reminder on: it asks for notifications if they're still needed. */
@Composable
fun rememberAskForNotifications(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return remember(context, launcher) {
        {
            if (NotificationAsk.needed(context)) {
                NotificationAsk.markAsked(context)
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
