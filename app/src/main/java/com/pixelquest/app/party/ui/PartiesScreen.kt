package com.pixelquest.app.party.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.PartyRefreshBox
import com.pixelquest.app.darood.ui.list.DaroodPartiesUiState
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.darood.ui.list.InviteRow
import com.pixelquest.app.darood.ui.list.PartiesProblem
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.PartyKinds
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqProgressBar
import com.pixelquest.app.ui.components.PqSegmented
import com.pixelquest.app.ui.components.PqSwitch
import com.pixelquest.app.ui.components.PqTag
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** A kind of party's icon on UI v2's screens: the open hands for Darood, the cap for study. */
val PartyType.icon: ImageVector
    get() = when (this) {
        PartyType.DAROOD -> Icons.Rounded.VolunteerActivism
        PartyType.STUDY -> Icons.Rounded.School
    }

/** A kind of party's colour on UI v2's screens. */
val PartyType.tint: Color
    get() = when (this) {
        PartyType.DAROOD -> PqColors.Amber
        PartyType.STUDY -> PqColors.Lilac
    }

/** A kind of party's short name, for its card and the filter ("Darood", "Study"). */
fun PartyType.shortName(): Int = when (this) {
    PartyType.DAROOD -> R.string.pq_party_darood
    PartyType.STUDY -> R.string.pq_party_study
}

/**
 * The Parties hub (UI v2, mocks 4B and 4C; docs/party-core.md), from Home's groups button, Deen's
 * Darood Party tile and Hero's Parties row: every party the player is in, of both kinds, with the
 * invitations waiting, the weekly note, and a filter by kind. Darood's appear while Darood is
 * available. With none, what parties are, and how to start one or be invited.
 */
@Composable
fun PartiesScreen(
    onSignIn: () -> Unit,
    onOpenParty: (Party) -> Unit,
    onCreateParty: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onOpenRaceBoard: () -> Unit = {},
    onChooseUsername: () -> Unit = {},
    viewModel: DaroodPartiesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val weekly by viewModel.weeklyReminder.collectAsState()
    val username by viewModel.username.collectAsState()
    val darood by viewModel.daroodAvailable.collectAsState()
    // An invitation sent meanwhile arrives at once, while the screen is up.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.followInvitations() }
    }
    val context = LocalContext.current
    val shareText = username?.let { stringResource(R.string.pq_parties_share_text, it) }
    val askForNotifications = com.pixelquest.app.ui.notifications.rememberAskForNotifications()
    PartiesContent(
        state = state,
        onSignIn = onSignIn,
        onOpenParty = onOpenParty,
        onCreateParty = onCreateParty,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        weeklyNote = weekly,
        onWeeklyNote = { on ->
            if (on) askForNotifications()
            viewModel.setWeeklyReminder(on)
        },
        username = username,
        daroodAvailable = darood,
        onOpenRaceBoard = onOpenRaceBoard,
        onShareUsername = {
            if (shareText == null) {
                onChooseUsername()
            } else {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                context.startActivity(Intent.createChooser(send, null))
            }
        }
    )
}

/**
 * The hub for [state]. Signed out, why parties need an account. The filter shows All, Darood or
 * Study; the weekly note is Darood's, once there's a Darood Party to add to.
 */
