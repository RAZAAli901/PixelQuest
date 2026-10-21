package com.pixelquest.app.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.AvatarTierCalculator
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.DayStatus
import com.pixelquest.app.ui.components.PixelAvatar
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqListRow
import com.pixelquest.app.ui.components.PqSegmentBar
import com.pixelquest.app.ui.components.PqSegmented
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.screens.profile.ProfileUiState
import com.pixelquest.app.ui.screens.profile.ProfileViewModel
import com.pixelquest.app.ui.screens.stats.NamazStats
import com.pixelquest.app.ui.screens.stats.StatsUiState
import com.pixelquest.app.ui.screens.stats.StatsViewModel
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.isPixelSkin
import com.pixelquest.app.ui.theme.lsp
import com.pixelquest.app.ui.theme.pqShape
import com.pixelquest.app.ui.theme.pqShapeTop
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Hero's two halves (mocks 2G and 2H): who the player is, and how they've done. */
enum class HeroSegment { Profile, Stats }

/** Tags for tests. */
object HeroTags {
    const val SCREEN = "hero_screen"
    const val CARD = "hero_card"
    const val SETTINGS = "hero_settings"
    const val HEATMAP = "hero_heatmap"
    const val WEEKLY = "hero_weekly"
}

/**
 * How the player has done (mock 2H).
 *
 * @param heatmap Monday-first weeks, oldest first, today's week last (days to come are [DayStatus.None]).
 * @param weekly the last four weeks' share of days with quests done, in percent, "now" last.
 */
data class StatsUi(
    val xpLast7Days: Int,
    val perfectDays: Int,
    val heatmap: List<DayStatus>,
    val weekly: List<Int>,
    val targetPercent: Int,
    val longestStreak: Int = 0,
    val completionPercent: Int = 0,
    val namaz: NamazStats? = null,
)

data class HeroUiState(
    val name: String,
    val avatar: AvatarClass,
    val tier: AvatarTier,
    val difficulty: DifficultyLevel,
    val level: Int,
    val perfectDays: Int,
    val perfectDaysNeeded: Int,
    val totalXp: Int,
    val streak: Int,
    val questsDone: Int,
    val activeQuests: Int,
    val leaderboardLocked: Boolean,
    val stats: StatsUi,
    val isSimpleMode: Boolean = false,
    /** The Darood Party username, without the @; null hides it. */
    val username: String? = null,
)

/** How many weeks the activity heatmap shows: as many as fit a 360dp phone. */
const val HEATMAP_WEEKS = 17

/** The heatmap's days, Monday-first weeks ending with today's week; days to come show as nothing. */
fun heatmapDays(statuses: Map<LocalDate, DailyStatus>, today: LocalDate, weeks: Int = HEATMAP_WEEKS): List<DayStatus> {
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((weeks - 1).toLong())
    return (0 until weeks * 7).map { i ->
        val day = start.plusDays(i.toLong())
        when {
            day.isAfter(today) -> DayStatus.None
            else -> when (statuses[day]) {
                DailyStatus.PERFECT -> DayStatus.Perfect
                DailyStatus.PARTIAL -> DayStatus.Partial
                DailyStatus.MISSED -> DayStatus.Missed
                else -> DayStatus.None
            }
        }
    }
}

