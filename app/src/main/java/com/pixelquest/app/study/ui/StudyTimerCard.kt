package com.pixelquest.app.study.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import kotlinx.coroutines.delay

/** The study timer's buttons, and logging minutes by hand. */
data class TimerActions(
    val onStart: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onLogMinutes: (Int) -> Unit = {}
)

/**
 * Study time on This week: the player's minutes today, the timer (start, then stop and log, or
 * cancel), and minutes added by hand, picked and then logged, since study time can't be changed
 * once logged.
 */
@Composable
fun StudyTimerCard(state: StudyPartyUiState, actions: TimerActions, clock: () -> Long = System::currentTimeMillis) {
    val colors = PixelTheme.colors
    val running = state.timer
    val partyId = state.party?.id
    PixelCard(variant = PixelPanelVariant.BORDER, contentPadding = 16.dp, modifier = Modifier.fillMaxWidth().testTag(StudyTags.TIMER)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.study_timer_title), style = MaterialTheme.typography.titleSmall, color = inkOnPanel(colors.primaryText), modifier = Modifier.weight(1f))
                Text(
                    text = pluralStringResource(R.plurals.study_minutes_today, state.studiedToday, state.studiedToday),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.testTag(StudyTags.TIMER_TODAY)
                )
            }
            when {
                running == null -> PixelButton(text = stringResource(R.string.study_timer_start), onClick = actions.onStart, modifier = Modifier.fillMaxWidth().testTag(StudyTags.TIMER_START))
                running.partyId == partyId -> {
                    val elapsed by produceState(clock() - running.startedAt, running) {
                        while (true) {
                            value = clock() - running.startedAt
                            delay(1_000)
                        }
                    }
                    Text(
                        text = stringResource(R.string.study_timer_running, elapsedText(elapsed)),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier.testTag(StudyTags.TIMER_RUNNING)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PixelButton(text = stringResource(R.string.study_timer_stop), onClick = actions.onStop, modifier = Modifier.weight(1f).testTag(StudyTags.TIMER_STOP))
                        TextButton(onClick = actions.onCancel, modifier = Modifier.testTag(StudyTags.TIMER_CANCEL)) {
                            Text(text = stringResource(R.string.study_timer_cancel), color = colors.primaryText)
                        }
                    }
                }
                else -> Text(text = stringResource(R.string.study_timer_elsewhere), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            var chosen by rememberSaveable { mutableStateOf<Int?>(null) }
            Text(text = stringResource(R.string.study_add_minutes), style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                items(MINUTE_CHOICES, key = { it }) { minutes ->
                    ChoiceChip(stringResource(R.string.study_minutes_chip, minutes), chosen == minutes, Modifier.testTag(StudyTags.minutes(minutes))) {
                        chosen = if (chosen == minutes) null else minutes
                    }
                }
            }
            chosen?.let { minutes ->
                PixelButton(
                    text = pluralStringResource(R.plurals.study_log_minutes, minutes, minutes),
                    onClick = {
                        actions.onLogMinutes(minutes)
                        chosen = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag(StudyTags.LOG_MINUTES)
                )
            }
        }
    }
}

/** The minutes offered to add by hand. */
val MINUTE_CHOICES = listOf(15, 25, 30, 45, 60, 90, 120)

/** "25:03", or "1:02:09" from an hour on. */
fun elapsedText(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val locale = java.util.Locale.getDefault()
    return if (h > 0) String.format(locale, "%d:%02d:%02d", h, m, s) else String.format(locale, "%d:%02d", m, s)
}
