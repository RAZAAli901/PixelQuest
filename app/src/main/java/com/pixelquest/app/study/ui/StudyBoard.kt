package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.domain.StudyPointsConfig
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.format.DateTimeFormatter

/**
 * The Scoreboard tab: this week's board (every active member, most points first, equal points
 * sharing a place, the player's own line marked), each line with where its points came from, then
 * how points are earned. Points are the party's own, never XP.
 */
fun LazyListScope.scoreboard(state: StudyPartyUiState) {
    item(key = "board_week") {
        val locale = LocalConfiguration.current.locales[0]
        val format = DateTimeFormatter.ofPattern("EEE d MMM", locale)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.study_board_week, state.weekStart.format(format), state.weekStart.plusDays(6).format(format)).uppercase(),
                style = PqType.Label,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Text(text = stringResource(R.string.study_board_resets), style = PqType.Meta)
        }
    }
    if (state.board.none { it.points > 0 }) {
        item(key = "board_empty") {
            Text(
                text = stringResource(if (state.loaded) R.string.study_board_empty else R.string.study_loading),
                style = PqType.Body.copy(color = PqColors.Ink2),
                modifier = Modifier.fillMaxWidth().testTag(StudyTags.EMPTY)
            )
        }
    } else {
        // One card, a line a member, with ink between them (mock 4J).
        item(key = "board") {
            PqCard(shadow = PqDimens.ShadowLg) {
                Column {
                    state.board.forEachIndexed { i, line ->
                        BoardRow(line)
                        if (i != state.board.lastIndex) Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
                    }
                }
            }
        }
    }
    if (state.pastWeeks.isNotEmpty()) {
        item(key = "past_title") {
            PqGroupLabel(stringResource(R.string.study_past_weeks), Modifier.semantics { heading() })
        }
        items(state.pastWeeks, key = { "past_" + it.weekStart }) { week -> PastWeekRow(week, state.members) }
    }
    item(key = "board_rules") { PointsRules() }
}

/** Last week's winners, over This week and the Scoreboard, until dismissed. */
@Composable
fun WinnerBannerIfAny(state: StudyPartyUiState, onDismiss: () -> Unit) {
    if (state.winnerBanner.isNotEmpty()) WinnerBanner(state, onDismiss)
}

@Composable
private fun WinnerBanner(state: StudyPartyUiState, onDismiss: () -> Unit) {
    val winners = state.winnerBanner
    val names = winners.map { winnerName(it, state) }.joinToString(", ")
    val iWon = winners.any { it.userId == state.myId }
    PqCard(bg = PqColors.Gold, modifier = Modifier.fillMaxWidth().testTag(StudyTags.WINNER_BANNER)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = PqColors.Ink)
                Text(
                    text = stringResource(if (iWon) R.string.study_winner_you else R.string.study_winner_title),
                    style = PqType.CardTitle,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Text(
                text = pluralStringResource(R.plurals.study_winner_line, winners.size, names, pluralStringResource(R.plurals.study_points, winners.first().points, winners.first().points)),
                style = PqType.Body
            )
            Text(
                text = stringResource(R.string.study_winner_dismiss),
                style = PqType.MetaStrong.copy(color = PqColors.Ink),
                modifier = Modifier
                    .testTag(StudyTags.WINNER_DISMISS)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .heightIn(min = 48.dp)
                    .padding(vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun winnerName(winner: com.pixelquest.app.study.domain.WeekWinner, state: StudyPartyUiState): String {
    val name = winner.username ?: state.members.firstOrNull { it.userId == winner.userId }?.username
    return name?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)
}

/** A week gone by: its dates, who won, and the player's points. */
@Composable
private fun PastWeekRow(week: PastWeek, members: List<com.pixelquest.app.party.domain.PartyMember>) {
    val format = DateTimeFormatter.ofPattern("EEE d MMM", LocalConfiguration.current.locales[0])
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(StudyTags.pastWeek(week.weekStart.toString()))
            .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = stringResource(R.string.study_board_week, week.weekStart.format(format), week.weekStart.plusDays(6).format(format)), style = PqType.BodyStrong)
        val names = week.winners.map { w ->
            (w.username ?: members.firstOrNull { it.userId == w.userId }?.username)?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)
        }
        Text(
            text = if (names.isEmpty()) stringResource(R.string.study_past_no_winner)
            else pluralStringResource(R.plurals.study_past_winner, names.size, names.joinToString(", "), pluralStringResource(R.plurals.study_points, week.winners.first().points, week.winners.first().points)),
            style = PqType.Meta.copy(color = PqColors.Ink2)
        )
        Text(text = stringResource(R.string.study_past_mine, pluralStringResource(R.plurals.study_points, week.myPoints, week.myPoints)), style = PqType.Meta)
    }
}

