package com.pixelquest.app.party.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.pixelquest.app.darood.ui.PartyRefreshBox
import com.pixelquest.app.darood.ui.list.DaroodPartiesUiState
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.darood.ui.list.InviteRow
import com.pixelquest.app.darood.ui.list.PartiesProblem
import com.pixelquest.app.darood.ui.list.PartyRow
import com.pixelquest.app.party.domain.PartyKind
import com.pixelquest.app.study.ui.StudyPartyKind
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.menu.LocalMenuDrawer
import com.pixelquest.app.ui.menu.MenuTopBarAction
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import com.pixelquest.app.party.domain.Party
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/** The Parties screen, from the side menu, with its state from the list's view model (its route says: study parties). */
@Composable
fun PartiesScreen(
    onSignIn: () -> Unit,
    onOpenParty: (String) -> Unit,
    onCreateParty: () -> Unit,
    viewModel: DaroodPartiesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val invites by viewModel.allInvites.collectAsState()
    // An invitation sent meanwhile arrives at once, while the screen is up.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.followInvitations() }
    }
    PartiesContent(
        state = state,
        invites = invites,
        kind = viewModel.kind,
        onSignIn = onSignIn,
        onRefresh = viewModel::refresh,
        onOpenParty = onOpenParty,
        onCreateParty = onCreateParty,
        onAccept = viewModel::accept,
        onDecline = viewModel::decline
    )
}

/**
 * Parties, the main app's (docs/party-core.md): a button to start a study party, the invitations
 * waiting for an answer, and the study parties the player is in. Signed out, why they need an
 * account. A top-level screen: the side menu's three lines at its top, not a way back.
 *
 * @param invites every invitation waiting, of any kind: one inbox, each saying its kind.
 */
@Composable
fun PartiesContent(
    state: DaroodPartiesUiState,
    onSignIn: () -> Unit,
    onOpenParty: (String) -> Unit,
    onCreateParty: () -> Unit,
    kind: PartyKind = StudyPartyKind,
    invites: List<Party> = state.invites,
    onRefresh: () -> Unit = {},
    onAccept: (String) -> Unit = {},
    onDecline: (String) -> Unit = {}
) {
    val colors = PixelTheme.colors
    val hasMenu = LocalMenuDrawer.current != null
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.parties_title),
                navigationIcon = if (hasMenu) { { MenuTopBarAction() } } else null
            )
        },
        modifier = Modifier.testTag(PartiesTags.SCREEN)
    ) { innerPadding ->
        when (state.signedIn) {
            null -> Unit
            false -> Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                LandingCard(
                    icon = kind.icon,
                    title = stringResource(R.string.parties_signed_out_title),
                    body = stringResource(kind.signedOutBodyRes),
                    modifier = Modifier.testTag(PartiesTags.SIGNED_OUT)
                ) {
                    PixelButton(text = stringResource(R.string.darood_sign_in), onClick = onSignIn, modifier = Modifier.fillMaxWidth().testTag(PartiesTags.SIGN_IN))
                }
            }
            true -> PartyRefreshBox(refreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag(PartiesTags.LIST)
                ) {
                    item(key = "create") {
                        PixelButton(
                            text = stringResource(R.string.parties_create),
                            onClick = onCreateParty,
                            modifier = Modifier.fillMaxWidth().testTag(PartiesTags.CREATE)
                        )
                    }
                    state.problem?.let { problem ->
                        item(key = "problem") {
                            Text(
                                text = stringResource(
                                    when (problem) {
                                        PartiesProblem.OFFLINE -> R.string.darood_list_offline
                                        PartiesProblem.INVITE_GONE -> R.string.darood_invite_gone
                                        PartiesProblem.FAILED -> R.string.darood_list_failed
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    if (invites.isNotEmpty()) {
                        item(key = "invites_title") { SectionTitle(stringResource(R.string.darood_invites_title)) }
                        items(invites, key = { "invite_" + it.id }) { party ->
                            InviteRow(
                                party = party,
                                enabled = state.answering == null,
                                onAccept = { onAccept(party.id) },
                                onDecline = { onDecline(party.id) }
                            )
                        }
                    }
                    if (state.parties.isNotEmpty()) {
                        item(key = "parties_title") { SectionTitle(stringResource(kind.listTitleRes)) }
                        items(state.parties, key = { it.id }) { party -> PartyRow(party = party, onClick = { onOpenParty(party.id) }) }
                    }
                    if (state.parties.isEmpty() && invites.isEmpty()) {
                        item(key = "empty") {
                            LandingCard(
                                icon = kind.icon,
                                title = stringResource(R.string.parties_empty_title),
                                body = stringResource(kind.emptyBodyRes),
                                modifier = Modifier.testTag(PartiesTags.EMPTY)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A card with the kind's icon, a title, a line or two, and an optional action under them. */
@Composable
private fun LandingCard(icon: String, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val colors = PixelTheme.colors
    PixelCard(variant = PixelPanelVariant.BEIGE, contentPadding = 20.dp, modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(text = icon, style = MaterialTheme.typography.displaySmall)
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = inkOnPanel(colors.primaryText), textAlign = TextAlign.Center)
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, textAlign = TextAlign.Center)
            action?.invoke()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = PixelTheme.colors.primaryText, modifier = Modifier.semantics { heading() })
}

/** The Parties screen's test tags. */
object PartiesTags {
    const val SCREEN = "parties_screen"
    const val LIST = "parties_list"
    const val CREATE = "parties_create"
    const val EMPTY = "parties_empty"
    const val SIGNED_OUT = "parties_signed_out"
    const val SIGN_IN = "parties_sign_in"
}
