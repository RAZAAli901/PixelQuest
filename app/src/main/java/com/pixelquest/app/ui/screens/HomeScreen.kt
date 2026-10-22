package com.pixelquest.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.PqSectionHeader
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestRowCompact
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.motion.LocalPqToast
import com.pixelquest.app.ui.navigation.LocalPqRootState
import com.pixelquest.app.ui.screens.home.HomeViewModel
import com.pixelquest.app.ui.screens.leveling.LevelUpCelebrationScreen
import com.pixelquest.app.ui.screens.namaz.rememberPrayerTimeFormatter
import com.pixelquest.app.ui.screens.today.TodayTaskItem
import com.pixelquest.app.ui.screens.today.TodayUiState
import com.pixelquest.app.ui.screens.today.TodayViewModel
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
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
    val showsStreakBroken = remember(today) {
        (today as? TodayUiState.Success)?.let { it.isStreakBroken && !it.isSimpleMode } == true
    }
    androidx.compose.runtime.LaunchedEffect(showsStreakBroken) {
        if (showsStreakBroken && todayViewModel.shouldPlayStreakBrokenSound()) sound?.playTaskMissedSound()
    }

    val toast = LocalPqToast.current
    val context = LocalContext.current
    val greetingRes = remember { greetingFor(LocalTime.now()) }
    Box(Modifier.fillMaxSize().background(PqColors.Ground)) {
        // M12: a slow first load shows shimmering rows where the cards will be.
        if (today is TodayUiState.Loading) com.pixelquest.app.ui.motion.PqSkeleton(Modifier.padding(16.dp))
        (today as? TodayUiState.Success)?.let { state ->
            val time = rememberPrayerTimeFormatter()
            val labels = repeatLabels()
            val home = remember(state, time, labels) { state.toHomeUi(time) { labels[it] } }
            HomeContent(
                state = home,
                greeting = stringResource(greetingRes),
                onCompleteQuest = { quest ->
                    state.tasks.firstOrNull { it.task.id == quest.id }?.let { item ->
                        completeFeedback(haptics, sound)
                        todayViewModel.completeTask(item.task)
                        toast?.show(context.getString(R.string.motion_toast_done, item.task.name))
                    }
                },
                onSkipQuest = { quest ->
                    state.tasks.firstOrNull { it.task.id == quest.id }?.let { item ->
                        PixelHaptics.performWarning(haptics)
                        sound?.playTaskMissedSound()
                        todayViewModel.skipTask(item.task)
                        toast?.show(context.getString(R.string.motion_toast_skipped, item.task.name))
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
                        onNavigateToAccount = onNavigateToAccount,
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
        SkipQuestDialog(quest, state.isSimpleMode, onSkip = {
            toSkip = null
            onSkipQuest(quest)
        }, onDismiss = { toSkip = null })
    }

    LazyColumn(
        modifier.fillMaxSize().background(PqColors.Ground).testTag(HomeTags.SCREEN),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") { HomeHeader(state, greeting, onAvatarClick, onParties, partiesDot) }
        // One item, so a card that isn't there (the coach, prayers, study) leaves no gap.
        item(key = "cards") {
            // A day that turns perfect while Home is open is celebrated (M08); one that already was isn't.
            var wasPerfect by remember { mutableStateOf(state.isPerfectDay) }
            var celebrate by remember { mutableIntStateOf(0) }
            LaunchedEffect(state.isPerfectDay) {
                if (state.isPerfectDay && !wasPerfect) celebrate++
                wasPerfect = state.isPerfectDay
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when {
                    state.totalCount == 0 -> EmptyDayCard(state.isSimpleMode, onNewQuest)
                    state.isPerfectDay -> PerfectDayCard(state, celebrate)
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
                SkippableQuestRow(quest, HomeTags.quest(quest.id), onComplete = { onCompleteQuest(quest) }, onSkip = { toSkip = quest }, onClick = { onQuestClick(quest) })
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
