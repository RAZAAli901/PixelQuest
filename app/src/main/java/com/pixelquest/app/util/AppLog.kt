package com.pixelquest.app.util

import android.util.Log
import com.pixelquest.app.BuildConfig

/**
 * The app's only way to write to Logcat (S055; docs/security/LOGGING.md). Logcat can be read over
 * USB and by bug-report tools, so a release build writes only what helps a crash report and can't
 * carry a player's data:
 *  - [d] writes nothing in a release build;
 *  - [w] and [e] write the message (ids, a prayer, a date: never names, text, tokens or emails)
 *    and only the exception's class, not its message or stack trace, which can quote a server
 *    answer or a file's contents.
 * Debug builds write everything, as before.
 */
object AppLog {

    /** Release-build behaviour: true unless this is a debug build. Tests can flip it. */
    @Volatile
    internal var release: Boolean = !BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (!release) Log.d(tag, message)
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        if (release) Log.w(tag, line(message, error)) else Log.w(tag, message, error)
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        if (release) Log.e(tag, line(message, error)) else Log.e(tag, message, error)
    }

    /** What a release build writes: the message and the exception's class name. */
    internal fun line(message: String, error: Throwable?): String =
        if (error == null) message else "$message (${error.javaClass.simpleName})"
}
