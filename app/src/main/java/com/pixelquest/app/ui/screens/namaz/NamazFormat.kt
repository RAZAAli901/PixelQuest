package com.pixelquest.app.ui.screens.namaz

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.Duration
import java.time.format.DateTimeFormatter

/** Prayer times as the phone shows the time: "5:29 PM", or "17:29" with the 24-hour clock on. */
@Composable
fun rememberPrayerTimeFormatter(): DateTimeFormatter {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(is24Hour) { prayerTimeFormatter(is24Hour) }
}

fun prayerTimeFormatter(is24Hour: Boolean): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")

/** A countdown as hours, minutes and seconds: "1:23:05", or "4:09" under an hour. */
fun formatCountdown(remaining: Duration): String {
    val total = remaining.seconds.coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