@Composable
fun PartiesContent(
    state: DaroodPartiesUiState,
    onSignIn: () -> Unit,
    onOpenParty: (Party) -> Unit,
    onCreateParty: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onRefresh: () -> Unit = {},
    onAccept: (String) -> Unit = {},
    onDecline: (String) -> Unit = {},
    weeklyNote: Boolean = false,
    onWeeklyNote: (Boolean) -> Unit = {},
    username: String? = null,
    daroodAvailable: Boolean = true,
    onOpenRaceBoard: () -> Unit = {},
    onShareUsername: () -> Unit = {}
) {
    var filter by rememberSaveable { mutableStateOf<PartyType?>(null) }
    Column(Modifier.fillMaxSize().background(PqColors.Ground).testTag(PartiesTags.SCREEN)) {
        PqTopBar(stringResource(R.string.parties_title), onBack = onNavigateBack, modifier = Modifier.padding(horizontal = PqDimens.ScreenPad)) {
            if (state.signedIn == true && daroodAvailable) {
                PqIconButton(Icons.Rounded.Flag, stringResource(R.string.darood_race_title), onOpenRaceBoard, Modifier.testTag(DaroodTestTags.RACE_ACTION))
            }
            // The empty hub's own button starts one; with parties, the one red button is up here.
            if (state.signedIn == true && state.parties.isNotEmpty()) {
                Box(
                    Modifier.size(40.dp).comicPressable(onCreateParty, bg = PqColors.Red, radius = 12.dp, shadow = 2.dp, role = Role.Button)
                        .testTag(PartiesTags.CREATE),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Add, stringResource(R.string.pq_parties_new), tint = PqColors.OnRed) }
            }
        }
        when (state.signedIn) {
            null -> Unit
            false -> SignedOut(onSignIn, Modifier.padding(PqDimens.ScreenPad))
            true -> PartyRefreshBox(refreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                val visible = state.parties.filter { filter == null || it.type == filter }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag(PartiesTags.LIST),
                    contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 4.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.problem?.let { problem ->
                        item(key = "problem") { Text(stringResource(problem.message()), style = PqType.MetaStrong.copy(color = PqColors.RedText)) }
                    }
                    if (state.invites.isNotEmpty()) {
                        item(key = "invites_title") { SectionTitle(stringResource(R.string.darood_invites_title)) }
                        items(state.invites, key = { "invite_" + it.id }) { party ->
                            InviteRow(party = party, enabled = state.answering == null, onAccept = { onAccept(party.id) }, onDecline = { onDecline(party.id) })
                        }
                    }
                    if (state.parties.isEmpty()) {
                        item(key = "empty") { PartiesEmpty(username, daroodAvailable, onCreateParty, onShareUsername) }
                        return@LazyColumn
                    }
                    if (daroodAvailable && state.parties.map { it.type }.distinct().size > 1 || filter != null) {
                        item(key = "filter") {
                            val options = listOf(null, PartyType.DAROOD, PartyType.STUDY)
                            PqSegmented(
                                listOf(
                                    stringResource(R.string.pq_parties_filter_all, state.parties.size),
                                    stringResource(R.string.pq_party_darood),
                                    stringResource(R.string.pq_party_study)
                                ),
                                options.indexOf(filter), { filter = options[it] }
                            )
                        }
                    }
                    items(visible, key = { it.id }) { party -> PartyCard(party, onClick = { onOpenParty(party) }, unread = state.unread[party.id] ?: 0) }
                    // Once there's a Darood Party to add to: the weekly note, off unless turned on.
                    if (daroodAvailable && state.parties.any { it.type == PartyType.DAROOD }) {
                        item(key = "weekly") { WeeklyNoteRow(weeklyNote, onWeeklyNote) }
                    }
                    item(key = "footer") {
                        Text(
                            username?.let { stringResource(R.string.pq_parties_footer, it) } ?: stringResource(R.string.pq_parties_footer_no_username),
                            style = PqType.Meta
                        )
                    }
                }
            }
        }
    }
}

private fun PartiesProblem.message(): Int = when (this) {
    PartiesProblem.OFFLINE -> R.string.darood_list_offline
    PartiesProblem.INVITE_GONE -> R.string.darood_invite_gone
    PartiesProblem.FAILED -> R.string.darood_list_failed
}

/** One party: its kind's icon and colour, its name, kind and members, and what its kind adds (Darood's total and milestone). */
@Composable
fun PartyCard(party: Party, onClick: () -> Unit, modifier: Modifier = Modifier, unread: Int = 0) {
    val summary = PartyKinds.of(party.type).summary(party)
    PqCard(
        modifier.fillMaxWidth().testTag(DaroodTestTags.partyRow(party.id)),
        onClick = onClick,
        onClickLabel = stringResource(R.string.darood_open_party)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(party.type.icon, party.type.tint)
                Column(Modifier.weight(1f)) {
                    Text(party.name, style = PqType.BodyStrong, maxLines = 1)
                    Text(
                        stringResource(R.string.pq_party_card_line, stringResource(party.type.shortName()), pluralStringResource(R.plurals.party_members, party.memberCount, party.memberCount)),
                        style = PqType.Meta
                    )
                }
                if (party.isFinished) PqTag(stringResource(R.string.darood_finished))
                if (unread > 0) UnreadBadge(unread)
                Icon(Icons.Rounded.ChevronRight, null, tint = PqColors.Ink)
            }
            summary.progress?.let { PqProgressBar(it) }
            summary.detail?.let { Text(phraseText(it), style = PqType.MetaStrong) }
        }
    }
}