/** The profile's and the stats' states as Hero shows them. */
fun heroUiOf(profile: ProfileUiState, stats: StatsUiState, today: LocalDate): HeroUiState {
    val level = profile.profile?.level ?: 1
    val difficulty = profile.difficulty?.difficultyLevel ?: stats.difficultyLevel
    val lastWeek = stats.heatmapStatusMap.filterKeys { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) }
    return HeroUiState(
        name = profile.profile?.username.orEmpty(),
        avatar = AvatarClass.from(profile.profile?.avatarId),
        tier = AvatarTierCalculator.calculateTier(level),
        difficulty = difficulty,
        level = level,
        perfectDays = profile.profile?.perfectDaysTowardNextLevel ?: 0,
        perfectDaysNeeded = DifficultyMode.daysRequiredPerLevel(profile.difficulty),
        totalXp = profile.profile?.totalXp ?: 0,
        streak = profile.streak?.currentStreak ?: 0,
        questsDone = profile.totalTasksCompleted,
        activeQuests = profile.activeTasksCount,
        leaderboardLocked = !profile.signedIn,
        isSimpleMode = profile.isSimpleMode,
        username = profile.daroodUsername,
        stats = StatsUi(
            xpLast7Days = profile.xpLastWeek,
            perfectDays = lastWeek.values.count { it == DailyStatus.PERFECT },
            heatmap = heatmapDays(stats.heatmapStatusMap, today),
            weekly = stats.weeklyTrend.map { (_, rate) -> (rate * 100).toInt() },
            targetPercent = (DifficultyMode.getPerfectDayThreshold(difficulty) * 100).toInt(),
            longestStreak = stats.longestStreak,
            completionPercent = (stats.overallCompletionRate * 100).toInt(),
            namaz = stats.namaz,
        ),
    )
}

/** The Hero tab's route: the profile's and the stats' view models, the half chosen kept across rotation. */
@Composable
fun HeroScreen(
    onSettings: () -> Unit,
    onChangeAvatar: () -> Unit,
    onDifficulty: () -> Unit,
    onQuestHistory: () -> Unit,
    onLevelHistory: () -> Unit,
    onLeaderboard: () -> Unit,
    onParties: () -> Unit,
    onUsername: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    statsViewModel: StatsViewModel = hiltViewModel(),
) {
    val profile by profileViewModel.uiState.collectAsState()
    val stats by statsViewModel.uiState.collectAsState()
    var segment by rememberSaveable { mutableStateOf(HeroSegment.Profile) }
    // Built once per change of the profile or stats, not on every recomposition.
    val state = remember(profile, stats) { heroUiOf(profile, stats, LocalDate.now()) }
    HeroContent(
        state = state,
        segment = segment,
        onSegmentChange = { segment = it },
        onSettings = onSettings,
        onChangeAvatar = onChangeAvatar,
        onDifficulty = onDifficulty,
        onQuestHistory = onQuestHistory,
        onLevelHistory = onLevelHistory,
        onLeaderboard = onLeaderboard,
        onParties = onParties,
        onUsername = onUsername,
    )
}

