package com.pixelquest.app.ui.screens

import com.pixelquest.app.ui.motion.LocalPqToast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.ui.components.PqChoiceChip
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqQuietButton
import com.pixelquest.app.ui.components.PqSegmented
import com.pixelquest.app.ui.components.PqTextField
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestRow
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.screens.history.HistoryFilter
import com.pixelquest.app.ui.screens.history.TaskHistoryItem
import com.pixelquest.app.ui.screens.history.TaskHistoryViewModel
import com.pixelquest.app.ui.screens.namaz.rememberPrayerTimeFormatter
import com.pixelquest.app.ui.screens.tasks.TaskUiState
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.screens.tasks.TaskWithStatus
import com.pixelquest.app.ui.screens.today.TodayTaskItem
import com.pixelquest.app.ui.screens.today.TodayUiState
import com.pixelquest.app.ui.screens.today.TodayViewModel
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Quests' three halves (mock 2C). History was the side menu's Quest History. */
enum class QuestTab { Today, All, History }

/** Tags for tests. */
object QuestsTags {
    const val SCREEN = "quests_screen"
    const val SEARCH = "quests_search"
    const val SEARCH_FIELD = "quests_search_field"
    const val NEW = "quests_new"
    const val MORE = "quests_history_more"
    const val EMPTY = "quests_empty"
    fun quest(id: Long) = "quests_quest_$id"
    /** A quest's row in the history. */
    fun history(taskId: Long) = "quests_history_$taskId"
}

/** Where in the day a quest falls, for Today's groups. */
enum class DayPart(@StringRes val label: Int) {
    Morning(R.string.pq_quests_morning),
    Afternoon(R.string.pq_quests_afternoon),
    Evening(R.string.pq_quests_evening);

    companion object {
        fun of(hour: Int): DayPart = when {
            hour < 12 -> Morning
            hour < 17 -> Afternoon
            else -> Evening
        }
    }
}

/** A group of rows under its label: [title] is already in words. */
data class QuestGroup(val key: String, val title: String, val quests: List<QuestUi>)

/** Today's quests by part of the day, in time order. */
fun todayParts(items: List<TodayTaskItem>): List<Pair<DayPart, List<TodayTaskItem>>> =
    items.sortedBy { it.scheduledTime }.groupBy { DayPart.of(it.scheduledTime.hour) }.toList().sortedBy { it.first.ordinal }

/** Every quest by how it repeats: daily first, one-time last, each in time order. */
fun recurrenceGroups(tasks: List<TaskWithStatus>): List<Pair<RecurrenceType, List<TaskWithStatus>>> {
    val order = listOf(RecurrenceType.DAILY, RecurrenceType.WEEKLY, RecurrenceType.MONTHLY, RecurrenceType.ONE_TIME)
    return tasks.groupBy { it.task.recurrenceType }.toList().sortedBy { order.indexOf(it.first) }
        .map { (type, list) -> type to list.sortedBy { it.task.scheduledTime } }
}

/** The history by day, the latest first. */
fun historyDays(items: List<TaskHistoryItem>): List<Pair<LocalDate, List<TaskHistoryItem>>> =
    items.groupBy { it.completedDate }.toList().sortedByDescending { it.first }

/** Keeps the quests whose title has [query] in it (any case); all of them for a blank one. */
fun List<QuestGroup>.matching(query: String): List<QuestGroup> =
    if (query.isBlank()) this else mapNotNull { g -> g.copy(quests = g.quests.filter { it.title.contains(query.trim(), ignoreCase = true) }).takeIf { it.quests.isNotEmpty() } }

/**
 * The Quests tab (docs/ui-v2.md, mock 2C): Today's quests by part of the day, every quest by how it
 * repeats, and the history by day. A quest of today's has a circle to complete it and a swipe to skip
 * it; another day's opens. The search keeps the titles that match.
 *
 * @param initialTab History when opened from Hero's Quest history.
 */
