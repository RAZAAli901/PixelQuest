package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import kotlinx.coroutines.delay
import java.time.Duration

/**
 * The next prayer, big: its name, its time, and a countdown that moves every second. The screen's
 * state moves on every minute, which sets the countdown right again; at zero, [onCountdownEnd]
 * asks for the next prayer straight away.
 */
@Composable
fun NextPrayerCard(
    next: NextPrayer,
    onCountdownEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val formatter = rememberPrayerTimeFormatter()
    val currentOnEnd by rememberUpdatedState(onCountdownEnd)
    var remaining by remember(next) { mutableStateOf(next.remaining.coerceAtLeast(Duration.ZERO)) }
    LaunchedEffect(next) {
        while (remaining > Duration.ZERO) {
            delay(1_000)
            remaining = remaining.minusSeconds(1).coerceAtLeast(Duration.ZERO)
        }
        currentOnEnd()
    }

    PixelCard(variant = PixelPanelVariant.BEIGE, modifier = modifier.fillMaxWidth().testTag(NamazTestTags.NEXT)) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.namaz_next_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                text = stringResource(next.next.prayer.displayName),
                style = MaterialTheme.typography.headlineMedium,
                color = inkOnPanel(colors.primaryText)
            )
            Text(
                text = stringResource(R.string.namaz_next_at, next.next.time.format(formatter)),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            Text(
                text = "⏳ " + stringResource(R.string.namaz_next_starts_in, formatCountdown(remaining)),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                modifier = Modifier.testTag(NamazTestTags.COUNTDOWN)
            )
            Text(
                text = stringResource(R.string.namaz_now_label, stringResource(next.current.prayer.displayName)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