@Composable
fun HeroContent(
    state: HeroUiState,
    segment: HeroSegment,
    onSegmentChange: (HeroSegment) -> Unit,
    onSettings: () -> Unit,
    onChangeAvatar: () -> Unit,
    onDifficulty: () -> Unit,
    onQuestHistory: () -> Unit,
    onLevelHistory: () -> Unit,
    onLeaderboard: () -> Unit,
    onParties: () -> Unit = {},
    onUsername: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val simple = state.isSimpleMode
    LazyColumn(
        modifier.fillMaxSize().background(PqColors.Ground).testTag(HeroTags.SCREEN),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "top") {
            PqTopBar(stringResource(if (simple) R.string.pq_hero_title_simple else R.string.pq_hero_title)) {
                PqIconButton(Icons.Rounded.Settings, stringResource(R.string.pq_hero_settings), onSettings, Modifier.testTag(HeroTags.SETTINGS))
            }
        }
        item(key = "segments") {
            PqSegmented(
                listOf(stringResource(R.string.pq_hero_profile), stringResource(R.string.pq_hero_stats)),
                segment.ordinal, { onSegmentChange(HeroSegment.entries[it]) },
            )
        }
        if (segment == HeroSegment.Profile) {
            item(key = "card") { HeroCard(state, onChangeAvatar, onUsername) }
            item(key = "list") {
                PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
                    Column {
                        PqListRow(Icons.Rounded.Face, stringResource(R.string.pq_hero_change_avatar), onChangeAvatar, stringResource(state.avatar.label))
                        // Simple Mode keeps the difficulty it has (DifficultyLockUnderSimpleMode).
                        PqListRow(
                            Icons.Rounded.Tune, stringResource(R.string.pq_hero_difficulty),
                            if (simple) ({}) else onDifficulty,
                            if (simple) stringResource(R.string.pq_hero_locked) else DifficultyMode.getDisplayName(state.difficulty),
                        )
                        PqListRow(Icons.Rounded.History, stringResource(if (simple) R.string.pq_hero_quest_history_simple else R.string.pq_hero_quest_history), onQuestHistory)
                        if (!simple) PqListRow(Icons.Rounded.Timeline, stringResource(R.string.pq_hero_level_history), onLevelHistory)
                        PqListRow(Icons.Rounded.Groups, stringResource(R.string.pq_hero_parties), onParties)
                        PqListRow(
                            Icons.Rounded.Leaderboard, stringResource(R.string.pq_hero_leaderboard), onLeaderboard,
                            if (state.leaderboardLocked) stringResource(R.string.pq_hero_locked) else null, showDivider = false,
                        )
                    }
                }
            }
        } else {
            item(key = "summary") { StatsSummary(state.stats, simple) }
            item(key = "heatmap") { Heatmap(state.stats.heatmap, simple) }
            item(key = "weekly") { WeeklyTrend(state.stats) }
            item(key = "more") { MoreStats(state, simple) }
        }
    }
}

@Composable
private fun HeroCard(state: HeroUiState, onChangeAvatar: () -> Unit, onUsername: () -> Unit) {
    val simple = state.isSimpleMode
    PqCard(Modifier.testTag(HeroTags.CARD), shadow = PqDimens.ShadowLg) {
        Column {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                val change = stringResource(R.string.pq_hero_change_avatar)
                Box(Modifier.comicPressable(onChangeAvatar, radius = 13.dp, shadow = 0.dp).clearAndSetSemantics { contentDescription = change; role = Role.Button }) {
                    PixelAvatar(state.avatar, 80.dp)
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(6.dp, 6.dp).size(28.dp).clip(PqCircle).background(PqColors.Gold).border(PqDimens.BorderThin, PqColors.Ink, PqCircle),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Edit, null, tint = PqColors.Ink, modifier = Modifier.size(16.dp)) }
                }
                Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.name.uppercase(), style = PqType.display(34), maxLines = 1)
                    // The Darood Party username, once the account has one: tap to change it.
                    state.username?.let { handle ->
                        val shown = stringResource(R.string.darood_username_handle, handle)
                        // TalkBack hears what the name is for, not just "at aria k".
                        val spoken = stringResource(R.string.darood_username_profile_label) + ", " + shown
                        Text(
                            shown,
                            style = PqType.MetaStrong,
                            maxLines = 1,
                            modifier = Modifier
                                .clickable(onClickLabel = stringResource(R.string.darood_username_profile_change), onClick = onUsername)
                                .semantics { contentDescription = spoken }
                                .testTag(com.pixelquest.app.darood.ui.DaroodTestTags.PROFILE_USERNAME),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!simple) SmallChip(stringResource(tierLabel(state.tier)), PqColors.Amber)
                        SmallChip(DifficultyMode.getDisplayName(state.difficulty), PqColors.Sky)
                    }
                }
            }
            if (!simple) {
                InkRule()
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text(stringResource(R.string.pq_hero_level_next, state.level, state.level + 1), style = PqType.Button, modifier = Modifier.weight(1f))
                        Text(
                            pluralStringResource(R.plurals.pq_hero_perfect_days_progress, state.perfectDaysNeeded, state.perfectDays, state.perfectDaysNeeded),
                            style = PqType.Meta.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                    PqSegmentBar(state.perfectDaysNeeded, state.perfectDays, fill = if (isPixelSkin) PqColors.Progress else PqColors.Gold)
                }
            }
            InkRule()
            Row(Modifier.height(IntrinsicSize.Min)) {
                if (simple) {
                    StatCell("${state.questsDone}", stringResource(R.string.pq_hero_tasks_done), Modifier.weight(1f))
                    CellRule()
                    StatCell("${state.activeQuests}", stringResource(R.string.pq_hero_active), Modifier.weight(1f))
                } else {
                    StatCell("${state.totalXp}", stringResource(R.string.pq_hero_total_xp), Modifier.weight(1f))
                    CellRule()
                    StatCell("${state.streak}", stringResource(R.string.pq_hero_streak), Modifier.weight(1f))
                    CellRule()
                    StatCell("${state.questsDone}", stringResource(R.string.pq_hero_quests), Modifier.weight(1f))
                }
            }
        }
    }
}

