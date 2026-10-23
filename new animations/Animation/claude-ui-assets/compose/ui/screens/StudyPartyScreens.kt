package com.pixelquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.ui.components.*
import com.pixelquest.ui.theme.*

/* STUDY PARTY — mocks 4H–4J (comic) / 4R–4T (pixel). Route: parties/{id} when type == Study. */

enum class StudyTab(val label: String) { Week("Week"), Items("Items"), Scores("Scores"), People("People") }
enum class StudyItemType(val label: String, val icon: ImageVector) {
    Assignment("Assignment", Icons.Rounded.Description), Quiz("Quiz", Icons.Rounded.Quiz);
    val tint: Color get() = if (this == Assignment) PqColors.Amber else PqColors.Lilac
}

data class StudyItemUi(
    val id: String, val type: StudyItemType, val title: String, val subject: String?,
    val dueLabel: String?,            // "Fri 23 Oct", "Tomorrow", null = no date
    val pinned: Boolean, val done: Boolean, val overdue: Boolean = false,
)

data class ScoreUi(val handle: String, val points: Int, val items: Int, val earlyBonus: Int, val minutes: Int, val you: Boolean)

data class StudyPartyUiState(
    val name: String,
    val members: Int,
    val tab: StudyTab,
    val minutesToday: Int,
    val timerRunning: Boolean,
    val timerLabel: String = "00:00",
    val thisWeek: List<StudyItemUi>,
    val allItems: List<StudyItemUi>,
    val weekLabel: String,            // "Mon 19 – Sun 25 Oct"
    val scores: List<ScoreUi>,        // sorted desc
    val people: List<MemberUi>,
)

@Composable
fun StudyPartyScreen(
    state: StudyPartyUiState,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onTab: (StudyTab) -> Unit,
    onToggleTimer: () -> Unit,
    onLogMinutes: (Int) -> Unit,
    onToggleItem: (StudyItemUi) -> Unit,
    onItemClick: (StudyItemUi) -> Unit,
    onAddItem: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Box(Modifier.fillMaxSize().background(PqColors.Ground)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PqIconButton(Icons.Rounded.ArrowBack, "Back", onBack)
                    Column(Modifier.weight(1f)) {
                        Text(state.name.uppercase(), style = PqType.Title.copy(color = PqColors.Title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Study party · ${state.members} members", style = PqType.Meta)
                    }
                    PqIconButton(Icons.Rounded.Info, "Party info", onInfo)
                }
            }
            item { PqSegmented(StudyTab.entries.map { it.label }, state.tab.ordinal, { onTab(StudyTab.entries[it]) }) }
            when (state.tab) {
                StudyTab.Week -> {
                    item { TimerCard(state, onToggleTimer, onLogMinutes) }
                    item { PqGroupLabel("Due this week · ${state.thisWeek.size}") }
                    if (state.thisWeek.isEmpty()) item { EmptyLine("Nothing due this week. Add an assignment or quiz so the party can see it.") }
                    items(state.thisWeek, key = { it.id }) { StudyItemRow(it, { onToggleItem(it) }, { onItemClick(it) }) }
                }
                StudyTab.Items -> {
                    if (state.allItems.isEmpty()) item { EmptyLine("No items yet. Add what's due, and everyone in the party will see it.") }
                    items(state.allItems, key = { it.id }) { StudyItemRow(it, { onToggleItem(it) }, { onItemClick(it) }) }
                }
                StudyTab.Scores -> {
                    item {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(state.weekLabel.uppercase(), style = PqType.Label, modifier = Modifier.weight(1f))
                            Text("Resets Monday", style = PqType.Meta)
                        }
                    }
                    item { Scoreboard(state.scores) }
                    item { PqGroupLabel("How points work") }
                    item { PointRules() }
                }
                StudyTab.People -> item {
                    PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
                        Column {
                            state.people.forEachIndexed { i, m ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    InitialAvatar(m.handle)
                                    Column(Modifier.weight(1f)) { Text(m.handle + if (m.you) " (you)" else "", style = PqType.BodyStrong); Text("Joined ${m.joined}", style = PqType.Meta) }
                                    if (m.leader) PqChip("LEADER", PqColors.Gold)
                                }
                                if (i != state.people.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
                            }
                        }
                    }
                }
            }
        }
        if (state.tab == StudyTab.Week || state.tab == StudyTab.Items) {
            Row(
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = PqDimens.ScreenPad, bottom = contentPadding.calculateBottomPadding() + 18.dp)
                    .height(52.dp).comicPressable(onAddItem, bg = PqColors.Red, radius = 16.dp, shadow = PqDimens.ShadowLg)
                    .padding(start = 14.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, tint = PqColors.OnRed, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Text("ADD ITEM", style = PqType.Button.copy(color = PqColors.OnRed))
            }
        }
    }
}

