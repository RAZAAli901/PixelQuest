package com.pixelquest.mascot

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Moody app icon (design doc §02). Swaps the launcher icon between 5 Pix states using activity-aliases.
 * See manifest-snippet.xml. Call MoodyIcon.install(app) once in Application.onCreate().
 *
 * Caveats (Android): some launchers take a few seconds to refresh, and a few briefly drop the
 * home-screen shortcut. That's why we switch rarely and only while the app is in the background.
 */
object MoodyIcon {
    enum class State(val alias: String) {
        Happy(".LauncherPixHappy"), OnFire(".LauncherPixOnFire"), Worried(".LauncherPixWorried"),
        Sad(".LauncherPixSad"), Sleepy(".LauncherPixSleepy")
    }

    /** Inputs come from your repositories. */
    data class Inputs(
        val enabled: Boolean,            // Settings › Look › Moody app icon
        val streakDays: Int,
        val doneToday: Boolean,
        val hourOfDay: Int,
        val streakBrokeWithin24h: Boolean,
        val daysSinceLastOpen: Int,
        val gameOrTimerRunning: Boolean,
    )

    fun target(i: Inputs): State = when {
        !i.enabled -> State.Happy
        i.daysSinceLastOpen >= 3 -> State.Sleepy
        i.streakBrokeWithin24h && i.streakDays == 0 -> State.Sad
        i.streakDays >= 2 && !i.doneToday && i.hourOfDay >= 20 -> State.Worried
        i.streakDays >= 7 && i.doneToday -> State.OnFire
        else -> State.Happy
    }

    private const val PREFS = "pq_moody_icon"
    private const val MIN_GAP_MS = 6 * 60 * 60 * 1000L

    /** Re-evaluate on every background transition. [inputs] is called lazily. */
    fun install(app: Context, inputs: () -> Inputs) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) { apply(app, inputs()) }
        })
    }

    /** Also call from your daily WorkManager job (e.g. at 20:00 and 00:05) so the icon changes while the app is closed. */
    fun apply(ctx: Context, i: Inputs, force: Boolean = false) {
        if (i.gameOrTimerRunning) return
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString("state", State.Happy.name)!!.let(State::valueOf)
        val next = target(i)
        if (next == current) return
        val last = prefs.getLong("at", 0L)
        // Going back to Happy (e.g. quest completed) is always allowed; other switches respect the gap.
        if (!force && next != State.Happy && System.currentTimeMillis() - last < MIN_GAP_MS) return
        val pm = ctx.packageManager
        State.entries.forEach { s ->
            pm.setComponentEnabledSetting(
                ComponentName(ctx, ctx.packageName + s.alias),
                if (s == next) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        prefs.edit().putString("state", next.name).putLong("at", System.currentTimeMillis()).apply()
    }
}