/** One member on the board (mock 4J): place in its medal, name and breakdown, points. */
@Composable
fun BoardRow(line: BoardLine) {
    val name = line.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)
    val shownName = if (line.isMe) stringResource(R.string.study_board_you, name) else name
    val points = pluralStringResource(R.plurals.study_points, line.points, line.points)
    val said = stringResource(R.string.study_board_line, line.place, shownName, points) + breakdownText(line)
    val medal = when (medalFor(line.place, line.points)) {
        Medal.GOLD -> PqColors.Gold
        Medal.SILVER -> PqColors.Sunken
        Medal.BRONZE -> PqColors.Amber
        null -> PqColors.Paper
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(StudyTags.boardRow(line.userId))
            .clearAndSetSemantics { contentDescription = said }
            .background(if (line.isMe) PqColors.Sky else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(30.dp).comic(bg = medal, radius = 15.dp, border = PqDimens.BorderThin, shadow = 0.dp).testTag(StudyTags.boardPlace(line.userId))
        ) {
            Text(text = line.place.toString(), style = PqType.Button)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = shownName, style = PqType.BodyStrong)
            line.score?.let { Text(text = breakdownText(line).trim(), style = PqType.Meta.copy(color = PqColors.Ink2)) }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = line.points.toString(), style = PqType.Number, modifier = Modifier.testTag(StudyTags.boardPoints(line.userId)))
            Text(text = stringResource(R.string.study_points_short), style = PqType.Meta.copy(color = PqColors.Ink2))
        }
    }
}

/** Where a line's points came from: items, early bonus, study time. Empty for a member with none. */
@Composable
private fun breakdownText(line: BoardLine): String {
    val score = line.score ?: return ""
    return " " + stringResource(
        R.string.study_board_breakdown,
        pluralStringResource(R.plurals.study_items_done, score.itemsCompleted, score.itemsCompleted),
        score.bonusPoints,
        pluralStringResource(R.plurals.study_minutes, score.studyMinutes, score.studyMinutes)
    )
}

/** How points are earned, from [StudyPointsConfig] (mock 4J): a line a rule, then that they're the party's own. */
@Composable
private fun PointsRules() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().testTag(StudyTags.POINTS_RULES)) {
        PqGroupLabel(stringResource(R.string.study_points_title), Modifier.semantics { heading() })
        PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
            Column {
                listOf(
                    Icons.Rounded.Description to stringResource(R.string.study_points_assignment, StudyPointsConfig.ASSIGNMENT),
                    Icons.Rounded.Quiz to stringResource(R.string.study_points_quiz, StudyPointsConfig.QUIZ),
                    Icons.Rounded.Bolt to stringResource(R.string.study_points_early, StudyPointsConfig.EARLY_BONUS),
                    Icons.Rounded.Timer to stringResource(R.string.study_points_study, StudyPointsConfig.MINUTES_PER_POINT, StudyPointsConfig.DAILY_STUDY_MAX)
                ).forEach { (icon, rule) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
                        Text(text = rule, style = PqType.Body, modifier = Modifier.weight(1f))
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
                }
                Text(text = stringResource(R.string.study_points_not_xp), style = PqType.Meta, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
            }
        }
    }
}