private fun tierLabel(tier: AvatarTier): Int = when (tier) {
    AvatarTier.BRONZE -> R.string.pq_tier_bronze
    AvatarTier.SILVER -> R.string.pq_tier_silver
    AvatarTier.GOLD -> R.string.pq_tier_gold
}

@Composable private fun InkRule() = Box(Modifier.fillMaxWidth().height(PqDimens.Border).background(PqColors.Ink))

@Composable private fun CellRule() = Box(Modifier.width(PqDimens.Border).fillMaxHeight().background(PqColors.Ink))

@Composable
private fun SmallChip(text: String, bg: Color) {
    Text(
        text.uppercase(), style = PqType.Label.copy(letterSpacing = 0.4.sp),
        modifier = Modifier.clip(PqCircle).background(bg).border(PqDimens.BorderThin, PqColors.Ink, PqCircle).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 14.dp, vertical = 10.dp).semantics(mergeDescendants = true) {}) {
        Text(value, style = PqType.Number)
        Text(label, style = PqType.Meta.copy(fontSize = 11.bsp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun StatsSummary(s: StatsUi, simple: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!simple) {
            PqCard(Modifier.weight(1f).semantics(mergeDescendants = true) {}, bg = PqColors.Sky, radius = PqDimens.Radius) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(stringResource(R.string.pq_xp_earned_plain, s.xpLast7Days), style = PqType.display(34))
                    Text(stringResource(R.string.pq_hero_last_7_days), style = PqType.MetaStrong)
                }
            }
        }
        PqCard(Modifier.weight(1f).semantics(mergeDescendants = true) {}, bg = PqColors.Gold, radius = PqDimens.Radius) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("${s.perfectDays}", style = PqType.display(34))
                Text(stringResource(R.string.pq_hero_perfect_days_week), style = PqType.MetaStrong)
            }
        }
    }
}

@Composable
private fun Heatmap(days: List<DayStatus>, simple: Boolean) {
    PqCard(Modifier.testTag(HeroTags.HEATMAP), radius = PqDimens.Radius) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(if (simple) R.string.pq_hero_activity_simple else R.string.pq_hero_activity), style = PqType.Label.copy(fontSize = 13.lsp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(R.string.pq_heatmap_monday, null, R.string.pq_heatmap_wednesday, null, R.string.pq_heatmap_friday, null, R.string.pq_heatmap_sunday).forEach {
                        Text(it?.let { r -> stringResource(r) }.orEmpty(), style = PqType.Meta.copy(fontSize = 9.bsp, lineHeight = 13.bsp), modifier = Modifier.height(13.dp))
                    }
                }
                HeatmapCells(days)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    DayStatus.Perfect to R.string.pq_heatmap_perfect,
                    DayStatus.Partial to R.string.pq_heatmap_partial,
                    DayStatus.Missed to R.string.pq_heatmap_missed,
                    DayStatus.None to R.string.pq_heatmap_none,
                ).forEach { (d, l) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(pqShape(2.dp)).background(d.color).border(1.5.dp, PqColors.Ink, pqShape(2.dp)))
                        Text(" " + stringResource(l), style = PqType.Meta.copy(fontSize = 11.bsp, color = PqColors.Ink2))
                    }
                }
            }
        }
    }
}

