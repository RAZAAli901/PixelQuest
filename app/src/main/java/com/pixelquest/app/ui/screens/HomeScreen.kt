package com.pixelquest.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.PixelAvatar
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqChip
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqProgressBar
import com.pixelquest.app.ui.components.PqSectionHeader
import com.pixelquest.app.ui.components.ProgressRing
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestRow
import com.pixelquest.app.ui.components.QuestRowCompact
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.navigation.LocalPqRootState
import com.pixelquest.app.ui.screens.home.HomeViewModel
import com.pixelquest.app.ui.screens.leveling.LevelUpCelebrationScreen
import com.pixelquest.app.ui.screens.namaz.rememberPrayerTimeFormatter
import com.pixelquest.app.ui.screens.today.TodayTaskItem
import com.pixelquest.app.ui.screens.today.TodayUiState
import com.pixelquest.app.ui.screens.today.TodayViewModel
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.pqShape
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Tags for tests. */
object HomeTags {
    const val SCREEN = "home_screen"
    const val PROGRESS = "home_progress"
    const val PERFECT = "home_perfect_day"
    const val EMPTY = "home_empty"
    const val AVATAR = "home_avatar"
    const val PARTIES = "home_parties"
    const val STREAK_BROKEN = "home_streak_broken"
    fun quest(id: Long) = "home_quest_$id"
}

/**
 * Home as UI v2 draws it (docs/ui-v2.md, mocks 2A and 2B): who's playing, today's progress (or the
 * perfect day), the coach, the next prayer, and the quests still to do.
 *
 * @param isPerfectDay today's target is met (the difficulty's threshold, not every quest).
 * @param upNext the quests still to do today: pending, past their time, or skipped (which can still be done).
 */
data class HomeUiState(
    val heroName: String,
    val level: Int,
    val avatar: AvatarClass,
    val streakDays: Int,
    val doneCount: Int,
    val totalCount: Int,
    val targetPercent: Int,
    val xpToday: Int,
    val upNext: List<QuestUi>,
    val doneToday: List<QuestUi>,
    val isPerfectDay: Boolean,
    val isStreakBroken: Boolean = false,
    val isSimpleMode: Boolean = false,
) {
    val progress: Float get() = if (totalCount == 0) 0f else doneCount / totalCount.toFloat()
}

/** The greeting for the time of day. */
@StringRes
fun greetingFor(time: LocalTime): Int = when (time.hour) {
    in 5..11 -> R.string.pq_home_greeting_morning
    in 12..16 -> R.string.pq_home_greeting_afternoon
    else -> R.string.pq_home_greeting_evening
}

/** How a quest's repetition reads on its row, or null for a one-time quest. */
@StringRes
fun repeatLabelOf(type: RecurrenceType): Int? = when (type) {
    RecurrenceType.DAILY -> R.string.pq_repeat_daily
    RecurrenceType.WEEKLY -> R.string.pq_repeat_weekly
    RecurrenceType.MONTHLY -> R.string.pq_repeat_monthly
    RecurrenceType.ONE_TIME -> null
}

/** A quest on today's list, as its row shows it. */
fun TodayTaskItem.toQuestUi(time: DateTimeFormatter, repeatLabel: (RecurrenceType) -> String?): QuestUi = QuestUi(
    id = task.id,
    title = task.name,
    time = scheduledTime.format(time),
    category = QuestCategory.from(task.category.name),
    repeatLabel = repeatLabel(task.recurrenceType),
    done = status == TaskItemStatus.DONE,
    overdue = status == TaskItemStatus.GRACE_PERIOD || status == TaskItemStatus.MISSED,
    xp = xp,
)

/** Today's state as Home shows it. */
fun TodayUiState.Success.toHomeUi(time: DateTimeFormatter, repeatLabel: (RecurrenceType) -> String?): HomeUiState {
    val done = tasks.filter { it.status == TaskItemStatus.DONE }
    return HomeUiState(
        heroName = heroName,
        level = level,
        avatar = AvatarClass.from(avatarId),
        streakDays = currentStreak,
        doneCount = done.size,
        totalCount = tasks.size,
        targetPercent = (targetThreshold * 100).toInt(),
        xpToday = xpToday,
        upNext = tasks.filter { it.status != TaskItemStatus.DONE }.map { it.toQuestUi(time, repeatLabel) },
        doneToday = done.map { it.toQuestUi(time, repeatLabel) },
        isPerfectDay = isPerfectDay,
        isStreakBroken = isStreakBroken,
        isSimpleMode = isSimpleMode,
    )
}

