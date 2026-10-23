package com.pixelquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.ui.components.*
import com.pixelquest.ui.theme.*
import java.text.NumberFormat

/*
 * PARTIES — mocks 4B–4G (comic) / 4L–4Q (pixel).
 * Entry points (the drawer is gone): Home header "groups" button, Deen > Darood Party tile, Hero list "Parties".
 * Routes: parties, parties/new, parties/{id} (Darood room or Study party), parties/{id}/info
 */

enum class PartyType(val label: String, val icon: ImageVector) {
    Darood("Darood", Icons.Rounded.VolunteerActivism),
    Study("Study", Icons.Rounded.School);
    val tint: Color get() = if (this == Darood) PqColors.Amber else PqColors.Lilac
}

data class PartyUi(
    val id: String,
    val name: String,
    val type: PartyType,
    val members: Int,
    // Darood
    val total: Int = 0,
    val milestone: Int? = null,
    val youToday: Int = 0,
    // Study
    val dueThisWeek: Int = 0,
    val yourPoints: Int = 0,
    val yourPlace: Int = 0,
)

data class PartiesUiState(
    val filter: PartyType?,          // null = All
    val parties: List<PartyUi>,
    val fridayNote: Boolean,
    val username: String,            // "raza"
)

private val nf = NumberFormat.getIntegerInstance()
private fun Int.fmt() = nf.format(this)
private fun ordinal(n: Int) = n.toString() + when { n % 100 in 11..13 -> "th"; n % 10 == 1 -> "st"; n % 10 == 2 -> "nd"; n % 10 == 3 -> "rd"; else -> "th" }

// =====================  HUB + EMPTY  =====================

@Composable
fun PartiesScreen(
    state: PartiesUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onFilter: (PartyType?) -> Unit,
    onOpenParty: (PartyUi) -> Unit,
    onFridayNote: (Boolean) -> Unit,
    onShareUsername: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val visible = state.parties.filter { state.filter == null || it.type == state.filter }
    LazyColumn(
        Modifier.fillMaxSize().background(PqColors.Ground),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PqTopBar("Parties", onBack = onBack) {
                if (state.parties.isNotEmpty()) Box(Modifier.size(40.dp).comicPressable(onCreate, bg = PqColors.Red, radius = 12.dp, shadow = 2.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, "New party", tint = PqColors.OnRed)
                }
            }
        }
        if (state.parties.isEmpty()) {
            item { PartiesEmpty(state.username, onCreate, onShareUsername) }
            return@LazyColumn
        }
        item {
            val opts = listOf<PartyType?>(null, PartyType.Darood, PartyType.Study)
            PqSegmented(
                listOf("All · ${state.parties.size}", "Darood", "Study"), opts.indexOf(state.filter), { onFilter(opts[it]) },
            )
        }
        items(visible, key = { it.id }) { p -> PartyCard(p) { onOpenParty(p) } }
        item {
            Row(
                Modifier.fillMaxWidth().comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Bedtime, null, tint = PqColors.Ink)
                Column(Modifier.weight(1f)) {
                    Text("Friday note", style = PqType.BodyStrong)
                    Text("A quiet Friday-evening summary of the days you added Darood.", style = PqType.Meta.copy(color = PqColors.Ink2))
                }
                PqSwitch(state.fridayNote, onFridayNote)
            }
        }
        item { Text("Invites from friends show up here. Your username is @${state.username}.", style = PqType.Meta) }
    }
}

@Composable
private fun PartyCard(p: PartyUi, onClick: () -> Unit) {
    PqCard(onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(p.type.icon, p.type.tint)
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = PqType.BodyStrong)
                    Text("${p.type.label} · ${p.members} member${if (p.members == 1) "" else "s"}", style = PqType.Meta)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = PqColors.Ink)
            }
            when (p.type) {
                PartyType.Darood -> {
                    if (p.milestone != null) PqProgressBar(p.total / p.milestone.toFloat())
                    Row {
                        Text(if (p.milestone != null) "${p.total.fmt()} of ${p.milestone.fmt()}" else "${p.total.fmt()} total", style = PqType.MetaStrong, modifier = Modifier.weight(1f))
                        if (p.youToday > 0) Text("You today +${p.youToday.fmt()}", style = PqType.MetaStrong.copy(color = PqColors.RedText))
                    }
                }
                PartyType.Study -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (p.dueThisWeek > 0) PqChip("${p.dueThisWeek} due this week", PqColors.Gold)
                    PqChip("You: ${p.yourPoints} pts · ${ordinal(p.yourPlace)}", PqColors.Paper)
                }
            }
        }
    }
}

