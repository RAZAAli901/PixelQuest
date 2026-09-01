package com.pixelquest.app.util

import android.util.Log

suspend inline fun <T> safeDatabaseCall(
    fallback: T,
    crossinline block: suspend () -> T
): T {
    return try {
        block()
    } catch (e: Exception) {
        Log.e("SafeDatabaseCall", "Database operation failed, using graceful fallback", e)
        fallback
    }
}
