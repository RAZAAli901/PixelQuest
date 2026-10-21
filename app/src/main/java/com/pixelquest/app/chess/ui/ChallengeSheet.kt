package com.pixelquest.app.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.components.PqSwitch
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

object ChallengeTags {
    const val SHEET = "chess_challenge_sheet"
    const val SEND = "chess_challenge_send"
    const val RATED = "chess_challenge_rated"
    const val RECORD = "chess_challenge_record"
    const val ROOM = "chess_challenge_room"
    fun member(id: String) = "chess_challenge_member_$id"
    fun time(tc: TimeControl) = "chess_challenge_time_${tc.seconds}"
    fun colour(c: ColourChoice) = "chess_challenge_colour_${c.wire}"
}

/**
 * The challenge sheet (J150), as the design's: pick who (a member, or the whole room), see the
 * record against them at once, then the time control (1, 3, 5 or 10 minutes), the colour
 * (White, Random or Black) and rated or friendly. [onPick] asks for the record of whoever's picked.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChallengeSheet(
    state: ChessPartyState,
    onPick: (String?) -> Unit,
    onSend: (to: String?, TimeControl, ColourChoice, rated: Boolean) -> Unit,
    onDismiss: () -> Unit,
    initialOpponent: String? = null
) {
    var to by rememberSaveable { mutableStateOf(initialOpponent) }
    var room by rememberSaveable { mutableStateOf(false) }
    var time by rememberSaveable { mutableStateOf(TimeControl.THREE) }
    var colour by rememberSaveable { mutableStateOf(ColourChoice.RANDOM) }
    var rated by rememberSaveable { mutableStateOf(true) }
    PqSheet(onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag(ChallengeTags.SHEET)
        ) {
            Text(text = stringResource(R.string.chess_challenge_title).uppercase(), style = PqType.Title.copy(color = PqColors.Title), modifier = Modifier.semantics { heading() })
            PqFieldLabel(stringResource(R.string.chess_challenge_who))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                state.members.forEachIndexed { i, member ->
                    val picked = !room && to == member.userId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .testTag(ChallengeTags.member(member.userId))
                            .selectable(selected = picked, role = Role.RadioButton) {
                                room = false
                                to = member.userId
                                onPick(member.userId)
                            }
                            .padding(4.dp)
                    ) {
                        Box(if (picked) Modifier.comic(radius = 999.dp, border = 3.dp, shadow = 2.dp) else Modifier) {
                            InitialAvatar(member.name, 44.dp, ChessLook.avatar(i))
                        }
                        Text(text = "@${member.name}", style = if (picked) PqType.MetaStrong else PqType.Meta, maxLines = 1)
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .testTag(ChallengeTags.ROOM)
                        .selectable(selected = room, role = Role.RadioButton) {
                            room = true
                            to = null
                            onPick(null)
                        }
                        .padding(4.dp)
                ) {
                    Box(if (room) Modifier.comic(radius = 999.dp, border = 3.dp, shadow = 2.dp) else Modifier) { InitialAvatar("*", 44.dp, PqColors.Gold) }
                    Text(text = stringResource(R.string.chess_challenge_room), style = if (room) PqType.MetaStrong else PqType.Meta)
                }
            }
            val record = state.preview.firstOrNull { it.timeControl == null }
            if (!room && to != null) {
                Text(
                    text = if (record == null || record.games == 0) stringResource(R.string.chess_challenge_first)
                    else stringResource(R.string.chess_challenge_record, record.myWins, record.draws, record.theirWins),
                    style = PqType.MetaStrong,
                    modifier = Modifier.testTag(ChallengeTags.RECORD)
                )
            }
            PqFieldLabel(stringResource(R.string.chess_challenge_time))
            Segmented(TimeControl.entries, time, { stringResource(R.string.chess_minutes, it.seconds / 60) }, ChallengeTags::time) { time = it }
            PqFieldLabel(stringResource(R.string.chess_challenge_colour))
            Segmented(listOf(ColourChoice.WHITE, ColourChoice.RANDOM, ColourChoice.BLACK), colour, {
                stringResource(
                    when (it) {
                        ColourChoice.WHITE -> R.string.chess_white
                        ColourChoice.BLACK -> R.string.chess_black
                        ColourChoice.RANDOM -> R.string.chess_random
                    }
                )
            }, ChallengeTags::colour) { colour = it }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ChallengeTags.RATED)
                    .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
                    .toggleable(value = rated, role = Role.Switch, onValueChange = { rated = it })
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .heightIn(min = 48.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = stringResource(R.string.chess_rated), style = PqType.BodyStrong)
                    Text(text = stringResource(if (rated) R.string.chess_rated_body else R.string.chess_friendly_body), style = PqType.Meta)
                }
                PqSwitch(checked = rated, onCheckedChange = null)
            }
            state.problem?.let { Text(text = problemText(it), style = PqType.Meta.copy(color = PqColors.Error)) }
            PqButton(
                text = stringResource(R.string.chess_challenge_send),
                onClick = { onSend(if (room) null else to, time, colour, rated) },
                enabled = !state.sending && (room || to != null),
                modifier = Modifier.fillMaxWidth().testTag(ChallengeTags.SEND)
            )
        }
    }
}

/** A row of choices, one selected, as the design's segmented controls. */
@Composable
internal fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, tag: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().comic(radius = PqDimens.RadiusSm, border = PqDimens.BorderThin, shadow = 0.dp)) {
        options.forEach { option ->
            val on = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (on) PqColors.Ink else PqColors.Paper)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(option) }
                    .testTag(tag(option))
            ) {
                Text(text = label(option), style = PqType.Button.copy(color = if (on) PqColors.Paper else PqColors.Ink), textAlign = TextAlign.Center)
            }
        }
    }
}

/** What a server refusal means, for the player. */
@Composable
fun problemText(token: String): String = stringResource(
    when (token) {
        "already_playing" -> R.string.chess_problem_playing
        "rate_limited" -> R.string.chess_problem_rate
        "blocked" -> R.string.chess_problem_blocked
        "not_a_member" -> R.string.chess_problem_member
        "expired" -> R.string.chess_problem_expired
        "already_answered" -> R.string.chess_problem_answered
        ChessSessionViewModel.OFFLINE -> R.string.chess_problem_offline
        else -> R.string.chess_problem_other
    }
)
