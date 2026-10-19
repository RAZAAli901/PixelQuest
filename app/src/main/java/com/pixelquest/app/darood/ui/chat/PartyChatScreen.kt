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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.pixelquest.app.R
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.PartyAvatar
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * A party's own screen, opened from the list, as a group chat: its top bar has the party's picture,
 * its name and how many are in it, and opens the party's info (so does the info button). The chat
 * itself fills the rest.
 *
 * @param memberCount the party's active members; 0 while not known yet, which shows no count.
 * @param items the chat's lines, oldest first ([chatItemsOf]).
 * @param usernames posters' usernames by user id, for the bubbles.
 * @param loadingOlder an older page is on its way; [onReachOldest] asks for one.
 * @param canPost the player may post here (an active member of a party that isn't finished): the
 *   number field shows, holding [draft].
 * @param confirming a big number waiting to be confirmed: a dialog asks if it's what the player meant.
 */
@Composable
fun PartyChatScreen(
    partyName: String,
    onNavigateBack: () -> Unit,
    onOpenInfo: () -> Unit,
    isLeader: Boolean = false,
    onAddPeople: () -> Unit = {},
    memberCount: Int = 0,
    items: List<ChatItem> = emptyList(),
    usernames: Map<String, String> = emptyMap(),
    today: java.time.LocalDate = java.time.LocalDate.now(),
    loadingOlder: Boolean = false,
    onReachOldest: () -> Unit = {},
    canPost: Boolean = false,
    draft: String = "",
    onDraftChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    confirming: Int? = null,
    onConfirmSend: () -> Unit = {},
    onCancelSend: () -> Unit = {},
    onRetry: (String) -> Unit = {}
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
        },
        bottomBar = {
            if (canPost) {
                // Empty and 0 simply can't be sent; too many is said, as the server would refuse it.
                val check = checkDraft(draft)
                NumberInputBar(
                    value = draft,
                    onValueChange = onDraftChange,
                    error = if (check == DraftCheck.TOO_MANY) {
                        stringResource(R.string.darood_input_too_many, com.pixelquest.app.darood.ui.formatCount(com.pixelquest.app.darood.domain.DaroodRules.AMOUNT_MAX))
                    } else {
                        null
                    },
                    onSend = onSend,
                    quickAdd = true
                ) {
                    SendButton(enabled = check.canSend, onClick = onSend)
                }
            }
        }
    ) { innerPadding ->
        ChatBackground(modifier = Modifier.fillMaxSize().padding(innerPadding).testTag(DaroodTestTags.CHAT_BACKGROUND)) {
            if (items.isEmpty()) {
                // Nothing posted yet: say how the chat starts; the leader can bring people in from here.
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).testTag(DaroodTestTags.CHAT_SCREEN),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.darood_chat_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(DaroodTestTags.CHAT_EMPTY)
                    )
                    if (isLeader) {
                        com.pixelquest.app.ui.components.PixelButton(
                            text = stringResource(R.string.darood_add_title),
                            onClick = onAddPeople,
                            modifier = Modifier.testTag(DaroodTestTags.CHAT_ADD_PEOPLE)
                        )
                    }
                }
            } else {
                ChatList(
                    items = items,
                    usernames = usernames,
                    today = today,
                    loadingOlder = loadingOlder,
                    onReachOldest = onReachOldest,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize().testTag(DaroodTestTags.CHAT_SCREEN)
                )
            }
        }
    }

    if (confirming != null) {
        com.pixelquest.app.ui.components.PixelConfirmDialog(
            title = stringResource(R.string.darood_confirm_title, com.pixelquest.app.darood.ui.formatCount(confirming)),
            message = stringResource(R.string.darood_confirm_message),
            confirmText = stringResource(R.string.darood_send),
            dismissText = stringResource(R.string.darood_confirm_change),
            onConfirm = onConfirmSend,
            onDismiss = onCancelSend
        )
    }
}

