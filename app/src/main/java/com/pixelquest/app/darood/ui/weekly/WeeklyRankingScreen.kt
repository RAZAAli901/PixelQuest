package com.pixelquest.app.darood.ui.weekly

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.DaroodWeeks
import com.pixelquest.app.darood.domain.WeekResult
import com.pixelquest.app.darood.sync.WeekRankingText
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object WeeklyScreenTags {
    const val SCREEN = "darood_weeks_screen"
    const val TREND = "darood_weeks_trend"
    const val TREND_SUMMARY = "darood_weeks_trend_summary"
    fun week(start: java.time.LocalDate) = "darood_weeks_week_$start"
}

@Composable
fun WeeklyRankingScreen(onNavigateBack: () -> Unit, viewModel: WeeklyRankingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    WeeklyRankingContent(state, onNavigateBack, onRetry = viewModel::refresh)
}

/**
 * The weekly ranking (J079): the player's place over the last 8 closed weeks as a small chart,
 * then each week, newest first, its places shown on a tap (the newest open to begin with).
 */
@Composable
fun WeeklyRankingContent(state: WeeklyRankingState, onNavigateBack: () -> Unit, onRetry: () -> Unit = {}) {
    val weeks = state.weeks.take(DaroodWeeks.HISTORY_WEEKS)
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(PqColors.Ground).testTag(WeeklyScreenTags.SCREEN)) {
        PqTopBar(stringResource(R.string.darood_week_title), onBack = onNavigateBack, modifier = Modifier.padding(horizontal = PqDimens.ScreenPad).padding(top = 8.dp))
        if (weeks.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Text(
                    text = stringResource(if (state.offline) R.string.darood_week_offline else R.string.darood_week_none),
                    style = PqType.Body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(WeeklyRankingTags.EMPTY)
                )
                if (state.offline) PqButton(text = stringResource(R.string.darood_race_retry), onClick = onRetry)
            }
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(PqDimens.Gap)
        ) {
            if (state.me != null) item(key = "trend") { PlaceTrend(weeks, state.me) }
            items(weeks, key = { it.weekStart.toString() }) { week ->
                val isOpen = (open ?: weeks.first().weekStart.toString()) == week.weekStart.toString()
                WeekSection(week, state.me, state.names, isOpen, onToggle = { open = if (isOpen) "" else week.weekStart.toString() })
            }
        }
    }
}

/** A closed week: its dates and the player's place; tapped, everyone's places. */
@Composable
private fun WeekSection(week: WeekResult, me: String?, names: Map<String, String?>, open: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val shown = stringResource(if (open) R.string.darood_week_shown else R.string.darood_week_hidden)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(WeeklyScreenTags.week(week.weekStart))
            .comic(bg = PqColors.Paper, radius = PqDimens.Radius, shadow = 0.dp)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .semantics(mergeDescendants = true) { stateDescription = shown }
        ) {
            Text(
                text = stringResource(R.string.darood_week_dates, week.weekStart.format(dates), week.weekStart.plusDays(6).format(dates)),
                style = PqType.BodyStrong
            )
            val mine = me?.let { WeekRankingText.line(context, week, it) } ?: stringResource(R.string.darood_week_not_ranked)
            Text(text = mine, style = PqType.Meta)
        }
        if (open) WeekPlaces(week, me, names)
    }
}

/**
 * The player's place week by week, oldest on the left: the taller the bar, the higher the place
 * (first fills it). A week they weren't ranked in is an empty slot. A line under it says how the
 * last two compare, kindly whichever way it went.
 */
@Composable
private fun PlaceTrend(weeks: List<WeekResult>, me: String) {
    val context = LocalContext.current
    val oldestFirst = weeks.asReversed()
    val dates = DateTimeFormatter.ofPattern("d MMM")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().testTag(WeeklyScreenTags.TREND)) {
        PqFieldLabel(stringResource(R.string.darood_week_trend), Modifier.semantics { heading() })
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().height(120.dp)
        ) {
            oldestFirst.forEach { week ->
                val place = week.placeOf(me)
                val ordinal = place?.let { WeekRankingText.ordinal(context, it.position) }
                val said = stringResource(R.string.darood_week_trend_said, week.weekStart.format(dates), ordinal ?: stringResource(R.string.darood_week_not_ranked))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics { contentDescription = said }
                ) {
                    Text(text = ordinal ?: "-", style = PqType.Meta)
                    // 1st of 8 fills the bar; 8th of 8 is a sliver, never nothing.
                    val share = place?.let { (week.members - it.position + 1).toFloat() / week.members.coerceAtLeast(1) } ?: 0f
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((8 + 72 * share).dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .background(if (place == null) PqColors.Sunken else if (week == weeks.first()) PqColors.Gold else PqColors.Sky)
                    )
                    Text(text = week.weekStart.format(dates), style = PqType.Meta, maxLines = 1)
                }
            }
        }
        trendSummary(context, weeks, me)?.let {
            Text(text = it, style = PqType.Body, modifier = Modifier.testTag(WeeklyScreenTags.TREND_SUMMARY))
        }
    }
}

/** The last two weeks the player was ranked in, compared: up, the same, or down (kindly). */
internal fun trendSummary(context: android.content.Context, weeks: List<WeekResult>, me: String): String? {
    val ranked = weeks.mapNotNull { week -> week.placeOf(me)?.position }
    if (ranked.size < 2) return null
    val (now, before) = ranked[0] to ranked[1]
    val nowText = WeekRankingText.ordinal(context, now)
    val beforeText = WeekRankingText.ordinal(context, before)
    return when {
        now < before -> context.getString(R.string.darood_week_trend_up, beforeText, nowText)
        now == before -> context.getString(R.string.darood_week_trend_same, nowText)
        else -> context.getString(R.string.darood_week_trend_down, beforeText, nowText)
    }
}
