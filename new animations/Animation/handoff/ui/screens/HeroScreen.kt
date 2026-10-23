package com.pixelquest.ui.screens

import com.pixelquest.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.ui.components.*
import com.pixelquest.ui.theme.PixelQuestTheme
import com.pixelquest.ui.theme.PqColors
import com.pixelquest.ui.theme.PqDimens
import com.pixelquest.ui.theme.PqType

/** Hero tab = old Profile + old Stats + the drawer's Leaderboard/History/Account/Settings. */
enum class HeroSegment { Profile, Stats }

data class StatsUi(
    val xpLast7Days: Int,
    val perfectDays: Int,
    val heatmap: List<DayStatus>,            // oldest → newest, length multiple of 7 (Mon-first weeks)
    val weekly: List<Pair<String, Int>>,     // ("W-3", 0) … ("Now", 14) — percent complete
    val targetPercent: Int,
)

data class HeroUiState(
    val segment: HeroSegment,
    val name: String,
    val avatar: AvatarClass,
    val rank: String,          // "Bronze"
    val difficulty: String,    // "Medium"
    val level: Int,
    val perfectDays: Int,      // toward next level
    val perfectDaysNeeded: Int,
    val totalXp: Int,
    val streak: Int,
    val questsDone: Int,
    val leaderboardLocked: Boolean,
    val stats: StatsUi,
)

@Composable
fun HeroScreen(
    state: HeroUiState,
    onSegmentChange: (HeroSegment) -> Unit,
    onSettings: () -> Unit,
    onChangeAvatar: () -> Unit,
    onDifficulty: () -> Unit,
    onQuestHistory: () -> Unit,
    onLevelHistory: () -> Unit,
    onLeaderboard: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        Modifier.fillMaxSize().background(PqColors.Ground),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PqTopBar("Hero") { PqIconButton(Icons.Rounded.Settings, "Settings", onSettings) } }
        item { PqSegmented(listOf("Profile", "Stats"), state.segment.ordinal, { onSegmentChange(HeroSegment.entries[it]) }) }
        if (state.segment == HeroSegment.Profile) {
            item { HeroCard(state, onChangeAvatar) }
            item {
                PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
                    Column {
                        PqListRow(Icons.Rounded.Face, "Change avatar", onChangeAvatar, state.avatar.label)
                        PqListRow(Icons.Rounded.Tune, "Difficulty", onDifficulty, state.difficulty)
                        PqListRow(Icons.Rounded.History, "Quest history", onQuestHistory)
                        PqListRow(Icons.Rounded.Timeline, "Level history", onLevelHistory)
                        PqListRow(Icons.Rounded.Leaderboard, "Leaderboard", onLeaderboard, if (state.leaderboardLocked) "Locked" else null, showDivider = false)
                    }
                }
            }
        } else {
            item { StatsSummary(state.stats) }
            item { Heatmap(state.stats.heatmap) }
            item { WeeklyTrend(state.stats) }
        }
    }
}

