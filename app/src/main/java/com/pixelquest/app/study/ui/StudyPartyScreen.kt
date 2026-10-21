package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.PartyRefreshBox
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.ui.components.MemberList
import com.pixelquest.app.study.domain.StudyRefusal
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelFloatingActionButton
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.inkOnPanel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * A study party's own screen (docs/study-party.md): its name over four tabs, This week, All items,
 * Scoreboard and People. Its info (members, leader actions) opens from the top bar.
 */
@Composable
fun StudyPartyScreen(
    onNavigateBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onAddPeople: () -> Unit,
    onOpenItem: (String) -> Unit = {},
    viewModel: StudyPartyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val dueReminders by viewModel.dueReminders.collectAsState()
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    // Followed live only while the screen is visible.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.followWhileVisible() }
    }
    StudyPartyContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onOpenInfo = onOpenInfo,
        onAddPeople = onAddPeople,
        onSelectTab = viewModel::selectTab,
        onRefresh = viewModel::refresh,
        onDismissNotice = viewModel::dismissNotice,
        onAddItem = viewModel::openAddItem,
        onFormChange = viewModel::updateForm,
        onSaveForm = viewModel::saveForm,
        onCloseForm = viewModel::closeForm,
        onOpenItem = onOpenItem,
        actions = ItemActions(
            onEdit = viewModel::editItem,
            onTogglePin = viewModel::togglePin,
            onDelete = viewModel::askDelete,
            onDone = viewModel::complete,
            onUndo = viewModel::undo,
            onRetry = viewModel::retry,
            onDiscard = viewModel::discard
        ),
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        timerActions = TimerActions(viewModel::startTimer, viewModel::stopTimer, viewModel::cancelTimer, viewModel::logStudy),
        onDismissWinners = viewModel::dismissWinnerBanner,
        reminders = ReminderToggles(dueReminders, viewModel::setDueReminders, weeklySummary, viewModel::setWeeklySummary)
    )
}

@Composable
fun StudyPartyContent(
    state: StudyPartyUiState,
    onNavigateBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onAddPeople: () -> Unit = {},
    onSelectTab: (PartyTab) -> Unit = {},
    onRefresh: () -> Unit = {},
    onDismissNotice: () -> Unit = {},
    onOpenItem: (String) -> Unit = {},
    onAddItem: () -> Unit = {},
    onFormChange: (ItemForm) -> Unit = {},
    onSaveForm: () -> Unit = {},
    onCloseForm: () -> Unit = {},
    actions: ItemActions = ItemActions(),
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    timerActions: TimerActions = TimerActions(),
    onDismissWinners: () -> Unit = {},
    reminders: ReminderToggles = ReminderToggles()
) {
    CompositionLocalProvider(LocalStudySimpleMode provides state.simpleMode) {
        StudyPartyScaffold(state, onNavigateBack, onOpenInfo, onAddPeople, onSelectTab, onRefresh, onDismissNotice, onOpenItem, onAddItem, onFormChange, onSaveForm, onCloseForm, actions, onConfirmDelete, onCancelDelete, timerActions, onDismissWinners, reminders)
    }
}