@Composable private fun EmptyLine(text: String) = Text(text, style = PqType.Body.copy(color = PqColors.Ink2), modifier = Modifier.padding(vertical = 8.dp))

@Composable
private fun TimerCard(state: StudyPartyUiState, onToggleTimer: () -> Unit, onLogMinutes: (Int) -> Unit) {
    PqCard(shadow = PqDimens.ShadowLg) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("STUDY TIME", style = PqType.CardTitle, modifier = Modifier.weight(1f))
                Text("${state.minutesToday} min today", style = PqType.MetaStrong)
            }
            PqButton(
                if (state.timerRunning) "Stop · ${state.timerLabel}" else "Start timer", onToggleTimer, Modifier.fillMaxWidth(),
                icon = if (state.timerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, height = 50.dp,
            )
            Text("Or log minutes you already studied", style = PqType.Meta)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15, 25, 30, 45, 60).forEach { m ->
                    Box(Modifier.weight(1f).height(36.dp).comicPressable({ onLogMinutes(m) }, radius = 18.dp, shadow = 0.dp), contentAlignment = Alignment.Center) {
                        Text("${m}m", style = PqType.MetaStrong)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyItemRow(item: StudyItemUi, onToggle: () -> Unit, onClick: () -> Unit) {
    val base = if (item.done) Modifier.comic(PqColors.Sunken, PqDimens.Radius, shadow = 0.dp) else Modifier.comicPressable(onClick, radius = PqDimens.Radius)
    Row(base.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryTile(item.type.icon, item.type.tint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = PqType.BodyStrong.copy(color = if (item.done) PqColors.Muted else PqColors.Ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (item.pinned) Icon(Icons.Rounded.PushPin, "Pinned", tint = PqColors.RedText, modifier = Modifier.size(16.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.overdue && !item.done) PqTag("Overdue")
                Text(listOfNotNull(item.type.label, item.subject, item.dueLabel?.let { "Due $it" }).joinToString(" · "), style = PqType.Meta, maxLines = 1)
            }
        }
        CheckCircle(item.done, onToggle, size = 38.dp)
    }
}

@Composable
private fun Scoreboard(scores: List<ScoreUi>) {
    PqCard(shadow = PqDimens.ShadowLg) {
        Column {
            scores.forEachIndexed { i, s ->
                val medal = when (i) { 0 -> PqColors.Gold; 1 -> Color(0xFFD9D4CC); 2 -> PqColors.Amber; else -> PqColors.Paper }
                Row(
                    Modifier.fillMaxWidth().background(if (s.you) PqColors.Sky else Color.Transparent).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(30.dp).comic(bg = medal, radius = 15.dp, border = PqDimens.BorderThin, shadow = 0.dp), contentAlignment = Alignment.Center) { Text("${i + 1}", style = PqType.Button) }
                    Column(Modifier.weight(1f)) {
                        Text(s.handle + if (s.you) " (you)" else "", style = PqType.BodyStrong)
                        Text(buildString { append("${s.items} item${if (s.items == 1) "" else "s"}"); if (s.earlyBonus > 0) append(" · ${s.earlyBonus} early"); append(" · ${s.minutes} min") }, style = PqType.Meta.copy(color = PqColors.Ink2))
                    }
                    Column(horizontalAlignment = Alignment.End) { Text("${s.points}", style = PqType.Number); Text("pts", style = PqType.Meta.copy(color = PqColors.Ink2)) }
                }
                if (i != scores.lastIndex) Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
            }
        }
    }
}

@Composable
private fun PointRules() {
    PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
        Column {
            listOf(
                Triple(Icons.Rounded.Description, "Assignment done", "+10"),
                Triple(Icons.Rounded.Quiz, "Quiz done", "+15"),
                Triple(Icons.Rounded.Bolt, "Done a day or more early", "+5"),
                Triple(Icons.Rounded.Timer, "Every 10 min studied", "+1 · max 12/day"),
            ).forEach { (icon, l, v) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
                    Text(l, style = PqType.Body, modifier = Modifier.weight(1f))
                    Text(v, style = PqType.MetaStrong.copy(color = PqColors.RedText))
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
            }
            Text("Party points stay in this party. They never change your XP, level or streak.", style = PqType.Meta, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
        }
    }
}

// =====================  ADD ITEM SHEET  =====================

data class StudyItemDraft(
    val type: StudyItemType = StudyItemType.Assignment,
    val title: String = "",
    val subject: String = "",
    val notes: String = "",
    val dueOption: Int = 0,           // index into dueOptions; last = custom date
    val customDueLabel: String? = null,
    val pinned: Boolean = false,
) { val canAdd get() = title.isNotBlank() }

/** dueOptions example: ["No date", "Today", "Tomorrow", "Fri 23", "Sat 24"]. A trailing calendar chip opens a DatePicker. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddStudyItemSheet(
    draft: StudyItemDraft,
    dueOptions: List<String>,
    onChange: (StudyItemDraft) -> Unit,
    onPickDate: () -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    PqSheet(onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ADD ITEM", style = PqType.Title.copy(color = PqColors.Title))
            PqSegmented(StudyItemType.entries.map { it.label }, draft.type.ordinal, { onChange(draft.copy(type = StudyItemType.entries[it])) })
            PqFieldLabel("Title")
            PqTextField(draft.title, { onChange(draft.copy(title = it)) }, if (draft.type == StudyItemType.Quiz) "e.g. Calc quiz 3" else "e.g. Physics lab report", maxLength = 80)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PqTextField(draft.subject, { onChange(draft.copy(subject = it)) }, "Subject", Modifier.weight(1f))
                PqTextField(draft.notes, { onChange(draft.copy(notes = it)) }, "Notes (optional)", Modifier.weight(1f))
            }
            PqFieldLabel("Due")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dueOptions.forEachIndexed { i, l -> PqChoiceChip(l, draft.dueOption == i && draft.customDueLabel == null, { onChange(draft.copy(dueOption = i, customDueLabel = null)) }) }
                PqChoiceChip(draft.customDueLabel ?: "Pick…", draft.customDueLabel != null, onPickDate, icon = Icons.Rounded.CalendarMonth)
            }
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.PushPin, null, tint = PqColors.Ink)
                Column(Modifier.weight(1f)) { Text("Pin for the party", style = PqType.BodyStrong); Text("Stays at the top of everyone's week", style = PqType.Meta) }
                PqSwitch(draft.pinned, { onChange(draft.copy(pinned = it)) })
            }
            PqButton("Add it", onAdd, Modifier.fillMaxWidth(), enabled = draft.canAdd, height = 52.dp)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun StudyPreview() = PixelQuestTheme {
    StudyPartyScreen(
        StudyPartyUiState(
            "Bros", 3, StudyTab.Week, 30, false,
            thisWeek = listOf(
                StudyItemUi("1", StudyItemType.Assignment, "Physics lab report", "Physics", "Fri 23 Oct", pinned = true, done = false),
                StudyItemUi("2", StudyItemType.Quiz, "Calc quiz 3", "Maths", "Tomorrow", pinned = false, done = false),
            ),
            allItems = emptyList(), weekLabel = "Mon 19 – Sun 25 Oct",
            scores = listOf(ScoreUi("@sara", 48, 3, 1, 90, false), ScoreUi("@raza", 33, 2, 0, 30, true), ScoreUi("@ali", 21, 1, 0, 60, false)),
            people = emptyList(),
        ),
        {}, {}, {}, {}, {}, {}, {}, {},
    )
}
