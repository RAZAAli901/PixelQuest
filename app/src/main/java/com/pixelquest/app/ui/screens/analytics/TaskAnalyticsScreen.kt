package com.pixelquest.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.PerTaskStats
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape
import java.time.format.DateTimeFormatter

/**
 * One quest's stats (Quests > History, a quest tapped), in UI v2's language: the quest, four numbers
 * on their colours, and its last 14 days, done or missed.
 */
@Composable
fun TaskAnalyticsContent(
    task: TaskEntity?,
    stats: PerTaskStats,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 24.dp)
    ) {
        PqTopBar("Quest stats", onBack = onBackClick)
        if (task != null) {
            val category = QuestCategory.from(task.category.name)
            PqCard(shadow = PqDimens.ShadowLg) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    CategoryTile(category.icon, category.color)
                    Column {
                        Text(text = task.name.uppercase(), style = PqType.CardTitle)
                        Text(text = task.category.displayName, style = PqType.Meta)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${stats.completionCount}", "Times done", PqColors.Sky, Modifier.weight(1f))
            StatTile("${(stats.completionRate * 100).toInt()}%", "Done rate", PqColors.Lilac, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${stats.currentStreak} days", "Streak now", PqColors.Amber, Modifier.weight(1f))
            StatTile("${stats.longestStreak} days", "Best streak", PqColors.Gold, Modifier.weight(1f))
        }
        if (stats.recentHistory.isNotEmpty()) {
            PqGroupLabel("Last 14 days")
            RecentDays(stats.recentHistory.takeLast(14))
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, bg: Color, modifier: Modifier) {
    Column(
        modifier
            .comic(bg = bg, radius = 12.dp, border = PqDimens.BorderThin, shadow = 0.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { }
    ) {
        Text(text = value, style = PqType.Number)
        Text(text = label, style = PqType.Meta.copy(color = PqColors.Ink))
    }
}

/** The quest's recent days as squares, filled where it was done; each read as "done" or "missed", then the day. */
@Composable
private fun RecentDays(days: List<Pair<java.time.LocalDate, Boolean>>) {
    val dayFormatter = DateTimeFormatter.ofPattern("d")
    val shape = pqShape(4.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        days.forEach { (date, done) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = (if (done) "done" else "missed") + ", " + date }
            ) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(shape)
                        .background(if (done) PqColors.Progress else PqColors.Sunken)
                        .border(PqDimens.BorderThin, if (done) PqColors.Ink else PqColors.Divider, shape)
                )
                Text(text = date.format(dayFormatter), style = PqType.Meta)
            }
        }
    }
}

@Composable
fun TaskAnalyticsScreen(
    viewModel: TaskAnalyticsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    TaskAnalyticsContent(task = state.task, stats = state.stats, onBackClick = onBackClick)
}
