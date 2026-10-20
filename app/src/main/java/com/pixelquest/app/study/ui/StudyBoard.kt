package com.pixelquest.app.study.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.domain.StudyPointsConfig
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
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
        Text(
            text = stringResource(R.string.study_board_week, state.weekStart.format(format), state.weekStart.plusDays(6).format(format)),
            style = MaterialTheme.typography.titleSmall,
            color = PixelTheme.colors.primaryText,
            modifier = Modifier.semantics { heading() }
        )
    }
    if (state.board.none { it.points > 0 }) {
        item(key = "board_empty") {
            Text(
                text = stringResource(if (state.loaded) R.string.study_board_empty else R.string.study_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = PixelTheme.colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().testTag(StudyTags.EMPTY)
            )
        }
    } else {
        items(state.board, key = { "board_" + it.userId }) { line -> BoardRow(line) }
    }
    item(key = "board_rules") { PointsRules() }
}

/** One member on the board: place, name, points, and their breakdown. */
@Composable
fun BoardRow(line: BoardLine) {
    val colors = PixelTheme.colors
    val name = line.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)
    val shownName = if (line.isMe) stringResource(R.string.study_board_you, name) else name
    val points = pluralStringResource(R.plurals.study_points, line.points, line.points)
    val said = stringResource(R.string.study_board_line, line.place, shownName, points) + breakdownText(line)
    PixelCard(
        variant = if (line.isMe) PixelPanelVariant.BEIGE else PixelPanelVariant.BORDER,
        contentPadding = 12.dp,
        modifier = Modifier.fillMaxWidth().testTag(StudyTags.boardRow(line.userId))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clearAndSetSemantics { contentDescription = said }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.study_board_place, line.place),
                    style = MaterialTheme.typography.titleMedium,
                    color = inkOnPanel(colors.primaryText),
                    modifier = Modifier.widthIn(min = 32.dp).testTag(StudyTags.boardPlace(line.userId))
                )
                Text(
                    text = shownName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (line.isMe) FontWeight.Bold else null,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(text = points, style = MaterialTheme.typography.titleSmall, color = inkOnPanel(colors.primaryText), modifier = Modifier.testTag(StudyTags.boardPoints(line.userId)))
            }
            line.score?.let {
                Text(text = breakdownText(line).trim(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
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

/** How points are earned, from [StudyPointsConfig]. */
@Composable
private fun PointsRules() {
    val colors = PixelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().testTag(StudyTags.POINTS_RULES)) {
        Text(text = stringResource(R.string.study_points_title), style = MaterialTheme.typography.titleSmall, color = colors.primaryText, modifier = Modifier.semantics { heading() })
        listOf(
            stringResource(R.string.study_points_assignment, StudyPointsConfig.ASSIGNMENT),
            stringResource(R.string.study_points_quiz, StudyPointsConfig.QUIZ),
            stringResource(R.string.study_points_early, StudyPointsConfig.EARLY_BONUS),
            stringResource(R.string.study_points_study, StudyPointsConfig.MINUTES_PER_POINT, StudyPointsConfig.DAILY_STUDY_MAX),
            stringResource(R.string.study_points_not_xp)
        ).forEach { Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.onSurface) }
    }
}
