package com.pixelquest.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PixelCrashHandler {

    private const val TAG = "PixelCrashHandler"
    private const val CRASH_LOG_FILENAME = "pixelquest_crash_log.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logCrashLocally(context, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash details locally", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun logCrashLocally(context: Context, thread: Thread, throwable: Throwable) {
        val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        
        FileWriter(crashFile, true).use { fw ->
            PrintWriter(fw).use { pw ->
                pw.println("=== CRASH REPORT [$timestamp] ===")
                pw.println("Thread: ${thread.name} (id: ${thread.id})")
                pw.println("Exception: ${throwable.javaClass.name} - ${throwable.message}")
                throwable.printStackTrace(pw)
                pw.println("=================================\n")
            }
        }
        Log.e(TAG, "Uncaught exception in thread ${thread.name} written to ${crashFile.absolutePath}", throwable)
    }

    fun getLatestCrashLog(context: Context): String? {
        val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
        return if (crashFile.exists()) crashFile.readText() else null
    }

    fun clearCrashLog(context: Context) {
        val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
        if (crashFile.exists()) {
            crashFile.delete()
        }
    }
}