/** A party's unread chat messages, in a red pill; read out as "3 unread messages". */
@Composable
private fun UnreadBadge(count: Int) {
    val said = pluralStringResource(R.plurals.study_chat_unread_count, count, count)
    Text(
        text = if (count > 99) "99+" else count.toString(),
        style = PqType.MetaStrong.copy(color = PqColors.OnRed),
        modifier = Modifier
            .background(PqColors.Red, com.pixelquest.app.ui.theme.PqCircle)
            .border(PqDimens.BorderThin, PqColors.Ink, com.pixelquest.app.ui.theme.PqCircle)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = said }
            .testTag(PartiesTags.UNREAD)
    )
}

/** Signed out: parties are shared, so they need an account. */
@Composable
private fun SignedOut(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    PqCard(modifier.fillMaxWidth().testTag(PartiesTags.SIGNED_OUT), bg = PqColors.Gold, shadow = PqDimens.ShadowLg) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryTile(Icons.Rounded.Groups, PqColors.Paper, size = 56.dp, iconSize = 30.dp, radius = 14.dp)
            Text(stringResource(R.string.parties_signed_out_title).uppercase(), style = PqType.display(30))
            Text(stringResource(R.string.party_signed_out_body), style = PqType.Body)
            PqButton(stringResource(R.string.darood_sign_in), onSignIn, Modifier.fillMaxWidth().testTag(PartiesTags.SIGN_IN), height = 50.dp)
        }
    }
}

/** No parties yet: what they are, how to start one, and the username friends invite by. */
@Composable
private fun PartiesEmpty(username: String?, daroodAvailable: Boolean, onCreate: () -> Unit, onShare: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.testTag(PartiesTags.EMPTY)) {
        PqCard(bg = PqColors.Gold, shadow = PqDimens.ShadowLg) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(Icons.Rounded.Groups, PqColors.Paper, size = 56.dp, iconSize = 30.dp, radius = 14.dp)
                Text(stringResource(R.string.pq_parties_empty_title), style = PqType.display(34))
                Text(stringResource(if (daroodAvailable) R.string.pq_parties_empty_body else R.string.pq_parties_empty_body_study), style = PqType.Body)
                PqButton(
                    stringResource(R.string.pq_parties_create), onCreate, Modifier.fillMaxWidth().testTag(DaroodTestTags.CREATE),
                    icon = Icons.Rounded.Add, height = 50.dp
                )
                PqButton(
                    username?.let { stringResource(R.string.pq_parties_share, it) } ?: stringResource(R.string.pq_parties_choose_username), onShare,
                    Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, icon = Icons.Rounded.Share, height = 46.dp
                )
            }
        }
        PqGroupLabel(stringResource(if (daroodAvailable) R.string.pq_parties_kinds else R.string.pq_parties_kind_one))
        PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
            Column {
                if (daroodAvailable) {
                    KindRow(PartyType.DAROOD, R.string.pq_party_darood_about)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
                }
                KindRow(PartyType.STUDY, R.string.pq_party_study_about)
            }
        }
    }
}

@Composable
private fun KindRow(type: PartyType, body: Int) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryTile(type.icon, type.tint, size = 40.dp, iconSize = 22.dp)
        Column {
            Text(stringResource(type.shortName()), style = PqType.BodyStrong)
            Text(stringResource(body), style = PqType.Meta)
        }
    }
}

/**
 * The weekly note's switch, at the foot of the parties: what it is, said plainly, and off unless the
 * player turns it on. The whole row is the switch, read by TalkBack as one.
 */
@Composable
internal fun WeeklyNoteRow(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth()
            .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .testTag(DaroodTestTags.WEEKLY_SWITCH)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Bedtime, null, tint = PqColors.Ink)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.darood_weekly_switch), style = PqType.BodyStrong)
            Text(stringResource(R.string.darood_weekly_explained), style = PqType.Meta.copy(color = PqColors.Ink2))
        }
        PqSwitch(on, onChange)
    }
}

/** A heading over a part of the list. */
@Composable
private fun SectionTitle(text: String) {
    PqGroupLabel(text, Modifier.semantics { heading() })
}

/**
 * The hub's tags. They're Darood's own: one screen lists both kinds, so tests written for either
 * find it the same way.
 */
object PartiesTags {
    const val SCREEN = DaroodTestTags.PARTY_SCREEN
    const val LIST = DaroodTestTags.PARTY_LIST
    const val CREATE = DaroodTestTags.CREATE_ACTION
    const val EMPTY = DaroodTestTags.EMPTY
    const val SIGNED_OUT = DaroodTestTags.SIGNED_OUT
    const val SIGN_IN = DaroodTestTags.SIGN_IN
    const val UNREAD = "parties_unread"
}