@Composable
private fun StudyPartyScaffold(
    state: StudyPartyUiState,
    onNavigateBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onAddPeople: () -> Unit,
    onSelectTab: (PartyTab) -> Unit,
    onRefresh: () -> Unit,
    onDismissNotice: () -> Unit,
    onOpenItem: (String) -> Unit,
    onAddItem: () -> Unit,
    onFormChange: (ItemForm) -> Unit,
    onSaveForm: () -> Unit,
    onCloseForm: () -> Unit,
    actions: ItemActions,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    timerActions: TimerActions,
    onDismissWinners: () -> Unit,
    reminders: ReminderToggles
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = state.party?.name ?: stringResource(R.string.party_study_title),
                subtitle = stringResource(R.string.party_study_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(symbol = "◀", contentDescription = stringResource(R.string.navigate_back), style = MaterialTheme.typography.titleMedium, color = colors.primaryText)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInfo, modifier = Modifier.testTag(StudyTags.INFO)) {
                        SymbolIcon(symbol = "ⓘ", contentDescription = stringResource(R.string.study_open_info), style = MaterialTheme.typography.titleMedium, color = colors.primaryText)
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.tab == PartyTab.THIS_WEEK || state.tab == PartyTab.ALL_ITEMS) {
                val label = stringResource(R.string.study_add_item)
                PixelFloatingActionButton(onClick = onAddItem, modifier = Modifier.semantics { contentDescription = label }.testTag(StudyTags.ADD_ITEM))
            }
        },
        modifier = Modifier.testTag(StudyTags.SCREEN)
    ) { innerPadding ->
        state.confirmingDelete?.let { row ->
            PixelConfirmDialog(
                title = stringResource(R.string.study_delete_title),
                message = stringResource(R.string.study_delete_message, row.item.title),
                confirmText = stringResource(R.string.study_delete),
                dismissText = stringResource(R.string.study_keep),
                onConfirm = onConfirmDelete,
                onDismiss = onCancelDelete
            )
        }
        state.form?.let { form ->
            ItemSheet(form = form, today = state.today, isLeader = state.isLeader, onChange = onFormChange, onSave = onSaveForm, onDismiss = onCloseForm)
        }
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            StudyTabRow(tabs = state.tabs, selected = state.tab, onSelect = onSelectTab)
            state.notice?.let { NoticeLine(it, onDismissNotice) }
            PartyRefreshBox(refreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    // Room at the end for the Add item button over the last card.
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag(StudyTags.tab(state.tab))
                ) {
                    when (state.tab) {
                        PartyTab.THIS_WEEK -> {
                            // One first item for what sits at the top, so what arrives there (a banner,
                            // a pin) shows in place rather than above where the list is held.
                            item(key = "top") {
                                TopOfList {
                                    WinnerBannerIfAny(state, onDismissWinners)
                                    if (state.pinned.isNotEmpty()) PinnedStrip(state.pinned, onOpen = onOpenItem)
                                    if (state.thisWeek.isNotEmpty()) WeekSummary(state)
                                    StudyTimerCard(state, timerActions)
                                }
                            }
                            thisWeek(state, onOpenItem, actions)
                        }
                        PartyTab.ALL_ITEMS -> {
                            item(key = "top") {
                                TopOfList { if (state.pinned.isNotEmpty()) PinnedStrip(state.pinned, onOpen = onOpenItem) }
                            }
                            itemCards(state, state.items, R.string.study_items_empty, onOpenItem, actions)
                        }
                        PartyTab.SCOREBOARD -> {
                            item(key = "top") { TopOfList { WinnerBannerIfAny(state, onDismissWinners) } }
                            scoreboard(state)
                        }
                        PartyTab.PEOPLE -> {
                            people(state, onAddPeople, onOpenInfo)
                            reminderToggles(reminders, simpleMode = state.simpleMode)
                        }
                        PartyTab.CHAT -> Unit
                    }
                }
            }
        }
    }
}

/** The tabs, side by side, scrolling sideways when the text is large. */
@Composable
private fun StudyTabRow(tabs: List<PartyTab>, selected: PartyTab, onSelect: (PartyTab) -> Unit) {
    val colors = PixelTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .selectableGroup()
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selected
            Text(
                text = stringResource(tabLabel(tab)),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) colors.onPrimary else colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .studyChip(if (isSelected) colors.primary else colors.surface)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
                    .testTag(StudyTags.tabButton(tab))
            )
        }
    }
}

fun tabLabel(tab: PartyTab): Int = when (tab) {
    PartyTab.THIS_WEEK -> R.string.study_tab_this_week
    PartyTab.ALL_ITEMS -> R.string.study_tab_all_items
    PartyTab.SCOREBOARD -> R.string.study_tab_scoreboard
    PartyTab.PEOPLE -> R.string.study_tab_people
    PartyTab.CHAT -> R.string.party_study_title
}

@Composable
private fun NoticeLine(notice: StudyNotice, onDismiss: () -> Unit) {
    Text(
        text = stringResource(noticeText(notice)),
        style = MaterialTheme.typography.bodySmall,
        color = PixelTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = false, role = Role.Button, onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(StudyTags.NOTICE)
    )
}

fun noticeText(notice: StudyNotice): Int = when (notice) {
    StudyNotice.Offline -> R.string.study_offline
    StudyNotice.TooShort -> R.string.study_timer_too_short
    is StudyNotice.Refused -> when (notice.refusal) {
        StudyRefusal.NOT_ALLOWED -> R.string.study_refused_not_allowed
        StudyRefusal.NOT_LEADER -> R.string.study_refused_not_leader
        StudyRefusal.BAD_DAY -> R.string.study_refused_bad_day
        StudyRefusal.NO_SUCH_ITEM -> R.string.study_refused_no_item
        else -> R.string.study_refused_other
    }
}

private fun LazyListScope.emptyLine(text: Int, loaded: Boolean) {
    item(key = "empty") {
        Text(
            text = stringResource(if (loaded) text else R.string.study_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = PixelTheme.colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().testTag(StudyTags.EMPTY)
        )
    }
}

/** The top of a tab's list, its pieces spaced as the list's items are. */
@Composable
private fun TopOfList(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) { content() }
}

