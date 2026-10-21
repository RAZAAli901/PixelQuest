package com.pixelquest.app.darood.ui.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.party.ui.components.InviteSearch
import com.pixelquest.app.party.ui.components.LookupResult
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqTopBar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp)
            .testTag(DaroodTestTags.ADD_SCREEN),
        verticalArrangement = Arrangement.spacedBy(PqDimens.Gap)
    ) {
        PqTopBar(stringResource(R.string.darood_add_title), onBack = onNavigateBack)
        Text(text = stringResource(R.string.darood_add_intro), style = PqType.Body.copy(color = PqColors.Ink2))
        InviteSearch(query = state.query, onQueryChange = onQueryChange)
        if (state.isFull) {
            Text(
                text = stringResource(R.string.darood_add_is_full, state.placesTaken, state.limit ?: 0),
                style = PqType.BodyStrong.copy(color = PqColors.Error),
                modifier = Modifier.testTag(DaroodTestTags.ADD_FULL)
            )
        }
        LookupResult(state.lookup, enabled = !state.sending && !state.isFull, onInvite = onInvite)
        state.result?.let { InviteResultLine(it) }
        if (state.people.isNotEmpty()) {
            PqGroupLabel(stringResource(R.string.darood_add_people_title), Modifier.semantics { heading() })
            PqCard(Modifier.fillMaxWidth(), radius = PqDimens.Radius, shadow = 0.dp) {
                Column {
                    state.people.forEachIndexed { i, person ->
                        PersonRow(person)
                        if (i != state.people.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
                    }
                }
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
                com.pixelquest.app.darood.domain.DaroodRefusal.TOO_MANY_INVITES -> R.string.darood_add_too_many
                else -> R.string.darood_add_failed
            }
        )
    }
    Text(
        text = text,
        style = PqType.BodyStrong.copy(color = PqColors.RedText),
        modifier = Modifier.testTag(DaroodTestTags.ADD_RESULT_LINE).semantics { liveRegion = LiveRegionMode.Polite }
    )
}

/** Someone the party has asked or had, with where they stand: Joined, Invited, Declined, Left or Removed. */
@Composable
private fun PersonRow(person: com.pixelquest.app.party.domain.PartyMember) {
    val name = person.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.person(person.userId)).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        InitialAvatar(person.username ?: "?")
        Text(text = name, style = PqType.BodyStrong, modifier = Modifier.weight(1f))
        Text(
            text = stringResource(
                when {
                    person.role == com.pixelquest.app.party.domain.PartyRole.LEADER -> R.string.darood_badge_leader
                    else -> when (person.status) {
                        com.pixelquest.app.party.domain.MemberStatus.ACTIVE -> R.string.darood_badge_joined
                        com.pixelquest.app.party.domain.MemberStatus.INVITED -> R.string.darood_badge_invited
                        com.pixelquest.app.party.domain.MemberStatus.DECLINED -> R.string.darood_badge_declined
                        com.pixelquest.app.party.domain.MemberStatus.LEFT -> R.string.darood_badge_left
                        com.pixelquest.app.party.domain.MemberStatus.REMOVED -> R.string.darood_badge_removed
                    }
                }
            ),
            style = PqType.MetaStrong.copy(color = PqColors.Ink2)
        )
    }
}
