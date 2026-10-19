package com.pixelquest.app.darood.ui.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/** Adding people to a party by their exact username. */
@Composable
fun AddPeopleScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddPeopleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    AddPeopleContent(state = state, onQueryChange = viewModel::onQueryChange, onNavigateBack = onNavigateBack, onInvite = viewModel::invite)
}

@Composable
fun AddPeopleContent(
    state: AddPeopleUiState,
    onQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onInvite: (String) -> Unit = {}
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_add_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            symbol = "◀",
                            contentDescription = stringResource(R.string.navigate_back),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryText
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag(DaroodTestTags.ADD_SCREEN),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(R.string.darood_add_intro), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            PixelTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = stringResource(R.string.darood_add_search),
                placeholder = stringResource(R.string.darood_username_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.ADD_SEARCH)
            )
            LookupResult(state.lookup, enabled = !state.sending, onInvite = onInvite)
            state.result?.let { InviteResultLine(it) }
            if (state.people.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.darood_add_people_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primaryText,
                    modifier = Modifier.semantics { heading() }
                )
                state.people.forEach { PersonRow(it) }
            }
        }
    }
}

/** What the search found: a row to invite from, or a line saying why not. */
@Composable
private fun LookupResult(lookup: Lookup, enabled: Boolean, onInvite: (String) -> Unit) {
    val colors = PixelTheme.colors
    val line = when (lookup) {
        Lookup.Idle -> null
        Lookup.Checking -> stringResource(R.string.darood_username_checking)
        is Lookup.NotFound -> stringResource(R.string.darood_add_not_found, lookup.username.value)
        Lookup.Yours -> stringResource(R.string.darood_add_yours)
        is Lookup.Invalid -> stringResource(
            when (lookup.problem) {
                UsernameProblem.TOO_SHORT -> R.string.darood_username_too_short
                UsernameProblem.TOO_LONG -> R.string.darood_username_too_long
                UsernameProblem.RESERVED -> R.string.darood_add_not_found_reserved
                UsernameProblem.EMPTY, UsernameProblem.INVALID_CHARACTERS -> R.string.darood_username_invalid_characters
            }
        )
        Lookup.Unknown -> stringResource(R.string.darood_username_unknown)
        is Lookup.Found -> null
    }
    if (line != null) {
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.testTag(DaroodTestTags.ADD_STATUS).semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
    if (lookup is Lookup.Found) {
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.ADD_RESULT)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.darood_username_handle, lookup.username.value),
                    style = MaterialTheme.typography.titleMedium,
                    color = inkOnPanel(colors.primaryText),
                    modifier = Modifier.weight(1f)
                )
                PixelButton(
                    text = stringResource(R.string.darood_add_invite),
                    onClick = { onInvite(lookup.username.value) },
                    enabled = enabled,
                    modifier = Modifier.testTag(DaroodTestTags.ADD_INVITE)
                )
            }
        }
    }
}

/** What became of the invitation just sent. */
@Composable
private fun InviteResultLine(result: InviteResult) {
    val text = when (result) {
        is InviteResult.Sent -> stringResource(R.string.darood_add_sent, result.username)
        is InviteResult.AlreadyIn -> stringResource(R.string.darood_add_already_in, result.username)
        InviteResult.Offline -> stringResource(R.string.darood_add_offline)
        is InviteResult.Refused -> stringResource(
            when (result.refusal) {
                com.pixelquest.app.darood.domain.DaroodRefusal.PARTY_FULL -> R.string.darood_add_full
                com.pixelquest.app.darood.domain.DaroodRefusal.NOT_LEADER -> R.string.darood_add_not_leader
                com.pixelquest.app.darood.domain.DaroodRefusal.PARTY_CLOSED -> R.string.darood_add_closed
                else -> R.string.darood_add_failed
            }
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = PixelTheme.colors.primaryText,
        modifier = Modifier.testTag(DaroodTestTags.ADD_RESULT_LINE).semantics { liveRegion = LiveRegionMode.Polite }
    )
}

/** Someone the party has asked or had, with where they stand: Joined, Invited, Declined, Left or Removed. */
@Composable
private fun PersonRow(person: com.pixelquest.app.darood.domain.PartyMember) {
    val colors = PixelTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.person(person.userId))
    ) {
        Text(
            text = person.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(
                when {
                    person.role == com.pixelquest.app.darood.domain.PartyRole.LEADER -> R.string.darood_badge_leader
                    else -> when (person.status) {
                        com.pixelquest.app.darood.domain.MemberStatus.ACTIVE -> R.string.darood_badge_joined
                        com.pixelquest.app.darood.domain.MemberStatus.INVITED -> R.string.darood_badge_invited
                        com.pixelquest.app.darood.domain.MemberStatus.DECLINED -> R.string.darood_badge_declined
                        com.pixelquest.app.darood.domain.MemberStatus.LEFT -> R.string.darood_badge_left
                        com.pixelquest.app.darood.domain.MemberStatus.REMOVED -> R.string.darood_badge_removed
                    }
                }
            ),
            style = MaterialTheme.typography.labelMedium,
            color = colors.primaryText
        )
    }
}