/** This week's items under overdue, each day, and no date, then the party's recent activity. */
private fun LazyListScope.thisWeek(state: StudyPartyUiState, onOpenItem: (String) -> Unit, actions: ItemActions) {
    if (state.thisWeek.isEmpty()) {
        emptyLine(R.string.study_week_empty, state.loaded)
        return activityFeed(state)
    }
    state.weekSections.forEach { (section, rows) ->
        item(key = "section_" + sectionKey(section)) { SectionHeading(sectionLabel(section, state.today)) }
        items(rows, key = { it.item.id }) { row -> CardFor(state, row, onOpenItem, actions) }
    }
    activityFeed(state)
}

/** What the player may do to an item from its card's ⋯ menu; each takes the item's id. */
data class ItemActions(
    val onEdit: (String) -> Unit = {},
    val onTogglePin: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onDone: (String) -> Unit = {},
    val onUndo: (String) -> Unit = {},
    /** A refused tick's own key: tried again, or dropped. */
    val onRetry: (String) -> Unit = {},
    val onDiscard: (String) -> Unit = {}
)

/** An item's card, with the menu its creator or the leader may use. */
@Composable
private fun CardFor(state: StudyPartyUiState, row: ItemRow, onOpenItem: (String) -> Unit, actions: ItemActions) {
    val id = row.item.id
    ItemCard(
        row = row,
        onOpen = { onOpenItem(id) },
        onEdit = if (row.canEdit) ({ actions.onEdit(id) }) else null,
        onTogglePin = if (state.isLeader) ({ actions.onTogglePin(id) }) else null,
        onDelete = if (row.canEdit) ({ actions.onDelete(id) }) else null,
        onDone = { actions.onDone(id) },
        onUndo = { actions.onUndo(id) },
        onRetry = row.mine?.clientId?.let { key -> { actions.onRetry(key) } },
        onDiscard = row.mine?.clientId?.let { key -> { actions.onDiscard(key) } }
    )
}

/** The week's line: items done of those this week, and the player's points so far. */
@Composable
private fun WeekSummary(state: StudyPartyUiState) {
    val colors = PixelTheme.colors
    PixelCard(variant = PixelPanelVariant.BEIGE, contentPadding = 16.dp, modifier = Modifier.fillMaxWidth().testTag(StudyTags.WEEK_SUMMARY)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.study_week_progress, state.weekProgress.done, state.weekProgress.total),
                style = MaterialTheme.typography.titleSmall,
                color = inkOnPanel(colors.primaryText)
            )
            if (!state.simpleMode) {
                val points = state.myScore?.points ?: 0
                Text(
                    text = stringResource(R.string.study_week_points, pluralStringResource(R.plurals.study_points, points, points)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.testTag(StudyTags.WEEK_POINTS)
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = PixelTheme.colors.primaryText,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() }
    )
}

private fun sectionKey(section: WeekSection): String = when (section) {
    WeekSection.Overdue -> "overdue"
    WeekSection.NoDate -> "none"
    is WeekSection.Day -> section.date.toString()
}

/** "Overdue", "Today", "Tomorrow", "Friday 23 Oct" or "No due date". */
@Composable
fun sectionLabel(section: WeekSection, today: LocalDate): String = when (section) {
    WeekSection.Overdue -> stringResource(R.string.study_section_overdue)
    WeekSection.NoDate -> stringResource(R.string.study_section_no_date)
    is WeekSection.Day -> when (section.date) {
        today -> stringResource(R.string.study_section_today)
        today.plusDays(1) -> stringResource(R.string.study_section_tomorrow)
        else -> section.date.format(DateTimeFormatter.ofPattern("EEEE d MMM", LocalConfiguration.current.locales[0]))
    }
}

private fun LazyListScope.itemCards(state: StudyPartyUiState, rows: List<ItemRow>, empty: Int, onOpenItem: (String) -> Unit, actions: ItemActions) {
    if (rows.isEmpty()) return emptyLine(empty, state.loaded)
    items(rows, key = { it.item.id }) { row -> CardFor(state, row, onOpenItem, actions) }
}

private fun LazyListScope.people(state: StudyPartyUiState, onAddPeople: () -> Unit, onOpenInfo: () -> Unit) {
    item(key = "members") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
            MemberList(
                members = state.members.filter { it.status == MemberStatus.ACTIVE || it.status == MemberStatus.INVITED },
                myId = state.myId,
                isLeader = false,
                enabled = false,
                onRemove = {},
                onTransfer = {}
            )
        }
    }
    if (state.isLeader) {
        item(key = "add") {
            PixelButton(text = stringResource(R.string.study_add_people), onClick = onAddPeople, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(StudyTags.ADD_PEOPLE))
        }
    }
    item(key = "info") {
        PixelButton(text = stringResource(R.string.study_open_info), onClick = onOpenInfo, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
    }
}

