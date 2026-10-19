package com.pixelquest.app.darood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * Darood Party's home, from the side menu: the account's parties. Parties need an account: signed
 * out, it offers the app's usual sign-in ([onSignIn] opens the Account screen).
 */
@Composable
fun DaroodPartyScreen(
    onNavigateBack: (() -> Unit)?,
    onSignIn: () -> Unit,
    onOpenParty: (String) -> Unit,
    onCreateParty: () -> Unit = {},
    onOpenRaceBoard: () -> Unit = {},
    viewModel: com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val weekly by viewModel.weeklyReminder.collectAsState()
    DaroodPartyContent(
        weeklyReminder = weekly,
        onWeeklyReminder = viewModel::setWeeklyReminder,
        state = state,
        onSignIn = onSignIn,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onOpenParty = onOpenParty,
        onCreateParty = onCreateParty,
        onOpenRaceBoard = onOpenRaceBoard,
        onAccept = viewModel::accept,
        onDecline = viewModel::decline
    )
}

/** The screen for [state]: nothing while the session loads, the sign-in card, or the parties. */
@Composable
fun DaroodPartyContent(
    state: com.pixelquest.app.darood.ui.list.DaroodPartiesUiState,
    onSignIn: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    onRefresh: () -> Unit = {},
    onOpenParty: (String) -> Unit = {},
    onCreateParty: () -> Unit = {},
    onOpenRaceBoard: () -> Unit = {},
    weeklyReminder: Boolean? = null,
    onWeeklyReminder: (Boolean) -> Unit = {},
    onAccept: (String) -> Unit = {},
    onDecline: (String) -> Unit = {}
) {
    val signedIn = state.signedIn
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_title),
                actions = if (signedIn == true) {
                    {
                        IconButton(onClick = onOpenRaceBoard, modifier = Modifier.testTag(DaroodTestTags.RACE_ACTION)) {
                            SymbolIcon(
                                symbol = "🏁",
                                contentDescription = stringResource(R.string.darood_race_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primaryText
                            )
                        }
                        IconButton(onClick = onCreateParty, modifier = Modifier.testTag(DaroodTestTags.CREATE_ACTION)) {
                            SymbolIcon(
                                symbol = "＋",
                                contentDescription = stringResource(R.string.darood_create),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primaryText
                            )
                        }
                    }
                } else {
                    null
                },
                navigationIcon = onNavigateBack?.let { back ->
                    {
                        IconButton(onClick = back) {
                            SymbolIcon(
                                symbol = "◀",
                                contentDescription = stringResource(R.string.navigate_back),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primaryText
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (signedIn == true) {
            PartyRefreshBox(
                refreshing = state.loading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(innerPadding).testTag(DaroodTestTags.PARTY_SCREEN)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag(DaroodTestTags.PARTY_LIST),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.problem?.let { problem ->
                        item(key = "problem") {
                            Text(
                                text = stringResource(
                                    when (problem) {
                                        com.pixelquest.app.darood.ui.list.PartiesProblem.OFFLINE -> R.string.darood_list_offline
                                        com.pixelquest.app.darood.ui.list.PartiesProblem.INVITE_GONE -> R.string.darood_invite_gone
                                        com.pixelquest.app.darood.ui.list.PartiesProblem.FAILED -> R.string.darood_list_failed
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    if (state.invites.isNotEmpty()) {
                        item(key = "invites_title") { SectionTitle(stringResource(R.string.darood_invites_title)) }
                        items(state.invites, key = { "invite_" + it.id }) { party ->
                            com.pixelquest.app.darood.ui.list.InviteRow(
                                party = party,
                                enabled = state.answering == null,
                                onAccept = { onAccept(party.id) },
                                onDecline = { onDecline(party.id) }
                            )
                        }
                        if (state.parties.isNotEmpty()) {
                            item(key = "parties_title") { SectionTitle(stringResource(R.string.darood_parties_title)) }
                        }
                    }
                    items(state.parties, key = { it.id }) { party ->
                        com.pixelquest.app.darood.ui.list.PartyRow(party = party, onClick = { onOpenParty(party.id) })
                    }
                    // Once there's a party to add to: the weekly note, off unless turned on.
                    if (state.parties.isNotEmpty() && weeklyReminder != null) {
                        item(key = "weekly") { WeeklyReminderSwitch(on = weeklyReminder, onChange = onWeeklyReminder) }
                    }
                    if (state.isEmpty) {
                        item(key = "empty") {
                            DaroodCard(
                                title = stringResource(R.string.darood_empty_title),
                                body = stringResource(R.string.darood_empty_body),
                                modifier = Modifier.testTag(DaroodTestTags.EMPTY)
                            ) {
                                PixelButton(
                                    text = stringResource(R.string.darood_create),
                                    onClick = onCreateParty,
                                    modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.CREATE)
                                )
                            }
                        }
                    }
                }
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag(DaroodTestTags.PARTY_SCREEN),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (signedIn) {
                null, true -> Unit
                false -> DaroodCard(
                    title = stringResource(R.string.darood_signed_out_title),
                    body = stringResource(R.string.darood_signed_out_body),
                    modifier = Modifier.testTag(DaroodTestTags.SIGNED_OUT)
                ) {
                    PixelButton(
                        text = stringResource(R.string.darood_sign_in),
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.SIGN_IN)
                    )
                }
            }
        }
    }
}

/** The 📿 card: a title, a line or two, and an optional action under them. */
@Composable
private fun DaroodCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val colors = PixelTheme.colors
    PixelCard(variant = PixelPanelVariant.BEIGE, contentPadding = 20.dp, modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "📿", style = MaterialTheme.typography.displaySmall)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = inkOnPanel(colors.primaryText),
                textAlign = TextAlign.Center
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            action?.invoke()
        }
    }
}

/** A heading over a part of the list. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = PixelTheme.colors.primaryText,
        modifier = Modifier.semantics { heading() }
    )
}

/**
 * The weekly note's switch, at the foot of the parties: what it is, said plainly, and off unless the
 * player turns it on. The whole row is the switch, read by TalkBack as one.
 */
@Composable
internal fun WeeklyReminderSwitch(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    com.pixelquest.app.ui.components.PixelCard(
        variant = com.pixelquest.app.ui.components.PixelPanelVariant.BORDER,
        contentPadding = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = on, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onChange)
                .testTag(DaroodTestTags.WEEKLY_SWITCH)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🌙  " + stringResource(R.string.darood_weekly_switch),
                    style = MaterialTheme.typography.titleSmall,
                    color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primaryText)
                )
                Text(
                    text = stringResource(R.string.darood_weekly_explained),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            androidx.compose.material3.Switch(
                checked = on,
                onCheckedChange = null,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = colors.onSurfaceVariant,
                    uncheckedTrackColor = colors.surfaceVariant,
                    uncheckedBorderColor = colors.pixelBorder
                )
            )
        }
    }
}
