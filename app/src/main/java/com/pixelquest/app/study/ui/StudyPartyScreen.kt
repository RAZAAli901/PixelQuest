package com.pixelquest.app.study.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.PartyRefreshBox
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.ui.components.MemberList
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqSwitch
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    viewModel: StudyPartyViewModel = hiltViewModel(),
    /** The party's reminders (docs/study-reminders.md); without one (a test), the screen has none. */
    remindersViewModel: StudyRemindersViewModel? = null
) {
    val state by viewModel.uiState.collectAsState()
    val reminderState by (remindersViewModel?.uiState ?: remember { kotlinx.coroutines.flow.MutableStateFlow(ReminderUiState()) }).collectAsState()
    val dueReminders by viewModel.dueReminders.collectAsState()
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val askForNotifications = com.pixelquest.app.ui.notifications.rememberAskForNotifications()
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
        reminders = ReminderToggles(
            dueReminders,
            { on -> if (on) askForNotifications(); viewModel.setDueReminders(on) },
            weeklySummary,
            { on -> if (on) askForNotifications(); viewModel.setWeeklySummary(on) }
        ),
        reminderState = reminderState,
        reminderActions = remindersViewModel?.let { vm ->
            ReminderActions(
                onAdd = { askForNotifications(); vm.openCreate() },
                onChange = vm::change,
                onSave = vm::save,
                onClose = vm::close,
                onDismissNotice = vm::dismissNotice
            )
        }
    )
}

