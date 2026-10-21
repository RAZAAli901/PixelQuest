package com.pixelquest.app.darood.ui.weekly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.WeekResult
import com.pixelquest.app.darood.sync.WeekRankingText
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object WeeklyRankingTags {
    const val SHEET = "darood_weekly_sheet"
    const val EMPTY = "darood_weekly_empty"
    const val ALL_WEEKS = "darood_weekly_all"
    const val OPEN = "darood_weekly_open"
    fun row(userId: String) = "darood_weekly_row_$userId"
}

/**
 * The Weekly ranking sheet in a Darood Party's chat (J077): the last closed week's places, the
 * player's own on gold with its kind line. [onSeeAll] opens the ranking screen's 8 weeks.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WeeklyRankingSheet(state: WeeklyRankingState, onDismiss: () -> Unit, onSeeAll: (() -> Unit)? = null) {
    PqSheet(onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag(WeeklyRankingTags.SHEET)
        ) {
            Text(
                text = stringResource(R.string.darood_week_title).uppercase(),
                style = PqType.Title.copy(color = PqColors.Title),
                modifier = Modifier.semantics { heading() }
            )
            val week = state.latest
            if (week == null) {
                Text(
                    text = stringResource(if (state.offline) R.string.darood_week_offline else R.string.darood_week_none),
                    style = PqType.Body,
                    modifier = Modifier.testTag(WeeklyRankingTags.EMPTY)
                )
            } else {
                WeekPlaces(week, state.me, state.names)
            }
            if (onSeeAll != null && state.weeks.isNotEmpty()) {
                PqButton(
                    text = stringResource(R.string.darood_week_all),
                    onClick = onSeeAll,
                    style = PqButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth().testTag(WeeklyRankingTags.ALL_WEEKS)
                )
            }
        }
    }
}

/** One week's places: its dates, the player's line and kind line, then everyone in order. */
@Composable
fun WeekPlaces(week: WeekResult, me: String?, names: Map<String, String?>) {
    val context = LocalContext.current
    val dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.darood_week_dates, week.weekStart.format(dates), week.weekStart.plusDays(6).format(dates)),
            style = PqType.Meta
        )
        if (me != null) {
            WeekRankingText.line(context, week, me)?.let { Text(text = it, style = PqType.BodyStrong) }
            WeekRankingText.tone(context, week, me)?.let { Text(text = it, style = PqType.Body) }
        }
        week.places.forEach { place ->
            val mine = place.userId == me
            val name = if (mine) stringResource(R.string.darood_week_you) else names[place.userId] ?: stringResource(R.string.darood_someone)
            val ordinal = WeekRankingText.ordinal(context, place.position)
            val said = stringResource(R.string.darood_week_row_said, ordinal, name, formatCount(place.weekTotal))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WeeklyRankingTags.row(place.userId))
                    .clearAndSetSemantics { contentDescription = said }
                    .comic(bg = if (mine) PqColors.Gold else PqColors.Paper, radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(text = ordinal, style = PqType.Number)
                InitialAvatar(name, 30.dp)
                Text(text = name, style = PqType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(text = formatCount(place.weekTotal), style = PqType.Number)
            }
        }
    }
}