/** Home's route: today's quests from [TodayViewModel], the level-up celebration over them. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    todayViewModel: TodayViewModel = hiltViewModel(),
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToEditTask: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAiInsight: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAccount: () -> Unit = onNavigateToSettings,
    onNavigateToNamaz: () -> Unit = {},
    onSeeAllQuests: () -> Unit = {},
    onOpenStudyParty: (String) -> Unit = {},
    onOpenParties: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val today by todayViewModel.uiState.collectAsState()
    // Coming back to the app (e.g. the next morning) re-reads the date.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) todayViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val haptics = LocalHapticFeedback.current
    val sound = LocalSoundManager.current
    // The streak-broken sting plays once, not each time the notice scrolls back into view.
    val showsStreakBroken = (today as? TodayUiState.Success)?.let { it.isStreakBroken && !it.isSimpleMode } == true
    androidx.compose.runtime.LaunchedEffect(showsStreakBroken) {
        if (showsStreakBroken && todayViewModel.shouldPlayStreakBrokenSound()) sound?.playTaskMissedSound()
    }

    Box(Modifier.fillMaxSize().background(PqColors.Ground)) {
        (today as? TodayUiState.Success)?.let { state ->
            val time = rememberPrayerTimeFormatter()
            val labels = mapOf(
                RecurrenceType.DAILY to stringResource(R.string.pq_repeat_daily),
                RecurrenceType.WEEKLY to stringResource(R.string.pq_repeat_weekly),
                RecurrenceType.MONTHLY to stringResource(R.string.pq_repeat_monthly),
            )
            val home = remember(state, time, labels) { state.toHomeUi(time) { labels[it] } }
            HomeContent(
                state = home,
                greeting = stringResource(greetingFor(LocalTime.now())),
                onCompleteQuest = { quest ->
                    state.tasks.firstOrNull { it.task.id == quest.id }?.let { item ->
                        completeFeedback(haptics, sound)
                        todayViewModel.completeTask(item.task)
                    }
                },
                onSkipQuest = { quest ->
                    state.tasks.firstOrNull { it.task.id == quest.id }?.let { item ->
                        PixelHaptics.performWarning(haptics)
                        sound?.playTaskMissedSound()
                        todayViewModel.skipTask(item.task)
                    }
                },
                onQuestClick = { onNavigateToEditTask(it.id) },
                onSeeAllQuests = onSeeAllQuests,
                onNewQuest = onNavigateToCreateTask,
                onAvatarClick = onNavigateToProfile,
                onParties = onOpenParties,
                partiesDot = LocalPqRootState.current.partyInviteWaiting,
                coachSection = {
                    com.pixelquest.app.ui.screens.today.TodayAiInsightSection(
                        onNavigateToAiInsight = onNavigateToAiInsight,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToAccount = onNavigateToAccount
                    )
                },
                prayerSection = { com.pixelquest.app.ui.screens.namaz.HomeNamazSection(onOpen = onNavigateToNamaz) },
                studySection = { com.pixelquest.app.study.ui.HomeStudySection(onOpenParty = onOpenStudyParty, onOpenParties = onOpenParties) },
            )
        }

        val pendingLevel = uiState.pendingLevelUp
        if (pendingLevel != null && !uiState.isSimpleMode) {
            LevelUpCelebrationScreen(level = pendingLevel, onDismiss = { viewModel.dismissLevelUpCelebration() })
        }
    }
}

private fun completeFeedback(haptics: HapticFeedback, sound: com.pixelquest.app.audio.SoundManager?) {
    PixelHaptics.performSuccessPattern(haptics)
    sound?.playTaskCompleteSound()
}

/**
 * Home for [state]. A quest's circle completes it; swiping it left (or TalkBack's action) skips it
 * for today, after asking. The coach, prayer and study cards are slots: each reads its own ViewModel.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    greeting: String,
    onCompleteQuest: (QuestUi) -> Unit,
    onSkipQuest: (QuestUi) -> Unit,
    onQuestClick: (QuestUi) -> Unit,
    onSeeAllQuests: () -> Unit,
    onNewQuest: () -> Unit,
    onAvatarClick: () -> Unit,
    onParties: () -> Unit,
    modifier: Modifier = Modifier,
    partiesDot: Boolean = false,
    coachSection: @Composable () -> Unit = {},
    prayerSection: @Composable () -> Unit = {},
    studySection: @Composable () -> Unit = {},
) {
    var toSkip by remember { mutableStateOf<QuestUi?>(null) }
    toSkip?.let { quest ->
        PqConfirmDialog(
            title = stringResource(if (state.isSimpleMode) R.string.pq_skip_title_simple else R.string.pq_skip_title),
            message = stringResource(R.string.pq_skip_message, quest.title),
            confirm = stringResource(R.string.pq_skip_confirm),
            onConfirm = { toSkip = null; onSkipQuest(quest) },
            onDismiss = { toSkip = null },
        )
    }

    LazyColumn(
        modifier.fillMaxSize().background(PqColors.Ground).testTag(HomeTags.SCREEN),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") { HomeHeader(state, greeting, onAvatarClick, onParties, partiesDot) }
        // One item, so a card that isn't there (the coach, prayers, study) leaves no gap.
        item(key = "cards") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when {
                    state.totalCount == 0 -> EmptyDayCard(state.isSimpleMode, onNewQuest)
                    state.isPerfectDay -> PerfectDayCard(state)
                    else -> ProgressCard(state)
                }
                if (state.isStreakBroken && !state.isSimpleMode) {
                    Text(
                        stringResource(R.string.pq_home_streak_broken),
                        style = PqType.MetaStrong.copy(color = PqColors.RedText),
                        modifier = Modifier.testTag(HomeTags.STREAK_BROKEN),
                    )
                }
                coachSection()
                prayerSection()
                studySection()
            }
        }
        if (state.upNext.isNotEmpty()) {
            item(key = "up_next") {
                PqSectionHeader(stringResource(R.string.pq_home_up_next), action = stringResource(R.string.pq_home_see_all), onAction = onSeeAllQuests)
            }
            items(state.upNext, key = { it.id }) { quest ->
                SkippableQuestRow(quest, onComplete = { onCompleteQuest(quest) }, onSkip = { toSkip = quest }, onClick = { onQuestClick(quest) })
            }
        }
        if (state.doneToday.isNotEmpty()) {
            item(key = "done_today") { PqSectionHeader(stringResource(R.string.pq_home_done_today, state.doneToday.size)) }
            items(state.doneToday, key = { it.id }) { quest ->
                QuestRowCompact(if (state.isSimpleMode) quest.copy(xp = 0) else quest, Modifier.testTag(HomeTags.quest(quest.id)))
            }
        }
    }
}

/** A quest still to do: its circle completes it; swiped left, it asks to skip it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkippableQuestRow(quest: QuestUi, onComplete: () -> Unit, onSkip: () -> Unit, onClick: () -> Unit) {
    val skipLabel = stringResource(R.string.pq_skip_action)
    val swipe = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart) onSkip()
        false // the row stays: skipping asks first
    })
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(pqShape(PqDimens.Radius)).background(PqColors.Rose).padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Text(skipLabel.uppercase(), style = PqType.Button) }
        },
        modifier = Modifier
            .testTag(HomeTags.quest(quest.id))
            .semantics { customActions = listOf(CustomAccessibilityAction(skipLabel) { onSkip(); true }) },
    ) {
        QuestRow(quest, onToggle = { if (!quest.done) onComplete() }, onClick = onClick)
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, greeting: String, onAvatarClick: () -> Unit, onParties: () -> Unit, partiesDot: Boolean) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PixelAvatar(state.avatar, 46.dp, Modifier.comicPressable(onAvatarClick, radius = 8.dp, shadow = 0.dp).testTag(HomeTags.AVATAR))
        Column(Modifier.weight(1f)) {
            Text(greeting, style = PqType.Meta.copy(fontSize = 12.bsp))
            Text(
                if (state.isSimpleMode) state.heroName else stringResource(R.string.pq_home_name_level, state.heroName, state.level),
                style = PqType.BodyStrong.copy(fontSize = 16.bsp), maxLines = 1,
            )
        }
        Box {
            val parties = stringResource(R.string.pq_home_parties)
            val waiting = stringResource(R.string.pq_nav_something_waiting)
            PqIconButton(Icons.Rounded.Groups, if (partiesDot) "$parties, $waiting" else parties, onParties, Modifier.testTag(HomeTags.PARTIES))
            if (partiesDot) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(12.dp).clip(PqCircle)
                        .background(PqColors.Red).border(PqDimens.BorderThin, PqColors.Ink, PqCircle)
                )
            }
        }
        if (!state.isSimpleMode) {
            val streak = pluralStringResource(R.plurals.pq_home_streak_days, state.streakDays, state.streakDays)
            PqChip(
                "${state.streakDays}", PqColors.Gold, icon = Icons.Rounded.LocalFireDepartment, iconTint = PqColors.Flame,
                modifier = Modifier.semantics { contentDescription = streak },
            )
        }
    }
}

@Composable
private fun ProgressCard(state: HomeUiState) {
    PqCard(Modifier.testTag(HomeTags.PROGRESS), shadow = PqDimens.ShadowLg) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(if (state.isSimpleMode) R.string.pq_home_progress_simple else R.string.pq_home_progress), style = PqType.CardTitle)
                Text(
                    stringResource(R.string.pq_home_progress_line, state.doneCount, state.totalCount, state.targetPercent),
                    style = PqType.Body.copy(fontSize = 13.bsp, color = PqColors.Ink2),
                )
                PqProgressBar(state.progress, target = state.targetPercent / 100f)
                if (!state.isSimpleMode) {
                    Text(stringResource(R.string.pq_home_xp_today, state.xpToday), style = PqType.MetaStrong.copy(color = PqColors.RedText))
                }
            }
            ProgressRing(state.progress, stringResource(R.string.pq_percent, (state.progress * 100).toInt()))
        }
    }
}

@Composable
private fun PerfectDayCard(state: HomeUiState) {
    PqCard(Modifier.testTag(HomeTags.PERFECT), bg = PqColors.Gold, shadow = PqDimens.ShadowLg) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (state.doneCount == state.totalCount) {
                    pluralStringResource(if (state.isSimpleMode) R.plurals.pq_home_all_done_simple else R.plurals.pq_home_all_done, state.totalCount, state.totalCount)
                } else {
                    stringResource(R.string.pq_home_some_done, state.doneCount, state.totalCount)
                },
                style = PqType.Label,
            )
            Text(
                stringResource(R.string.pq_home_perfect_day),
                style = PqType.Hero.copy(color = PqColors.Red, shadow = Shadow(PqColors.Ink, Offset(6f, 6f), 0f)),
            )
            Text(
                if (state.isSimpleMode) stringResource(R.string.pq_home_perfect_body_simple) else stringResource(R.string.pq_home_perfect_body, state.level + 1),
                style = PqType.Body,
            )
            if (!state.isSimpleMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PqChip(stringResource(R.string.pq_xp_earned, state.xpToday), PqColors.Paper)
                    PqChip(
                        pluralStringResource(R.plurals.pq_home_streak_days, state.streakDays, state.streakDays), PqColors.Paper,
                        icon = Icons.Rounded.LocalFireDepartment, iconTint = PqColors.Flame,
                    )
                }
            }
        }
    }
}

/** No quests today: what Home is for, and the screen's one red button. */
@Composable
private fun EmptyDayCard(isSimpleMode: Boolean, onNewQuest: () -> Unit) {
    PqCard(Modifier.testTag(HomeTags.EMPTY), shadow = PqDimens.ShadowLg) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(if (isSimpleMode) R.string.pq_home_empty_title_simple else R.string.pq_home_empty_title), style = PqType.CardTitle)
            Text(stringResource(R.string.pq_home_empty_body), style = PqType.Body.copy(color = PqColors.Ink2))
            Spacer(Modifier.height(2.dp))
            PqButton(stringResource(if (isSimpleMode) R.string.pq_new_task else R.string.pq_new_quest), onNewQuest, Modifier.fillMaxWidth())
        }
    }
}