/**
 * The heatmap's days, a week to a column, drawn in one Canvas: as many bordered boxes, it took the
 * Hero tab a frame of its own to lay out. Each cell is the skin's small shape, filled and inked.
 */
@Composable
private fun HeatmapCells(days: List<DayStatus>) {
    val weeks = (days.size + 6) / 7
    val shape = pqShape(3.dp)
    val ink = PqColors.Ink
    val colors = days.map { it.color }
    Canvas(Modifier.size(width = 13.dp * weeks + 3.dp * (weeks - 1).coerceAtLeast(0), height = 13.dp * 7 + 3.dp * 6)) {
        val cell = 13.dp.toPx()
        val gap = 3.dp.toPx()
        val stroke = 1.5.dp.toPx()
        val fill = shape.createOutline(Size(cell, cell), layoutDirection, this)
        val edge = shape.createOutline(Size(cell - stroke, cell - stroke), layoutDirection, this)
        colors.forEachIndexed { i, color ->
            translate(left = (i / 7) * (cell + gap), top = (i % 7) * (cell + gap)) {
                drawOutline(fill, color)
                translate(stroke / 2, stroke / 2) { drawOutline(edge, ink, style = Stroke(stroke)) }
            }
        }
    }
}

@Composable
private fun WeeklyTrend(s: StatsUi) {
    PqCard(Modifier.testTag(HeroTags.WEEKLY), radius = PqDimens.Radius) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.pq_hero_weekly), style = PqType.Label.copy(fontSize = 13.lsp))
            Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                s.weekly.forEachIndexed { i, pct ->
                    val label = if (i == s.weekly.lastIndex) stringResource(R.string.pq_hero_week_now) else stringResource(R.string.pq_hero_week_ago, s.weekly.lastIndex - i)
                    Column(
                        Modifier.weight(1f).semantics(mergeDescendants = true) {},
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(R.string.pq_percent, pct), style = PqType.Button.copy(fontSize = 11.lsp), textAlign = TextAlign.Center)
                        val h = (pct / s.targetPercent.coerceAtLeast(1).toFloat() * 56f).coerceIn(4f, 56f).dp
                        Box(
                            Modifier.fillMaxWidth().height(h).clip(pqShapeTop(5.dp))
                                .background(if (i == s.weekly.lastIndex) PqColors.Progress else PqColors.Lilac)
                                .border(PqDimens.BorderThin, PqColors.Ink, pqShapeTop(5.dp))
                        )
                        Text(label, style = PqType.Meta.copy(fontSize = 11.bsp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

/** What the old Stats screen also said: the longest streak, the 3-month completion, and prayers. */
@Composable
private fun MoreStats(state: HeroUiState, simple: Boolean) {
    PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!simple) {
                Text(pluralStringResource(R.plurals.pq_hero_longest_streak, state.stats.longestStreak, state.stats.longestStreak), style = PqType.Body)
            }
            Text(stringResource(R.string.pq_hero_completion, state.stats.completionPercent), style = PqType.Body)
            Text(stringResource(R.string.pq_hero_target, DifficultyMode.getDisplayName(state.difficulty), state.stats.targetPercent), style = PqType.Meta)
            state.stats.namaz?.let { namaz ->
                Text(stringResource(R.string.pq_hero_namaz), style = PqType.Label.copy(fontSize = 12.lsp), modifier = Modifier.padding(top = 6.dp))
                if (!simple) Text(stringResource(R.string.pq_hero_namaz_xp, namaz.xpTotal, namaz.xpLastWeek), style = PqType.Body)
                Text(pluralStringResource(R.plurals.pq_hero_namaz_prayed, namaz.prayed, namaz.prayed), style = PqType.Body)
            }
        }
    }
}
