package com.pixelquest.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Where a new day starts in the chat: Today, Yesterday, or the date, in a small centred pill; [tag] tests find it by. */
@Composable
fun DaySeparator(day: LocalDate, today: LocalDate, modifier: Modifier = Modifier, tag: String = "chat_day_$day") {
    val look = chatLook()
    val label = when (day) {
        today -> stringResource(R.string.chat_day_today)
        today.minusDays(1) -> stringResource(R.string.chat_day_yesterday)
        else -> dayDate(day)
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = PqType.MetaStrong,
            modifier = Modifier
                .testTag(tag)
                .semantics { heading() }
                .background(PqColors.Paper, look.pillShape)
                .then(look.outlined(look.pillShape))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/** A day before yesterday, in the phone's own language: Oct 17, 2026. */
fun dayDate(day: LocalDate): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(day)
