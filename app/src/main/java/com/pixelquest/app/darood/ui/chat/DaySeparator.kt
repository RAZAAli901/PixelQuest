package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Where a new day starts in the chat: Today, Yesterday, or the date, in a small centred pill. */
@Composable
fun DaySeparator(day: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    val look = chatLook()
    val label = when (day) {
        today -> stringResource(R.string.darood_day_today)
        today.minusDays(1) -> stringResource(R.string.darood_day_yesterday)
        else -> dayDate(day)
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .testTag(DaroodTestTags.day(day.toString()))
                .semantics { heading() }
                .then(look.shadowed(look.pillShape))
                .background(colors.surface, look.pillShape)
                .then(look.outlined(look.pillShape, look.pillOutline))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** A day before yesterday, in the phone's own language: Oct 17, 2026. */
fun dayDate(day: LocalDate): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(day)