/** What the reminders' part of the screen can do (docs/study-reminders.md). */
data class ReminderActions(
    val onAdd: () -> Unit = {},
    val onChange: (ReminderForm) -> Unit = {},
    val onSave: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onDismissNotice: () -> Unit = {}
)

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
    reminders: ReminderToggles = ReminderToggles(),
    reminderState: ReminderUiState = ReminderUiState(),
    /** Null when the screen has no reminders (a test without them): no Add reminder. */
    reminderActions: ReminderActions? = null
) {
    CompositionLocalProvider(LocalStudySimpleMode provides state.simpleMode) {
        StudyPartyScaffold(state, onNavigateBack, onOpenInfo, onAddPeople, onSelectTab, onRefresh, onDismissNotice, onOpenItem, onAddItem, onFormChange, onSaveForm, onCloseForm, actions, onConfirmDelete, onCancelDelete, timerActions, onDismissWinners, reminders, reminderState, reminderActions)
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
    reminders: ReminderToggles,
    reminderState: ReminderUiState,
    reminderActions: ReminderActions?
) {
    Scaffold(
        containerColor = PqColors.Ground,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().background(PqColors.Ground).padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp).heightIn(min = 56.dp)
            ) {
                PqIconButton(Icons.Rounded.ArrowBack, stringResource(R.string.navigate_back), onNavigateBack)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = (state.party?.name ?: stringResource(R.string.party_study_title)).uppercase(),
                        style = PqType.Title.copy(color = PqColors.Title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = if (state.memberCount > 0) {
                            stringResource(R.string.pq_party_card_line, stringResource(R.string.party_study_title), pluralStringResource(R.plurals.party_members, state.memberCount, state.memberCount))
                        } else {
                            stringResource(R.string.party_study_title)
                        },
                        style = PqType.Meta,
                        maxLines = 1
                    )
                }
                PqIconButton(Icons.Rounded.Info, stringResource(R.string.study_open_info), onOpenInfo, Modifier.testTag(StudyTags.INFO))
            }
        },
        floatingActionButton = {
            if (state.tab == PartyTab.THIS_WEEK || state.tab == PartyTab.ALL_ITEMS) {
                val label = stringResource(R.string.study_add_item)
                // ADD ITEM, on red with its plus (mock 4H).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .semantics(mergeDescendants = true) { contentDescription = label }
                        .testTag(StudyTags.ADD_ITEM)
                        .height(52.dp)
                        .comicPressable(onAddItem, bg = PqColors.Red, radius = 16.dp, shadow = PqDimens.ShadowLg, role = Role.Button)
                        .padding(start = 14.dp, end = 18.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = PqColors.OnRed, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = label.uppercase(), style = PqType.Button.copy(color = PqColors.OnRed), modifier = Modifier.clearAndSetSemantics { })
                }
            }
        },
        modifier = Modifier.testTag(StudyTags.SCREEN)
    ) { innerPadding ->
        state.confirmingDelete?.let { row ->
            PqConfirmDialog(
                title = stringResource(R.string.study_delete_title),
                message = stringResource(R.string.study_delete_message, row.item.title),
                confirm = stringResource(R.string.study_delete),
                dismiss = stringResource(R.string.study_keep),
                onConfirm = onConfirmDelete,
                onDismiss = onCancelDelete
            )
        }
        state.form?.let { form ->
            ItemSheet(form = form, today = state.today, isLeader = state.isLeader, onChange = onFormChange, onSave = onSaveForm, onDismiss = onCloseForm)
        }
        reminderState.form?.let { form ->
            val acts = reminderActions ?: ReminderActions()
            ReminderSheet(
                form = form, today = reminderState.today, zone = reminderState.zone, onChange = acts.onChange,
                onSave = acts.onSave, onDismiss = acts.onClose
            )
        }
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            StudyTabRow(tabs = state.tabs, selected = state.tab, onSelect = onSelectTab)
            state.notice?.let { NoticeLine(it, onDismissNotice) }
            reminderState.notice?.let { ReminderNoticeLine(it, reminderActions?.onDismissNotice ?: {}) }
            PartyRefreshBox(refreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    // Room at the end for the Add item button over the last card.
                    contentPadding = PaddingValues(start = PqDimens.ScreenPad, top = 12.dp, end = PqDimens.ScreenPad, bottom = 96.dp),
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
                                    if (reminderActions != null) AddReminderButton(reminderActions.onAdd)
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

/** The tabs, side by side in one inked bar (mock 4H), the chosen one filled. */
@Composable
private fun StudyTabRow(tabs: List<PartyTab>, selected: PartyTab, onSelect: (PartyTab) -> Unit) {
    val shape = pqShape(12.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .padding(horizontal = PqDimens.ScreenPad, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(PqColors.Paper)
            .border(PqDimens.Border, PqColors.Ink, shape)
            .padding(3.dp)
            .selectableGroup()
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(pqShape(9.dp))
                    .background(if (isSelected) PqColors.Ink else PqColors.Paper)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .testTag(StudyTags.tabButton(tab))
            ) {
                Text(
                    text = stringResource(tabLabel(tab)),
                    style = PqType.MetaStrong.copy(color = if (isSelected) PqColors.Paper else PqColors.Ink),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
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
        style = PqType.Meta.copy(color = PqColors.Ink2),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = false, role = Role.Button, onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(StudyTags.NOTICE)
    )
}

@Composable
private fun ReminderNoticeLine(notice: ReminderNotice, onDismiss: () -> Unit) {
    val text = when (notice) {
        ReminderNotice.SavedOffline -> R.string.study_reminder_saved_offline
        is ReminderNotice.Refused -> if (notice.refusal == StudyRefusal.TOO_MANY_REMINDERS) R.string.study_reminder_too_many else noticeText(StudyNotice.Refused(notice.refusal))
    }
    Text(
        text = stringResource(text),
        style = PqType.Meta.copy(color = PqColors.Ink2),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = false, role = Role.Button, onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(ReminderTags.NOTICE)
    )
}

/** Add reminder, on This week: for a quiz, an exam or anything the party should be told of in time. */
@Composable
private fun AddReminderButton(onAdd: () -> Unit) {
    PqButton(
        text = stringResource(R.string.study_reminder_add),
        onClick = onAdd,
        style = PqButtonStyle.Secondary,
        icon = Icons.Rounded.NotificationsActive,
        modifier = Modifier.fillMaxWidth().testTag(ReminderTags.ADD)
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
            style = PqType.Body.copy(color = PqColors.Ink2),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag(StudyTags.EMPTY)
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag(StudyTags.WEEK_SUMMARY).semantics(mergeDescendants = true) { }
    ) {
        PqGroupLabel(stringResource(R.string.study_week_progress, state.weekProgress.done, state.weekProgress.total), Modifier.weight(1f))
        if (!state.simpleMode) {
            val points = state.myScore?.points ?: 0
            Text(
                text = stringResource(R.string.study_week_points, pluralStringResource(R.plurals.study_points, points, points)),
                style = PqType.MetaStrong,
                modifier = Modifier.testTag(StudyTags.WEEK_POINTS)
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    PqGroupLabel(text, Modifier.semantics { heading() })
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
            PqButton(text = stringResource(R.string.study_add_people), onClick = onAddPeople, icon = Icons.Rounded.PersonAdd, modifier = Modifier.fillMaxWidth().testTag(StudyTags.ADD_PEOPLE))
        }
    }
    item(key = "info") {
        PqButton(text = stringResource(R.string.study_open_info), onClick = onOpenInfo, style = PqButtonStyle.Secondary, icon = Icons.Rounded.Info, modifier = Modifier.fillMaxWidth())
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
        PqGroupLabel(stringResource(R.string.study_reminders_title), Modifier.padding(top = 8.dp).semantics { heading() })
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .heightIn(min = 48.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = PqType.BodyStrong)
            Text(text = body, style = PqType.Meta.copy(color = PqColors.Ink2))
        }
        PqSwitch(checked = on, onCheckedChange = null)
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