@Composable
fun QuestsScreen(
    onNewQuest: () -> Unit,
    onEditQuest: (Long) -> Unit,
    onOpenHistoryQuest: (Long) -> Unit,
    initialTab: QuestTab = QuestTab.Today,
    onNavigateBack: (() -> Unit)? = null,
    todayViewModel: TodayViewModel = hiltViewModel(),
    allViewModel: TaskViewModel = hiltViewModel(),
    historyViewModel: TaskHistoryViewModel = hiltViewModel(),
) {
    val today by todayViewModel.uiState.collectAsState()
    val all by allViewModel.uiState.collectAsState()
    val history by historyViewModel.uiState.collectAsState()
    val simple by allViewModel.isSimpleMode.collectAsState()
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var query by rememberSaveable { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    val toast = LocalPqToast.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val sound = LocalSoundManager.current
    val time = rememberPrayerTimeFormatter()
    val repeat = repeatLabels()

    val todayItems = (today as? TodayUiState.Success)?.tasks.orEmpty()
    val todayById = remember(todayItems) { todayItems.associateBy { it.task.id } }
    // Grouping and mapping every quest is done once per change of the lists, not on every
    // recomposition (a keystroke in search, a dialog opening): only the group titles are rebuilt.
    val groups: List<QuestGroup> = when (tab) {
        QuestTab.Today -> remember(todayItems, time, repeat) {
            todayParts(todayItems).map { (part, items) -> part to items.map { it.toQuestUi(time) { t -> repeat[t] } } }
        }.map { (part, quests) ->
            val left = quests.count { !it.done }
            val name = stringResource(part.label)
            QuestGroup(
                part.name,
                if (left > 0) stringResource(R.string.pq_quests_group_left, name, left) else stringResource(R.string.pq_quests_group_done, name, quests.size),
                quests,
            )
        }
        QuestTab.All -> {
            val tasks = (all as? TaskUiState.Success)?.tasks.orEmpty()
            remember(tasks, time, repeat) {
                recurrenceGroups(tasks).map { (type, list) -> type to list.map { it.toQuestUi(time) { t -> repeat[t] } } }
            }.map { (type, quests) ->
                QuestGroup(
                    type.name,
                    stringResource(R.string.pq_quests_group_count, repeat[type] ?: stringResource(R.string.pq_repeat_once), quests.size),
                    quests,
                )
            }
        }
        QuestTab.History -> remember(history.items) {
            historyDays(history.items).map { (date, items) -> date to items.map { it.toQuestUi() } }
        }.map { (date, quests) ->
            QuestGroup(date.toString(), dayTitle(date), quests)
        }
    }
    val shown = remember(groups, query) { groups.matching(query.orEmpty()) }

    var toSkip by remember { mutableStateOf<QuestUi?>(null) }
    toSkip?.let { quest ->
        SkipQuestDialog(quest, simple, onSkip = {
            toSkip = null
            todayById[quest.id]?.let {
                PixelHaptics.performWarning(haptics)
                sound?.playTaskMissedSound()
                todayViewModel.skipTask(it.task)
                toast?.show(context.getString(R.string.motion_toast_skipped, it.task.name))
            }
        }, onDismiss = { toSkip = null })
    }

    QuestsContent(
        tab = tab,
        onTabChange = { tab = it },
        todayCount = todayItems.size,
        groups = shown,
        query = query,
        onQueryChange = { query = it },
        isSimpleMode = simple,
        onNewQuest = onNewQuest,
        onNavigateBack = onNavigateBack,
        onQuestClick = { quest -> if (tab == QuestTab.History) onOpenHistoryQuest(quest.id) else onEditQuest(quest.id) },
        // Only today's quests can be done or skipped; another day's opens instead.
        onToggleQuest = { quest ->
            val item = todayById[quest.id]
            when {
                tab == QuestTab.History -> Unit
                item == null -> onEditQuest(quest.id)
                item.status != TaskItemStatus.DONE -> {
                    PixelHaptics.performSuccessPattern(haptics)
                    sound?.playTaskCompleteSound()
                    todayViewModel.completeTask(item.task)
                    toast?.show(context.getString(R.string.motion_toast_done, item.task.name))
                }
            }
        },
        onSkipQuest = { quest -> if (todayById[quest.id]?.status != TaskItemStatus.DONE) toSkip = quest },
        canSkip = { tab == QuestTab.Today && todayById[it.id]?.status != TaskItemStatus.DONE },
        historyFilter = history.selectedFilter,
        onHistoryFilter = historyViewModel::setFilter,
        historyHasMore = history.hasMoreItems,
        onHistoryMore = historyViewModel::loadNextPage,
    )
}

/** How each kind of repetition reads ("Daily"); a one-time quest has none. */
@Composable
internal fun repeatLabels(): Map<RecurrenceType, String> = mapOf(
    RecurrenceType.DAILY to stringResource(R.string.pq_repeat_daily),
    RecurrenceType.WEEKLY to stringResource(R.string.pq_repeat_weekly),
    RecurrenceType.MONTHLY to stringResource(R.string.pq_repeat_monthly),
)

/** "Today", "Yesterday", or the day ("Mon 19 Oct"). */
@Composable
private fun dayTitle(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.pq_history_today)
        today.minusDays(1) -> stringResource(R.string.pq_history_yesterday)
        else -> date.format(DayFormatter)
    }
}

private val DayFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

/** A quest from the list of all of them, with how it stands today. */
fun TaskWithStatus.toQuestUi(time: DateTimeFormatter, repeatLabel: (RecurrenceType) -> String?): QuestUi = QuestUi(
    id = task.id,
    title = task.name,
    time = task.scheduledTime.format(time),
    category = QuestCategory.from(task.category.name),
    repeatLabel = repeatLabel(task.recurrenceType),
    done = status == TaskItemStatus.COMPLETED || status == TaskItemStatus.DONE,
    missed = status == TaskItemStatus.MISSED,
)

/** A day in the history: done, with its XP, or missed. Its id is the quest's, to open it. */
fun TaskHistoryItem.toQuestUi(): QuestUi = QuestUi(
    id = taskId,
    title = taskName,
    time = null,
    category = QuestCategory.from(category.name),
    done = wasCompleted,
    missed = !wasCompleted,
    xp = if (wasCompleted) pointsAwarded else 0,
)