/** The player's study notifications on this phone, and turning each on or off. */
data class ReminderToggles(
    val due: Boolean = false,
    val onDue: (Boolean) -> Unit = {},
    val weekly: Boolean = false,
    val onWeekly: (Boolean) -> Unit = {}
)

private fun LazyListScope.reminderToggles(reminders: ReminderToggles, simpleMode: Boolean) {
    item(key = "reminders_title") {
        Text(
            text = stringResource(R.string.study_reminders_title),
            style = MaterialTheme.typography.titleSmall,
            color = PixelTheme.colors.primaryText,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() }
        )
    }
    item(key = "reminders_due") {
        ToggleRow(stringResource(R.string.study_reminders_due), stringResource(R.string.study_reminders_due_body), reminders.due, reminders.onDue, StudyTags.REMINDERS_DUE)
    }
    // The summary is points and places: Simple Mode has none.
    if (!simpleMode) item(key = "reminders_weekly") {
        ToggleRow(stringResource(R.string.study_reminders_weekly), stringResource(R.string.study_reminders_weekly_body), reminders.weekly, reminders.onWeekly, StudyTags.REMINDERS_WEEKLY)
    }
}

/** A setting that's on or off: its name, a line about it, and a switch; the whole row toggles it. */
@Composable
fun ToggleRow(title: String, body: String, on: Boolean, onChange: (Boolean) -> Unit, tag: String) {
    val colors = PixelTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .testTag(tag)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        androidx.compose.material3.Switch(checked = on, onCheckedChange = null)
    }
}

/** The study party screen's test tags. */
object StudyTags {
    const val SCREEN = "study_screen"
    const val INFO = "study_info"
    const val NOTICE = "study_notice"
    const val EMPTY = "study_empty"
    const val ADD_PEOPLE = "study_add_people"
    const val WEEK_SUMMARY = "study_week_summary"
    const val WEEK_POINTS = "study_week_points"
    const val PINNED = "study_pinned"
    const val ADD_ITEM = "study_add_item"
    const val FORM = "study_form"
    const val FORM_TITLE = "study_form_title"
    const val FORM_SUBJECT = "study_form_subject"
    const val FORM_NOTES = "study_form_notes"
    const val FORM_NO_DATE = "study_form_no_date"
    const val FORM_PROBLEM = "study_form_problem"
    const val FORM_PIN = "study_form_pin"
    const val FORM_SAVE = "study_form_save"
    fun formKind(kind: com.pixelquest.app.study.domain.StudyItemKind) = "study_form_kind_${kind.name}"
    fun formDay(day: LocalDate) = "study_form_day_$day"
    fun formTime(time: java.time.LocalTime) = "study_form_time_$time"
    fun pinned(id: String) = "study_pinned_$id"
    fun tab(tab: PartyTab) = "study_tab_${tab.name}"
    fun tabButton(tab: PartyTab) = "study_tab_button_${tab.name}"
    fun item(id: String) = "study_item_$id"
    fun itemDue(id: String) = "study_item_due_$id"
    fun itemMine(id: String) = "study_item_mine_$id"
    fun itemMenu(id: String) = "study_item_menu_$id"
    const val ITEM_SCREEN = "study_item_screen"
    const val ITEM_NOTES = "study_item_notes"
    fun doneBy(userId: String) = "study_done_by_$userId"
    const val TIMER = "study_timer"
    const val TIMER_TODAY = "study_timer_today"
    const val TIMER_START = "study_timer_start"
    const val TIMER_RUNNING = "study_timer_running"
    const val TIMER_STOP = "study_timer_stop"
    const val TIMER_CANCEL = "study_timer_cancel"
    const val LOG_MINUTES = "study_log_minutes"
    fun minutes(m: Int) = "study_minutes_$m"
    const val POINTS_RULES = "study_points_rules"
    const val ACTIVITY = "study_activity"
    const val REMINDERS_DUE = "study_reminders_due"
    const val REMINDERS_WEEKLY = "study_reminders_weekly"
    const val HOME_CARD = "study_home_card"
    fun homeLine(itemId: String) = "study_home_$itemId"
    const val WINNER_BANNER = "study_winner_banner"
    const val WINNER_DISMISS = "study_winner_dismiss"
    fun pastWeek(weekStart: String) = "study_past_$weekStart"
    fun boardRow(userId: String) = "study_board_$userId"
    fun boardPlace(userId: String) = "study_board_place_$userId"
    fun boardPoints(userId: String) = "study_board_points_$userId"
    fun itemDone(id: String) = "study_item_done_$id"
    fun itemUndo(id: String) = "study_item_undo_$id"
    fun itemRetry(id: String) = "study_item_retry_$id"
    fun itemDiscard(id: String) = "study_item_discard_$id"
}