/**
 * The party's screen, as navigation opens it: its party, and its chat's lines, from the cache, kept
 * current from the server while the screen is visible ([follower]).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PartyChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DaroodPartyRepository,
    account: com.pixelquest.app.darood.data.remote.DaroodAccount? = null
) : ViewModel() {
    val partyId: String = savedStateHandle["partyId"] ?: ""

    /** The signed-in account, to know whether it leads this party, and which bubbles are its own. */
    val accountId: String? = account?.id()

    val party: StateFlow<Party?> = repository.observeParty(partyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** How many entries the chat holds: a page to start with, and a page more each time the reader scrolls up to the oldest. */
    private val limit = kotlinx.coroutines.flow.MutableStateFlow(PAGE)

    /** The server's entries the chat now shows, to know whether the phone has more than that. */
    @Volatile
    private var shownFromServer = 0

    /** False once the server has said there's nothing older than what the phone has. */
    @Volatile
    private var serverHasOlder = true

    private val _loadingOlder = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** An older page is on its way. */
    val loadingOlder: StateFlow<Boolean> = _loadingOlder

    private val members = repository.observeMembers(partyId)

    /** The chat's lines, oldest first: entries, days and what happened, as [chatItemsOf] makes them. */
    val items: StateFlow<List<ChatItem>> = kotlinx.coroutines.flow.combine(
        limit.flatMapLatest { repository.observeEntries(partyId, it) },
        members,
        repository.observeParty(partyId)
    ) { entries, members, party ->
        shownFromServer = entries.count { it.id != null }
        // The cache gives the newest first; the chat reads oldest first.
        chatItemsOf(entries.asReversed(), accountId, java.time.ZoneId.systemDefault(), partyEventsOf(party, members))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Posters' usernames by user id. */
    val usernames: StateFlow<Map<String, String>> = members
        .map { list -> list.mapNotNull { member -> member.username?.let { member.userId to it } }.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _draft = kotlinx.coroutines.flow.MutableStateFlow("")

    /** The number being typed: digits only, at most six ([amountDigits]). */
    val draft: StateFlow<String> = _draft

    fun onDraftChange(raw: String) {
        _draft.value = amountDigits(raw)
    }

    private val _confirming = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)

    /** A number of [CONFIRM_FROM] or more, waiting for the player to say it's what they meant. */
    val confirming: StateFlow<Int?> = _confirming

    /**
     * Sends the number typed, if it can go ([checkDraft]). From [CONFIRM_FROM] up, it waits for the
     * player to confirm it first ([confirmSend]). The field empties as it goes, so a second tap can't
     * send it twice. The entry is saved on the phone first, and sent when it can be.
     */
    fun send() {
        val typed = _draft.value
        if (!checkDraft(typed).canSend || _confirming.value != null) return
        val amount = typed.toInt()
        if (amount >= CONFIRM_FROM) {
            _confirming.value = amount
            return
        }
        post(amount)
    }

    /** The player says the big number is right: it goes. */
    fun confirmSend() {
        val amount = _confirming.value ?: return
        _confirming.value = null
        post(amount)
    }

    /** Tries a refused entry of the player's again ([DaroodPartyRepository.retryEntry]). */
    fun retry(clientId: String) {
        viewModelScope.launch { repository.retryEntry(clientId) }
    }

    /** Not what they meant: nothing is sent, and the number stays in the field to put right. */
    fun cancelSend() {
        _confirming.value = null
    }

    private fun post(amount: Int) {
        _draft.value = ""
        viewModelScope.launch {
            try {
                repository.addEntry(partyId, amount)
            } catch (gone: android.database.sqlite.SQLiteConstraintException) {
                // The party left the phone (deleted, or the player left it elsewhere) while the number
                // was being typed: there's nowhere to post it, and the screen is about to close.
            }
        }
    }

    /** Keeps the party current while its screen is visible. */
    val follower = com.pixelquest.app.darood.ui.PartyFollower(repository, viewModelScope, partyId)

    /**
     * The reader has scrolled up to the oldest line shown: shows a page more. The phone may have it
     * already; if not, the page before its oldest comes from the server. Once the server has nothing
     * older and everything the phone has is shown, it stops. Offline, the next scroll up tries again.
     */
    fun loadOlder() {
        if (_loadingOlder.value) return
        val allShown = shownFromServer < limit.value
        if (allShown && !serverHasOlder) return
        _loadingOlder.value = true
        viewModelScope.launch {
            if (serverHasOlder) {
                when (val result = repository.loadOlderEntries(partyId)) {
                    is SupabaseResult.Success -> serverHasOlder = result.data
                    else -> {
                        _loadingOlder.value = false
                        return@launch
                    }
                }
            }
            limit.value += PAGE
            _loadingOlder.value = false
        }
    }

    companion object {
        /** Entries in the chat to start with: the server's page. */
        const val PAGE = com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource.PAGE_SIZE
    }
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
    val items = viewModel.items.collectAsState().value
    val loadingOlder = viewModel.loadingOlder.collectAsState().value
    val draft = viewModel.draft.collectAsState().value
    val confirming = viewModel.confirming.collectAsState().value
    val usernames = viewModel.usernames.collectAsState().value
    com.pixelquest.app.darood.ui.FollowWhileStarted(viewModel.follower)
    PartyChatScreen(
        partyName = party?.name.orEmpty(),
        onNavigateBack = onNavigateBack,
        onOpenInfo = onOpenInfo,
        isLeader = party != null && party.leaderId != null && party.leaderId == viewModel.accountId,
        onAddPeople = onAddPeople,
        memberCount = party?.memberCount ?: 0,
        items = items,
        usernames = usernames,
        loadingOlder = loadingOlder,
        onReachOldest = viewModel::loadOlder,
        // Only an active member posts, and only while the party isn't finished (the server insists too).
        canPost = party != null && party.myStatus == com.pixelquest.app.darood.domain.MemberStatus.ACTIVE && !party.isFinished,
        draft = draft,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::send,
        confirming = confirming,
        onConfirmSend = viewModel::confirmSend,
        onCancelSend = viewModel::cancelSend,
        onRetry = viewModel::retry
    )
}
