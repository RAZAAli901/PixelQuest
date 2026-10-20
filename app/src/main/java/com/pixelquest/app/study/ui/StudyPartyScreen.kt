package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.pixelquest.app.ui.components.PixelButton
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
    viewModel: StudyPartyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    StudyPartyContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onOpenInfo = onOpenInfo,
        onAddPeople = onAddPeople,
        onSelectTab = viewModel::selectTab,
        onRefresh = viewModel::refresh,
        onDismissNotice = viewModel::dismissNotice
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
    onDismissNotice: () -> Unit = {}
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
        modifier = Modifier.testTag(StudyTags.SCREEN)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            StudyTabRow(tabs = state.tabs, selected = state.tab, onSelect = onSelectTab)
            state.notice?.let { NoticeLine(it, onDismissNotice) }
            PartyRefreshBox(refreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag(StudyTags.tab(state.tab))
                ) {
                    when (state.tab) {
                        PartyTab.THIS_WEEK -> itemLines(state.thisWeek, R.string.study_week_empty, state.loaded)
                        PartyTab.ALL_ITEMS -> itemLines(state.items, R.string.study_items_empty, state.loaded)
                        PartyTab.SCOREBOARD -> boardLines(state)
                        PartyTab.PEOPLE -> people(state, onAddPeople, onOpenInfo)
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.primary else colors.surface)
                    .border(1.dp, colors.pixelBorder, RoundedCornerShape(8.dp))
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

private fun LazyListScope.itemLines(rows: List<ItemRow>, empty: Int, loaded: Boolean) {
    if (rows.isEmpty()) return emptyLine(empty, loaded)
    items(rows, key = { it.item.id }) { row ->
        Text(text = row.item.title, style = MaterialTheme.typography.bodyLarge, color = PixelTheme.colors.onSurface, modifier = Modifier.testTag(StudyTags.item(row.item.id)))
    }
}

private fun LazyListScope.boardLines(state: StudyPartyUiState) {
    if (state.board.none { it.points > 0 }) return emptyLine(R.string.study_board_empty, state.loaded)
    items(state.board, key = { "board_" + it.userId }) { line ->
        val name = line.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)
        Text(
            text = stringResource(R.string.study_board_line, line.place, name, pluralStringResource(R.plurals.study_points, line.points, line.points)),
            style = MaterialTheme.typography.bodyLarge,
            color = PixelTheme.colors.onSurface
        )
    }
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
            PixelButton(text = stringResource(R.string.study_add_people), onClick = onAddPeople, modifier = Modifier.fillMaxWidth().testTag(StudyTags.ADD_PEOPLE))
        }
    }
    item(key = "info") {
        PixelButton(text = stringResource(R.string.study_open_info), onClick = onOpenInfo, modifier = Modifier.fillMaxWidth())
    }
}

/** The study party screen's test tags. */
object StudyTags {
    const val SCREEN = "study_screen"
    const val INFO = "study_info"
    const val NOTICE = "study_notice"
    const val EMPTY = "study_empty"
    const val ADD_PEOPLE = "study_add_people"
    fun tab(tab: PartyTab) = "study_tab_${tab.name}"
    fun tabButton(tab: PartyTab) = "study_tab_button_${tab.name}"
    fun item(id: String) = "study_item_$id"
}
