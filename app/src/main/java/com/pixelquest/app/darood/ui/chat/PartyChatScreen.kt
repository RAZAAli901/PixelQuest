package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.PartyAvatar
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * A party's own screen, opened from the list, as a group chat: its top bar has the party's picture,
 * its name and how many are in it, and opens the party's info (so does the info button). The chat
 * itself fills the rest.
 *
 * @param memberCount the party's active members; 0 while not known yet, which shows no count.
 */
@Composable
fun PartyChatScreen(
    partyName: String,
    onNavigateBack: () -> Unit,
    onOpenInfo: () -> Unit,
    isLeader: Boolean = false,
    onAddPeople: () -> Unit = {},
    memberCount: Int = 0
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = partyName,
                subtitle = if (memberCount > 0) pluralStringResource(R.plurals.darood_members, memberCount, memberCount) else null,
                leading = { PartyAvatar(partyName) },
                // The title opens the party's info, as in a messaging app; the buttons keep their own taps.
                modifier = Modifier
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.darood_party_info), onClick = onOpenInfo)
                    .testTag(DaroodTestTags.CHAT_TITLE),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            symbol = "◀",
                            contentDescription = stringResource(R.string.navigate_back),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryText
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInfo) {
                        SymbolIcon(
                            symbol = "ℹ️",
                            contentDescription = stringResource(R.string.darood_party_info),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryText
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        ChatBackground(modifier = Modifier.fillMaxSize().padding(innerPadding).testTag(DaroodTestTags.CHAT_BACKGROUND)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).testTag(DaroodTestTags.CHAT_SCREEN),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = stringResource(R.string.darood_chat_soon), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                if (isLeader) {
                    com.pixelquest.app.ui.components.PixelButton(
                        text = stringResource(R.string.darood_add_title),
                        onClick = onAddPeople,
                        modifier = Modifier.testTag(DaroodTestTags.CHAT_ADD_PEOPLE)
                    )
                }
            }
        }
    }
}

/** The party's screen, as navigation opens it: its party from the cache. */
@HiltViewModel
class PartyChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: DaroodPartyRepository,
    account: com.pixelquest.app.darood.data.remote.DaroodAccount? = null
) : ViewModel() {
    val partyId: String = savedStateHandle["partyId"] ?: ""

    /** The signed-in account, to know whether it leads this party. */
    val accountId: String? = account?.id()

    val party: StateFlow<Party?> = repository.observeParty(partyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun PartyChatRoute(
    partyId: String,
    onNavigateBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onAddPeople: () -> Unit = {},
    viewModel: PartyChatViewModel = hiltViewModel()
) {
    val party = viewModel.party.collectAsState().value
    PartyChatScreen(
        partyName = party?.name.orEmpty(),
        onNavigateBack = onNavigateBack,
        onOpenInfo = onOpenInfo,
        isLeader = party != null && party.leaderId != null && party.leaderId == viewModel.accountId,
        onAddPeople = onAddPeople,
        memberCount = party?.memberCount ?: 0
    )
}
