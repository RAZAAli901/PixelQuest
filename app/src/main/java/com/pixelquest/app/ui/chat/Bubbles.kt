package com.pixelquest.app.ui.chat

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.PqColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// What every party chat's bubbles share (mock 4E): their colours and the time they show.

/** A bubble's colours: its fill, and the text on it. */
data class BubbleColors(val container: Color, val content: Color, val secondary: Color)

/** The player's own bubbles in the primary red (mock 4E); everyone else's on paper. */
fun bubbleColors(mine: Boolean): BubbleColors =
    if (mine) {
        BubbleColors(container = PqColors.Red, content = PqColors.OnRed, secondary = PqColors.OnRed.copy(alpha = 0.8f))
    } else {
        BubbleColors(container = PqColors.Paper, content = PqColors.Ink, secondary = PqColors.Muted)
    }

/** The time a bubble shows, in the phone's own clock and language: 10:05 AM, or 10:05. */
fun bubbleTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(instant)