@Composable
private fun HeroCard(state: HeroUiState, onChangeAvatar: () -> Unit) {
    PqCard(shadow = PqDimens.ShadowLg) {
        Column {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickable(onClick = onChangeAvatar)) {
                    PixelAvatar(state.avatar, 80.dp)
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(6.dp, 6.dp).size(28.dp).clip(PqCircle).background(PqColors.Gold).border(PqDimens.BorderThin, PqColors.Ink, PqCircle),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Edit, "Change avatar", tint = PqColors.Ink, modifier = Modifier.size(16.dp)) }
                }
                Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.name.uppercase(), style = PqType.display(34))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallChip(state.rank, PqColors.Amber); SmallChip(state.difficulty, PqColors.Sky)
                    }
                }
            }
            InkRule()
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    Text("LVL ${state.level} → ${state.level + 1}", style = PqType.Button, modifier = Modifier.weight(1f))
                    Text("${state.perfectDays} / ${state.perfectDaysNeeded} perfect days", style = PqType.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
                }
                PqSegmentBar(state.perfectDaysNeeded, state.perfectDays, fill = if (isPixelSkin) PqColors.Progress else PqColors.Gold)
            }
            InkRule()
            Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                StatCell("${state.totalXp}", "TOTAL XP", Modifier.weight(1f))
                Box(Modifier.width(PqDimens.Border).fillMaxHeight().background(PqColors.Ink))
                StatCell("${state.streak}", "STREAK", Modifier.weight(1f))
                Box(Modifier.width(PqDimens.Border).fillMaxHeight().background(PqColors.Ink))
                StatCell("${state.questsDone}", "QUESTS", Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun InkRule() = Box(Modifier.fillMaxWidth().height(PqDimens.Border).background(PqColors.Ink))

@Composable
private fun SmallChip(text: String, bg: Color) {
    Text(
        text.uppercase(), style = PqType.Label.copy(letterSpacing = 0.4.sp),
        modifier = Modifier.clip(PqCircle).background(bg).border(PqDimens.BorderThin, PqColors.Ink, PqCircle).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(value, style = PqType.Number)
        Text(label, style = PqType.Meta.copy(fontSize = 11.bsp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}

@Composable
private fun StatsSummary(s: StatsUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PqCard(Modifier.weight(1f), bg = PqColors.Sky, radius = PqDimens.Radius) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) { Text("${s.xpLast7Days} XP", style = PqType.display(34)); Text("Last 7 days", style = PqType.MetaStrong) }
        }
        PqCard(Modifier.weight(1f), bg = PqColors.Gold, radius = PqDimens.Radius) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) { Text("${s.perfectDays}", style = PqType.display(34)); Text("Perfect days", style = PqType.MetaStrong) }
        }
    }
}

@Composable
private fun Heatmap(days: List<DayStatus>) {
    PqCard(radius = PqDimens.Radius) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("QUEST ACTIVITY", style = PqType.Label.copy(fontSize = 13.lsp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("M", "", "W", "", "F", "", "S").forEach { Text(it, style = PqType.Meta.copy(fontSize = 9.bsp, lineHeight = 13.bsp), modifier = Modifier.height(13.dp)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    days.chunked(7).forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            week.forEach { d ->
                                Box(Modifier.size(13.dp).clip(pqShape(3.dp)).background(d.color).border(1.5.dp, PqColors.Ink, pqShape(3.dp)))
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(DayStatus.Perfect to "Perfect", DayStatus.Partial to "Partial", DayStatus.Missed to "Missed", DayStatus.None to "None").forEach { (d, l) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(pqShape(2.dp)).background(d.color).border(1.5.dp, PqColors.Ink, pqShape(2.dp)))
                        Text(" $l", style = PqType.Meta.copy(fontSize = 11.bsp, color = PqColors.Ink2))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTrend(s: StatsUi) {
    PqCard(radius = PqDimens.Radius) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("WEEKLY COMPLETION", style = PqType.Label.copy(fontSize = 13.lsp))
            Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                s.weekly.forEachIndexed { i, (label, pct) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$pct%", style = PqType.Button.copy(fontSize = 11.lsp), textAlign = TextAlign.Center)
                        val h = (pct / s.targetPercent.toFloat() * 56f).coerceIn(4f, 56f).dp
                        Box(
                            Modifier.fillMaxWidth().height(h).clip(pqShapeTop(5.dp))
                                .background(if (i == s.weekly.lastIndex) PqColors.Progress else PqColors.Lilac)
                                .border(PqDimens.BorderThin, PqColors.Ink, pqShapeTop(5.dp))
                        )
                        Text(label, style = PqType.Meta.copy(fontSize = 11.bsp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun HeroPreview() = PixelQuestTheme {
    val heat = List(63) { DayStatus.None } + List(28) { listOf(DayStatus.Missed, DayStatus.Partial, DayStatus.Perfect)[it % 3] }
    HeroScreen(
        HeroUiState(
            HeroSegment.Profile, "CBF", AvatarClass.Hero, "Bronze", "Medium", 1, 2, 7, 410, 0, 12, true,
            StatsUi(60, 5, heat, listOf("W-3" to 0, "W-2" to 6, "W-1" to 28, "Now" to 14), 70),
        ),
        {}, {}, {}, {}, {}, {}, {},
    )
}
