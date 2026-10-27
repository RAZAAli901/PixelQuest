package com.pixelquest.app.study.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import kotlinx.coroutines.delay

/** The study timer's buttons, and logging minutes by hand. */
data class TimerActions(
    val onStart: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onLogMinutes: (Int) -> Unit = {},
    val onPause: () -> Unit = {},
    val onResume: () -> Unit = {},
)

/**
 * Study time on This week: the player's minutes today, the timer (start, pause, resume, stop and log, or
 * cancel), and minutes added by hand, picked and then logged, since study time can't be changed
 * once logged.
 */
@Composable
fun StudyTimerCard(state: StudyPartyUiState, actions: TimerActions, clock: () -> Long = System::currentTimeMillis) {
    val running = state.timer
    val partyId = state.party?.id
    PqCard(shadow = PqDimens.ShadowLg, modifier = Modifier.fillMaxWidth().testTag(StudyTags.TIMER)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.study_timer_title).uppercase(), style = PqType.CardTitle, modifier = Modifier.weight(1f).semantics { heading() })
                Text(
                    text = pluralStringResource(R.plurals.study_minutes_today, state.studiedToday, state.studiedToday),
                    style = PqType.MetaStrong,
                    modifier = Modifier.testTag(StudyTags.TIMER_TODAY),
                )
            }
            when {
                running == null -> PqButton(
                    text = stringResource(R.string.study_timer_start),
                    onClick = actions.onStart,
                    icon = Icons.Rounded.PlayArrow,
                    height = 50.dp,
                    modifier = Modifier.fillMaxWidth().testTag(StudyTags.TIMER_START),
                )
                running.partyId == partyId -> {
                    val isPaused = running.isPaused
                    val elapsed by produceState(running.elapsedMillisAt(clock()), running) {
                        while (true) {
                            value = running.elapsedMillisAt(clock())
                            if (running.isPaused) break
                            delay(1_000)
                        }
                    }
                    val displayText = if (isPaused) {
                        stringResource(R.string.study_timer_notif_paused, elapsedText(elapsed))
                    } else {
                        stringResource(R.string.study_timer_running, elapsedText(elapsed))
                    }
                    Text(
                        text = displayText,
                        style = PqType.number(28),
                        modifier = Modifier.testTag(StudyTags.TIMER_RUNNING),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (isPaused) {
                            PqButton(
                                text = stringResource(R.string.study_timer_action_resume),
                                onClick = actions.onResume,
                                icon = Icons.Rounded.PlayArrow,
                                height = 50.dp,
                                modifier = Modifier.weight(1f).testTag(StudyTags.TIMER_RESUME),
                            )
                        } else {
                            PqButton(
                                text = stringResource(R.string.study_timer_action_pause),
                                onClick = actions.onPause,
                                icon = Icons.Rounded.Pause,
                                height = 50.dp,
                                modifier = Modifier.weight(1f).testTag(StudyTags.TIMER_PAUSE),
                            )
                        }
                        PqButton(
                            text = stringResource(R.string.study_timer_stop),
                            onClick = actions.onStop,
                            style = PqButtonStyle.Secondary,
                            height = 50.dp,
                            modifier = Modifier.weight(1f).testTag(StudyTags.TIMER_STOP),
                        )
                        Text(
                            text = stringResource(R.string.study_timer_cancel),
                            style = PqType.MetaStrong.copy(color = PqColors.RedText),
                            modifier = Modifier
                                .testTag(StudyTags.TIMER_CANCEL)
                                .clickable(role = Role.Button, onClick = actions.onCancel)
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                        )
                    }
                }
                else -> Text(text = stringResource(R.string.study_timer_elsewhere), style = PqType.Meta)
            }
            var chosen by rememberSaveable { mutableStateOf<Int?>(null) }
            Text(text = stringResource(R.string.study_add_minutes), style = PqType.Meta)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.selectableGroup()) {
                items(MINUTE_CHOICES, key = { it }) { minutes ->
                    ChoiceChip(stringResource(R.string.study_minutes_chip, minutes), chosen == minutes, Modifier.testTag(StudyTags.minutes(minutes))) {
                        chosen = if (chosen == minutes) null else minutes
                    }
                }
            }
            chosen?.let { minutes ->
                PqButton(
                    text = pluralStringResource(R.plurals.study_log_minutes, minutes, minutes),
                    onClick = {
                        actions.onLogMinutes(minutes)
                        chosen = null
                    },
                    style = PqButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth().testTag(StudyTags.LOG_MINUTES),
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