@Composable
private fun PartiesEmpty(username: String, onCreate: () -> Unit, onShare: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PqCard(bg = PqColors.Gold, shadow = PqDimens.ShadowLg) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(Icons.Rounded.Groups, PqColors.Paper, size = 56.dp, iconSize = 30.dp, radius = 14.dp)
                Text("NO PARTIES YET", style = PqType.display(34))
                Text("Team up with 2 to 30 friends. Count Darood toward a shared goal, or share study deadlines and earn party points.", style = PqType.Body)
                PqButton("Create a party", onCreate, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add, height = 50.dp)
                PqButton("Share @$username", onShare, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, icon = Icons.Rounded.Share, height = 46.dp)
            }
        }
        PqGroupLabel("Two kinds of party")
        PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
            Column {
                KindRow(PartyType.Darood, "Post your daily count and reach a milestone together.")
                Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
                KindRow(PartyType.Study, "Share assignments and quizzes, log study time, top the weekly board.")
            }
        }
    }
}

@Composable
private fun KindRow(type: PartyType, body: String) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryTile(type.icon, type.tint, size = 40.dp, iconSize = 22.dp)
        Column { Text(type.label, style = PqType.BodyStrong); Text(body, style = PqType.Meta) }
    }
}

// =====================  NEW PARTY  =====================

data class NewPartyUiState(
    val type: PartyType = PartyType.Darood,
    val name: String = "",
    val size: Int = 10,
    val milestone: String = "",
    val submitting: Boolean = false,
) { val canCreate get() = name.isNotBlank() && !submitting }

@Composable
fun NewPartyScreen(
    state: NewPartyUiState,
    onClose: () -> Unit,
    onType: (PartyType) -> Unit,
    onName: (String) -> Unit,
    onSize: (Int) -> Unit,
    onMilestone: (String) -> Unit,
    onCreate: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        Modifier.fillMaxSize().background(PqColors.Ground).imePadding()
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PqTopBar("New party", onBack = onClose)
        PqFieldLabel("Type")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PartyType.entries.forEach { t ->
                val sel = t == state.type
                Box(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).comicPressable({ onType(t) }, bg = if (sel) t.tint else PqColors.Paper, radius = PqDimens.Radius, shadow = if (sel) PqDimens.Shadow else 0.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) { Icon(t.icon, null, tint = PqColors.Ink); Text(t.label, style = PqType.BodyStrong) }
                    if (sel) Box(Modifier.align(Alignment.TopEnd).padding(0.dp).size(24.dp).clip(PqCircle).background(PqColors.Ink), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, tint = PqColors.Paper, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        PqFieldLabel("Party name", trailing = "${state.name.length} / 40")
        PqTextField(state.name, onName, "e.g. Friday circle", maxLength = 40)
        PqFieldLabel("Party size")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PqStepper(state.size, onSize, 2..30)
            Text("2–30, you included. Pending invites count.", style = PqType.Meta, modifier = Modifier.weight(1f))
        }
        if (state.type == PartyType.Darood) {
            PqFieldLabel("Milestone · optional")
            PqTextField(state.milestone, { v -> onMilestone(v.filter(Char::isDigit)) }, "e.g. 100,000", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10_000, 50_000, 100_000).forEach { m -> PqChoiceChip(m.fmt(), state.milestone == m.toString(), { onMilestone(m.toString()) }) }
            }
            Text("The total the party aims for. You can set it later.", style = PqType.Meta)
        }
        Box(Modifier.weight(1f))
        PqButton("Create party", onCreate, Modifier.fillMaxWidth(), enabled = state.canCreate, height = 52.dp)
    }
}

// =====================  DAROOD ROOM  =====================

