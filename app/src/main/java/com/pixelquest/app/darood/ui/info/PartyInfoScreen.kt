package com.pixelquest.app.darood.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyRole
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * @param members the party's active members: the leader first, then in the order they joined.
 * @param myId the signed-in account, to mark it and to know whether it leads.
 */
data class PartyInfoUiState(
    val party: Party? = null,
    val members: List<PartyMember> = emptyList(),
    val myId: String? = null
) {
    val isLeader: Boolean get() = party?.leaderId != null && party.leaderId == myId
}

/** A party's info: who's in it, and since when (docs/darood-design.md, Parties). */
@HiltViewModel
class PartyInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DaroodPartyRepository,
    account: DaroodAccount? = null
) : ViewModel() {

    val partyId: String = savedStateHandle["partyId"] ?: ""

    private val _uiState = MutableStateFlow(PartyInfoUiState(myId = account?.id()))
    val uiState: StateFlow<PartyInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeParty(partyId).collect { party -> _uiState.update { it.copy(party = party) } }
        }
        viewModelScope.launch {
            repository.observeMembers(partyId).collect { members ->
                _uiState.update { state ->
                    state.copy(
                        members = members.filter { it.status == MemberStatus.ACTIVE }
                            .sortedWith(compareBy({ it.role != PartyRole.LEADER }, { it.joinedAt ?: Instant.MAX }))
                    )
                }
            }
        }
        viewModelScope.launch { repository.refreshParty(partyId) }
    }
}

@Composable
fun PartyInfoScreen(
    onNavigateBack: () -> Unit,
    onAddPeople: () -> Unit,
    viewModel: PartyInfoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    PartyInfoContent(state = state, onNavigateBack = onNavigateBack, onAddPeople = onAddPeople)
}

@Composable
fun PartyInfoContent(
    state: PartyInfoUiState,
    onNavigateBack: () -> Unit,
    onAddPeople: () -> Unit = {}
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_party_info),
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
                .testTag(DaroodTestTags.INFO_SCREEN),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val party = state.party ?: return@Column
            Text(
                text = party.name,
                style = MaterialTheme.typography.titleLarge,
                color = colors.primaryText,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = pluralStringResource(R.plurals.darood_info_size, state.members.size, state.members.size, party.maxMembers),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            if (state.isLeader && !party.isFinished) {
                PixelButton(
                    text = stringResource(R.string.darood_add_title),
                    onClick = onAddPeople,
                    modifier = Modifier.testTag(DaroodTestTags.INFO_ADD_PEOPLE)
                )
            }
            Text(
                text = stringResource(R.string.darood_info_members),
                style = MaterialTheme.typography.titleSmall,
                color = colors.primaryText,
                modifier = Modifier.semantics { heading() }
            )
            state.members.forEach { MemberRow(it, isMe = it.userId == state.myId) }
        }
    }
}

/** One member: their username, their role, and the day they joined. */
@Composable
private fun MemberRow(member: PartyMember, isMe: Boolean) {
    val colors = PixelTheme.colors
    val name = member.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)
    PixelCard(
        variant = PixelPanelVariant.BORDER,
        contentPadding = 12.dp,
        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.member(member.userId))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isMe) stringResource(R.string.darood_info_you, name) else name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = inkOnPanel(colors.primaryText)
                )
                member.joinedAt?.let { joined ->
                    Text(
                        text = stringResource(R.string.darood_info_joined, joinedDate(joined)),
                        style = MaterialTheme.typography.bodySmall,
                        color = inkOnPanel(colors.onSurfaceVariant)
                    )
                }
            }
            Text(
                text = stringResource(if (member.role == PartyRole.LEADER) R.string.darood_badge_leader else R.string.darood_badge_member),
                style = MaterialTheme.typography.labelMedium,
                color = inkOnPanel(colors.primaryText)
            )
        }
    }
}

/** The day, in the phone's own time zone and language: Oct 19, 2026. */
private fun joinedDate(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(instant)
