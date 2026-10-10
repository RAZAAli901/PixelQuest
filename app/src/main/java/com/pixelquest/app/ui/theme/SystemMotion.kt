package com.pixelquest.app.ui.theme

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Android's own "Remove animations" (Settings > Accessibility, which sets the animator duration scale
 * to 0). PixelQuest treats it like its own REDUCE MOTION switch, so players who turned motion off for
 * the whole phone don't have to find a second switch.
 */
object SystemMotion {

    fun animationsOff(resolver: ContentResolver): Boolean =
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    /** The setting now, and again whenever it changes. */
    fun animationsOffChanges(context: Context): Flow<Boolean> = callbackFlow {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(animationsOff(resolver))
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        trySend(animationsOff(resolver))
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()
}

/** Whether motion should be reduced: the app's REDUCE MOTION switch, or Android's "Remove animations". */
@Composable
fun rememberReduceMotion(appSetting: Boolean): Boolean {
    val context = LocalContext.current
    val systemOff by remember(context) { SystemMotion.animationsOffChanges(context) }
        .collectAsState(initial = SystemMotion.animationsOff(context.contentResolver))
    return appSetting || systemOff
}
