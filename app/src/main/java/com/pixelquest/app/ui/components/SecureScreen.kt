package com.pixelquest.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Keeps the screen out of screenshots, screen recordings and the recent-apps preview while it's
 * shown (S043): the account screen, a study party's chat, and a rated chess game being played.
 * Screens can nest (a chat inside a party screen), so the flag stays until the last one leaves.
 */
@Composable
fun SecureScreen(enabled: Boolean = true) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        SecureFlag.acquire(activity)
        onDispose { SecureFlag.release(activity) }
    }
}

internal object SecureFlag {
    private val holders = java.util.WeakHashMap<Activity, Int>()

    fun acquire(activity: Activity) {
        val count = (holders[activity] ?: 0) + 1
        holders[activity] = count
        if (count == 1) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun release(activity: Activity) {
        val count = (holders[activity] ?: 1) - 1
        if (count <= 0) {
            holders.remove(activity)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            holders[activity] = count
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