sealed interface RoomEvent {
    val key: String
    data class Day(override val key: String, val label: String) : RoomEvent
    data class System(override val key: String, val text: String) : RoomEvent
    data class Count(override val key: String, val amount: Int, val time: String, val author: String, val mine: Boolean, val sent: Boolean = true) : RoomEvent
    data class Milestone(override val key: String, val text: String) : RoomEvent
}

data class DaroodRoomUiState(
    val name: String,
    val members: Int,
    val total: Int,
    val milestone: Int?,
    val youToday: Int,
    val partyToday: Int,
    val events: List<RoomEvent>,      // newest LAST
    val input: String,
    val quickAdds: List<Int> = listOf(11, 33, 100, 313),
)

/** Hide the bottom bar on this route (PqRootScaffold(showBottomBar = false)). */
@Composable
fun DaroodRoomScreen(
    state: DaroodRoomUiState,
    onBack: () -> Unit,
    onStats: () -> Unit,
    onInfo: () -> Unit,
    onQuickAdd: (Int) -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(PqColors.Ground).imePadding()) {
        Column(
            Modifier.fillMaxWidth().padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PqIconButton(Icons.Rounded.ArrowBack, "Back", onBack)
                InitialAvatar(state.name)
                Column(Modifier.weight(1f)) {
                    Text(state.name.uppercase(), style = PqType.CardTitle)
                    Text("Darood · ${state.members} member${if (state.members == 1) "" else "s"}", style = PqType.Meta)
                }
                PqIconButton(Icons.Rounded.BarChart, "My stats", onStats)
                PqIconButton(Icons.Rounded.Info, "Party info", onInfo)
            }
            if (state.milestone != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${state.total.fmt()} ", style = PqType.BodyStrong)
                    Text("of ${state.milestone.fmt()}", style = PqType.Meta, modifier = Modifier.weight(1f))
                    Text("${(state.total * 100 / state.milestone)}%", style = PqType.MetaStrong.copy(color = PqColors.RedText))
                }
                PqProgressBar(state.total / state.milestone.toFloat())
            }
            Row {
                Text("You today ${state.youToday.fmt()}", style = PqType.MetaStrong, modifier = Modifier.weight(1f))
                Text("Party today ${state.partyToday.fmt()}", style = PqType.MetaStrong)
            }
        }
        Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = PqDimens.ScreenPad, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.events.asReversed(), key = { it.key }) { e ->
                when (e) {
                    is RoomEvent.Day -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(e.label, style = PqType.MetaStrong, modifier = Modifier.clip(PqCircle).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, PqCircle).padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                    is RoomEvent.System -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(e.text, style = PqType.Meta.copy(color = PqColors.Ink2)) }
                    is RoomEvent.Milestone -> Row(
                        Modifier.fillMaxWidth().comic(bg = PqColors.Gold, radius = PqDimens.Radius, shadow = 0.dp).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) { Icon(Icons.Rounded.Flag, null, tint = PqColors.Ink); Text(e.text, style = PqType.BodyStrong) }
                    is RoomEvent.Count -> CountBubble(e)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().background(PqColors.Paper).padding(top = 0.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
            Column(Modifier.navigationBarsPadding().padding(horizontal = PqDimens.ScreenPad, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.quickAdds.forEach { n ->
                        Box(Modifier.weight(1f).height(36.dp).comicPressable({ onQuickAdd(n) }, radius = 18.dp, shadow = 2.dp), contentAlignment = Alignment.Center) {
                            Text("+$n", style = PqType.Button)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PqTextField(
                        state.input, { onInputChange(it.filter(Char::isDigit).take(6)) }, "Add your count", Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), bg = PqColors.Ground,
                    )
                    Box(Modifier.size(48.dp).comicPressable(onSend, bg = PqColors.Red, radius = 12.dp, shadow = 2.dp, enabled = state.input.isNotEmpty()), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Send, "Send", tint = PqColors.OnRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun CountBubble(e: RoomEvent.Count) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (e.mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(horizontalAlignment = if (e.mine) Alignment.End else Alignment.Start) {
            if (!e.mine) Text(e.author, style = PqType.Meta, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            Row(
                Modifier.comic(bg = if (e.mine) PqColors.Red else PqColors.Paper, radius = 16.dp, shadow = 2.dp, tappable = true).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val fg = if (e.mine) PqColors.OnRed else PqColors.Ink
                Text("+${e.amount.fmt()}", style = PqType.display(28).copy(color = fg))
                Text(e.time + if (e.mine && e.sent) " ✓" else "", style = PqType.Meta.copy(color = fg))
            }
        }
    }
}

// =====================  MY STATS SHEET  =====================

data class MemberShareUi(val handle: String, val total: Int, val you: Boolean)

data class DaroodStatsUi(
    val myTotal: Int, val sharePercent: Int, val place: Int, val of: Int, val usualDay: Int,
    val activeDays30: Int, val streak: Int, val bestRun: Int,
    val last30: List<Boolean>,              // oldest → today
    val everyone: List<MemberShareUi>,      // sorted desc
)

@Composable
fun DaroodStatsSheet(stats: DaroodStatsUi, onDismiss: () -> Unit) {
    PqSheet(onDismiss) {
        Text("MY STATS", style = PqType.Title.copy(color = PqColors.Title))
        Box(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(pqShape(PqDimens.Radius)).border(PqDimens.Border, PqColors.Ink, pqShape(PqDimens.Radius))) {
            Row { StatCell2(stats.myTotal.fmt(), "My total", Modifier.weight(1f), right = true, bottom = true); StatCell2("${stats.sharePercent}%", "Share of party", Modifier.weight(1f), bottom = true) }
            Row { StatCell2("${stats.place} of ${stats.of}", "My place", Modifier.weight(1f), right = true); StatCell2(stats.usualDay.fmt(), "On a day I add", Modifier.weight(1f)) }
        }
        PqFieldLabel("How steady · last 30 days", Modifier.padding(top = 10.dp))
        Box(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SteadyTile("${stats.activeDays30}/30", "Active days", PqColors.Sky, Modifier.weight(1f))
            SteadyTile("${stats.streak}", "In a row", PqColors.Gold, Modifier.weight(1f))
            SteadyTile("${stats.bestRun}", "Best run", PqColors.Paper, Modifier.weight(1f))
        }
        Box(Modifier.height(10.dp))
        stats.last30.chunked(15).forEach { row ->
            Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { _, on ->
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(pqShape(3.dp)).background(if (on) PqColors.Red else PqColors.Sunken)
                        .then(if (on) Modifier.border(2.dp, PqColors.Ink, pqShape(3.dp)) else Modifier))
                }
            }
        }
        PqFieldLabel("Everyone's part", Modifier.padding(top = 8.dp))
        Box(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.everyone.forEachIndexed { i, m ->
                Row(
                    Modifier.fillMaxWidth().comic(bg = if (m.you) PqColors.Gold else PqColors.Paper, radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("${i + 1}", style = PqType.Number)
                    InitialAvatar(m.handle, 30.dp)
                    Text(m.handle + if (m.you) " (you)" else "", style = PqType.BodyStrong, modifier = Modifier.weight(1f))
                    Text(m.total.fmt(), style = PqType.Number)
                }
            }
        }
    }
}

@Composable
private fun StatCell2(v: String, l: String, modifier: Modifier, right: Boolean = false, bottom: Boolean = false) {
    Box(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) { Text(v, style = PqType.Number); Text(l, style = PqType.Meta) }
        if (right) Box(Modifier.align(Alignment.CenterEnd).size(PqDimens.BorderThin, 64.dp).background(PqColors.Ink))
        if (bottom) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
    }
}

@Composable
private fun SteadyTile(v: String, l: String, bg: Color, modifier: Modifier) {
    Column(modifier.comic(bg = bg, radius = 10.dp, border = PqDimens.BorderThin, shadow = 0.dp).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(v, style = PqType.Number); Text(l, style = PqType.Meta.copy(color = PqColors.Ink))
    }
}

// =====================  PARTY INFO  =====================

data class MemberUi(val handle: String, val joined: String, val leader: Boolean, val you: Boolean)

data class PartyInfoUiState(
    val name: String,
    val type: PartyType,
    val capacity: Int,
    val milestone: Int?,
    val raceBoard: Boolean,
    val members: List<MemberUi>,
    val youAreLeader: Boolean,
)

@Composable
fun PartyInfoScreen(
    state: PartyInfoUiState,
    onBack: () -> Unit,
    onChangeMilestone: () -> Unit,
    onAddPeople: () -> Unit,
    onRename: () -> Unit,
    onRaceBoard: (Boolean) -> Unit,
    onMemberClick: (MemberUi) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        Modifier.fillMaxSize().background(PqColors.Ground),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PqTopBar("Party info", onBack = onBack) }
        item {
            PqCard(shadow = PqDimens.ShadowLg) {
                Column {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InitialAvatar(state.name, 52.dp)
                        Column { Text(state.name.uppercase(), style = PqType.Title); Text("${state.type.label} · ${state.members.size} of ${state.capacity} members", style = PqType.Meta) }
                    }
                    if (state.type == PartyType.Darood) {
                        Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Flag, null, tint = PqColors.Ink)
                            Text(state.milestone?.let { "Milestone ${it.fmt()}" } ?: "No milestone yet", style = PqType.BodyStrong, modifier = Modifier.weight(1f))
                            if (state.youAreLeader) PqQuietButton(if (state.milestone == null) "Set" else "Change", onChangeMilestone)
                        }
                    }
                }
            }
        }
        if (state.youAreLeader) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PqButton("Add people", onAddPeople, Modifier.weight(1.3f), icon = Icons.Rounded.PersonAdd)
                PqButton("Rename", onRename, Modifier.weight(1f), style = PqButtonStyle.Secondary, icon = Icons.Rounded.Edit)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Join the race board", style = PqType.BodyStrong)
                    Text("Others see the party name, size, total and progress. Never names or personal counts.", style = PqType.Meta.copy(color = PqColors.Ink2))
                }
                PqSwitch(state.raceBoard, onRaceBoard)
            }
        }
        item { PqGroupLabel("Members · ${state.members.size}") }
        item {
            PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
                Column {
                    state.members.forEachIndexed { i, m ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            InitialAvatar(m.handle)
                            Column(Modifier.weight(1f)) { Text(m.handle + if (m.you) " (you)" else "", style = PqType.BodyStrong); Text("Joined ${m.joined}", style = PqType.Meta) }
                            if (m.leader) PqChip("LEADER", PqColors.Gold)
                        }
                        if (i != state.members.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
                    }
                }
            }
        }
        item {
            Text(
                if (state.youAreLeader && state.members.size > 1) "To leave, hand leadership to another member first."
                else if (state.youAreLeader) "You're the only member. Delete the party to close it." else "",
                style = PqType.Meta,
            )
        }
        item {
            // Destructive = outline in primary text colour, never a filled primary
            val shape = pqShape(12.dp)
            Row(
                Modifier.height(42.dp).clip(shape).border(PqDimens.Border, PqColors.RedText, shape)
                    .clickable(onClick = if (state.youAreLeader) onDelete else onLeave).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Delete, null, tint = PqColors.RedText, modifier = Modifier.size(20.dp))
                Text(if (state.youAreLeader) "DELETE PARTY" else "LEAVE PARTY", style = PqType.Button.copy(color = PqColors.RedText))
            }
        }
    }
}

// ---------- Previews ----------

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun PartiesPreview() = PixelQuestTheme {
    PartiesScreen(
        PartiesUiState(null, listOf(
            PartyUi("1", "Raza", PartyType.Darood, 1, total = 726, milestone = 10_000, youToday = 313),
            PartyUi("2", "Bros", PartyType.Study, 3, dueThisWeek = 2, yourPoints = 33, yourPlace = 2),
        ), false, "raza"),
        {}, {}, {}, {}, {}, {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun RoomPreview() = PixelQuestTheme {
    DaroodRoomScreen(
        DaroodRoomUiState("Raza", 1, 726, 10_000, 726, 726, listOf(
            RoomEvent.Day("d", "Today"), RoomEvent.System("s", "@raza joined · milestone set to 10,000"),
            RoomEvent.Count("a", 313, "8:50 AM", "@raza", true), RoomEvent.Count("b", 313, "8:50 AM", "@raza", true), RoomEvent.Count("c", 100, "8:51 AM", "@raza", true),
        ), ""),
        {}, {}, {}, {}, {}, {},
    )
}