@Composable
fun QuestsContent(
    tab: QuestTab,
    onTabChange: (QuestTab) -> Unit,
    todayCount: Int,
    groups: List<QuestGroup>,
    onToggleQuest: (QuestUi) -> Unit,
    onQuestClick: (QuestUi) -> Unit,
    onNewQuest: () -> Unit,
    modifier: Modifier = Modifier,
    query: String? = null,
    onQueryChange: (String?) -> Unit = {},
    isSimpleMode: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    onSkipQuest: (QuestUi) -> Unit = {},
    canSkip: (QuestUi) -> Boolean = { false },
    historyFilter: HistoryFilter = HistoryFilter.ALL_TIME,
    onHistoryFilter: (HistoryFilter) -> Unit = {},
    historyHasMore: Boolean = false,
    onHistoryMore: () -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(PqColors.Ground)) {
        LazyColumn(
            Modifier.fillMaxSize().testTag(QuestsTags.SCREEN),
            contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "top") {
                PqTopBar(stringResource(if (isSimpleMode) R.string.pq_quests_title_simple else R.string.pq_quests_title), onBack = onNavigateBack) {
                    if (query == null) {
                        PqIconButton(Icons.Rounded.Search, stringResource(R.string.pq_quests_search), { onQueryChange("") }, Modifier.testTag(QuestsTags.SEARCH))
                    } else {
                        PqIconButton(Icons.Rounded.Close, stringResource(R.string.pq_quests_search_close), { onQueryChange(null) }, Modifier.testTag(QuestsTags.SEARCH))
                    }
                }
            }
            if (query != null) {
                item(key = "search") {
                    PqTextField(
                        query, { onQueryChange(it) },
                        placeholder = stringResource(if (isSimpleMode) R.string.pq_quests_search_hint_simple else R.string.pq_quests_search_hint),
                        modifier = Modifier.testTag(QuestsTags.SEARCH_FIELD),
                    )
                }
            }
            item(key = "tabs") {
                val labels = listOf(
                    stringResource(R.string.pq_quests_tab_today, todayCount),
                    stringResource(if (isSimpleMode) R.string.pq_quests_tab_all_simple else R.string.pq_quests_tab_all),
                    stringResource(R.string.pq_quests_tab_history),
                )
                PqSegmented(labels, tab.ordinal, { onTabChange(QuestTab.entries[it]) })
            }
            if (tab == QuestTab.History) {
                item(key = "filters") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            HistoryFilter.LAST_7_DAYS to R.string.pq_history_7_days,
                            HistoryFilter.LAST_30_DAYS to R.string.pq_history_30_days,
                            HistoryFilter.ALL_TIME to R.string.pq_history_all_time,
                        ).forEach { (filter, label) -> PqChoiceChip(stringResource(label), filter == historyFilter, { onHistoryFilter(filter) }) }
                    }
                }
            }
            if (groups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        when {
                            !query.isNullOrBlank() -> stringResource(R.string.pq_quests_no_match, query)
                            tab == QuestTab.Today -> stringResource(R.string.pq_quests_empty_today)
                            tab == QuestTab.All -> stringResource(R.string.pq_quests_empty_all)
                            else -> stringResource(R.string.pq_history_empty)
                        },
                        style = PqType.Body.copy(color = PqColors.Ink2),
                        modifier = Modifier.padding(top = 8.dp).testTag(QuestsTags.EMPTY),
                    )
                }
            }
            groups.forEach { g ->
                item(key = "h_${tab}_${g.key}") { PqGroupLabel(g.title) }
                items(g.quests, key = { "${tab}_${g.key}_${it.id}" }) { q ->
                    val tag = if (tab == QuestTab.History) QuestsTags.history(q.id) else QuestsTags.quest(q.id)
                    if (canSkip(q)) {
                        SkippableQuestRow(q, tag, onComplete = { onToggleQuest(q) }, onSkip = { onSkipQuest(q) }, onClick = { onQuestClick(q) })
                    } else {
                        QuestRow(q, onToggle = { onToggleQuest(q) }, onClick = { onQuestClick(q) }, modifier = Modifier.testTag(tag))
                    }
                }
            }
            if (tab == QuestTab.History && historyHasMore) {
                item(key = "more") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PqQuietButton(stringResource(R.string.pq_history_more), onHistoryMore, Modifier.testTag(QuestsTags.MORE))
                    }
                }
            }
        }
        // The extended button says what it does: the screen's one red button.
        val newLabel = stringResource(if (isSimpleMode) R.string.pq_new_task else R.string.pq_new_quest)
        Row(
            Modifier.align(Alignment.BottomEnd)
                .padding(end = PqDimens.ScreenPad, bottom = 18.dp)
                .height(52.dp)
                .comicPressable(onNewQuest, bg = PqColors.Red, radius = 16.dp, shadow = PqDimens.ShadowLg)
                .semantics { role = Role.Button }
                .testTag(QuestsTags.NEW)
                .padding(start = 14.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = PqColors.OnRed, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
            Text(newLabel.uppercase(), style = PqType.Button.copy(color = PqColors.OnRed))
        }
    }
}
