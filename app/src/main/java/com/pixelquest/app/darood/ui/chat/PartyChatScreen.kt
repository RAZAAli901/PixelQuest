package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.ui.chat.ChatScaffold
import com.pixelquest.app.ui.chat.ChatTopBar
import com.pixelquest.app.ui.chat.LocalChatSimpleMode
import com.pixelquest.app.ui.chat.chatLook
import com.pixelquest.app.ui.components.PqConfirmDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.PartyAvatar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
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
 * @param todayTotals the day's numbers for the strip under the top bar; none until they're known.
 * @param myStats the player's numbers in the party: the top bar's 📊 opens them in a sheet.
 * @param simpleMode the app's Simple Mode: the chat's plain look in any theme ([chatLook]).
 * @param milestone the party's milestone, if it has one: a bar pinned under the top bar shows how far
 *   its [total] is toward it, and can be folded away to a slim line.
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
    onRetry: (String) -> Unit = {},
    todayTotals: com.pixelquest.app.darood.domain.DayTotals? = null,
    milestone: Long? = null,
    total: Long = 0,
    finished: Boolean = false,
    simpleMode: Boolean = false,
    myStats: MyStats? = null,
    consistency: com.pixelquest.app.darood.domain.Consistency? = null,
    onOpenStats: () -> Unit = {},
    board: PartyBoard? = null,
    milestoneBanner: Long? = null,
    onDismissBanner: () -> Unit = {},
    completedAt: java.time.Instant? = null,
    startedAt: java.time.Instant? = null,
    onOpenWeekly: (() -> Unit)? = null
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalChatSimpleMode provides simpleMode) {
        PartyChatContent(
            partyName, onNavigateBack, onOpenInfo, isLeader, onAddPeople, memberCount, items, usernames, today, loadingOlder,
            onReachOldest, canPost, draft, onDraftChange, onSend, confirming, onConfirmSend, onCancelSend, onRetry, todayTotals,
            milestone, total, finished, myStats, consistency, onOpenStats, board, milestoneBanner, onDismissBanner,
            completedAt, startedAt, onOpenWeekly
        )
    }
}

@Composable
private fun PartyChatContent(
    partyName: String,
    onNavigateBack: () -> Unit,
    onOpenInfo: () -> Unit,
    isLeader: Boolean,
    onAddPeople: () -> Unit,
    memberCount: Int,
    items: List<ChatItem>,
    usernames: Map<String, String>,
    today: java.time.LocalDate,
    loadingOlder: Boolean,
    onReachOldest: () -> Unit,
    canPost: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    confirming: Int?,
    onConfirmSend: () -> Unit,
    onCancelSend: () -> Unit,
    onRetry: (String) -> Unit,
    todayTotals: com.pixelquest.app.darood.domain.DayTotals?,
    milestone: Long?,
    total: Long,
    finished: Boolean,
    myStats: MyStats?,
    consistency: com.pixelquest.app.darood.domain.Consistency?,
    onOpenStats: () -> Unit,
    board: PartyBoard?,
    milestoneBanner: Long?,
    onDismissBanner: () -> Unit,
    completedAt: java.time.Instant?,
    startedAt: java.time.Instant?,
    onOpenWeekly: (() -> Unit)?
) {
    var showingStats by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    ChatScaffold(
        topBar = {
            ChatTopBar(
                title = partyName,
                subtitle = if (memberCount > 0) {
                    stringResource(R.string.pq_party_card_line, stringResource(R.string.pq_party_darood), pluralStringResource(R.plurals.darood_members, memberCount, memberCount))
                } else {
                    null
                },
                onNavigateBack = onNavigateBack,
                // The party's picture and name open its info, as in a messaging app.
                onOpenTitle = onOpenInfo,
                openTitleLabel = stringResource(R.string.darood_party_info),
                titleTag = DaroodTestTags.CHAT_TITLE,
                avatar = { PartyAvatar(partyName) },
                actions = {
                    if (myStats != null) {
                        PqIconButton(
                            Icons.Rounded.BarChart,
                            stringResource(R.string.darood_stats_title),
                            onClick = {
                                showingStats = true
                                onOpenStats()
                            },
                            modifier = Modifier.testTag(DaroodTestTags.CHAT_STATS)
                        )
                    }
                    if (onOpenWeekly != null) {
                        PqIconButton(
                            Icons.Rounded.EmojiEvents,
                            stringResource(R.string.darood_week_title),
                            onClick = onOpenWeekly,
                            modifier = Modifier.testTag(com.pixelquest.app.darood.ui.weekly.WeeklyRankingTags.OPEN)
                        )
                    }
                    PqIconButton(Icons.Rounded.Info, stringResource(R.string.darood_party_info), onOpenInfo)
                },
                below = {
                    if (milestone != null && milestone > 0) {
                        var folded by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
                        MilestoneBar(total = total, milestone = milestone, finished = finished, collapsed = folded, onToggle = { folded = !folded })
                    }
                    if (todayTotals != null) TodayStrip(todayTotals)
                }
            )
        },
        inputBar = {
            // A finished party takes no more numbers: its card takes the field's place, for good.
            if (completedAt != null) {
                CelebrationCard(
                    total = total,
                    milestone = milestone,
                    completedAt = completedAt,
                    startedAt = startedAt,
                    posters = myStats?.posters ?: board?.rows?.size ?: 0
                )
            } else if (canPost) {
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
        },
        backgroundTag = DaroodTestTags.CHAT_BACKGROUND
    ) {
        // A new milestone, announced over the top of the chat, like a notification.
        MilestoneBanner(target = milestoneBanner, onDismiss = onDismissBanner, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f))
        if (items.isEmpty()) {
            // Nothing posted yet: say how the chat starts; the leader can bring people in from here.
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).testTag(DaroodTestTags.CHAT_SCREEN),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.darood_chat_empty),
                    style = PqType.Body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(DaroodTestTags.CHAT_EMPTY)
                )
                if (isLeader) {
                    com.pixelquest.app.ui.components.PqButton(
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

    if (showingStats && myStats != null) MyStatsSheet(stats = myStats, consistency = consistency, board = board, onDismiss = { showingStats = false })
    if (confirming != null) {
        PqConfirmDialog(
            title = stringResource(R.string.darood_confirm_title, com.pixelquest.app.darood.ui.formatCount(confirming)),
            message = stringResource(R.string.darood_confirm_message),
            confirm = stringResource(R.string.darood_send),
            dismiss = stringResource(R.string.darood_confirm_change),
            onConfirm = onConfirmSend,
            onDismiss = onCancelSend
        )
    }
}

/**
 * The party's screen, as navigation opens it: its party, and its chat's lines, from the cache, kept
 * current from the server while the screen is visible ([follower]).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class PartyChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DaroodPartyRepository,
    account: com.pixelquest.app.darood.data.remote.DaroodAccount? = null,
    settings: com.pixelquest.app.domain.repository.SettingsRepository? = null,
    private val milestoneSeen: com.pixelquest.app.darood.data.MilestoneSeenStore? = null
) : ViewModel() {
    val partyId: String = savedStateHandle["partyId"] ?: ""

    /** The signed-in account, to know whether it leads this party, and which bubbles are its own. */
    val accountId: String? = account?.id()

    val party: StateFlow<Party?> = repository.observeParty(partyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The app's Simple Mode: the chat's plain look. */
    val simpleMode: StateFlow<Boolean> = (settings?.simpleModeEnabled ?: kotlinx.coroutines.flow.flowOf(false))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    /** The player's numbers in the party: total, share and place, from the members' totals. */
    val myStats: StateFlow<MyStats?> = kotlinx.coroutines.flow.combine(repository.observeMemberStats(partyId), repository.observeParty(partyId)) { stats, party ->
        party?.let { myStatsOf(stats, it.total, accountId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val myDays = kotlinx.coroutines.flow.MutableStateFlow<Map<java.time.LocalDate, Long>?>(null)

    /**
     * How steady the player is in the party: active days, streaks and their average, from the days
     * they posted on (the server's), with today counted too if they have a number still on its way.
     * None until [refreshConsistency] has an answer.
     */
    val consistency: StateFlow<com.pixelquest.app.darood.domain.Consistency?> = kotlinx.coroutines.flow.combine(
        myDays,
        repository.observePendingTotalOn(partyId, repository.today())
    ) { days, waiting ->
        days?.let {
            val today = repository.today()
            val withWaiting = if (waiting > 0) it + (today to (it[today] ?: 0) + waiting) else it
            com.pixelquest.app.darood.domain.Consistency.of(withWaiting, today)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Asks the server for the player's days in the party (when their stats are opened). */
    fun refreshConsistency() {
        viewModelScope.launch {
            val result = repository.fetchMyDays(partyId)
            if (result is SupabaseResult.Success) myDays.value = result.data
        }
    }

    private val _milestoneBanner = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)

    /** When the milestone being announced was set: what's marked seen once it's dismissed. */
    @Volatile
    private var announcedSetAt: Long? = null

    /**
     * A milestone to announce at the top of the chat: set (or raised) since the last one the player
     * was told about on this phone, even if that was while they were away. None for a finished
     * party, whose milestone is reached rather than new.
     */
    val milestoneBanner: StateFlow<Long?> = _milestoneBanner

    /** The banner has been seen (tapped away, or gone on its own): it isn't announced again. */
    fun dismissMilestoneBanner() {
        announcedSetAt?.let { milestoneSeen?.markSeen(partyId, it) }
        announcedSetAt = null
        _milestoneBanner.value = null
    }

    /** Everyone's part in the party, kindly ranked ([partyBoardOf]). */
    val board: StateFlow<PartyBoard?> = kotlinx.coroutines.flow.combine(repository.observeMemberStats(partyId), members) { stats, list ->
        partyBoardOf(stats, list, accountId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    /** The day's numbers from the server, without the player's entries still on the phone. */
    private val serverToday = kotlinx.coroutines.flow.MutableStateFlow<com.pixelquest.app.darood.domain.DayTotals?>(null)

    /** The day of [serverToday]: the phone's today when they were asked for. */
    @Volatile
    private var todayIs: java.time.LocalDate = repository.today()

    /**
     * The strip's numbers: the server's for today, with the player's entries still waiting on the
     * phone added on top, so a number shows the moment it's sent, connection or not. None until the
     * server has answered once.
     */
    val today: StateFlow<com.pixelquest.app.darood.domain.DayTotals?> = kotlinx.coroutines.flow.combine(
        serverToday,
        repository.observePendingTotalOn(partyId, todayIs)
    ) { server, waiting -> server?.let { com.pixelquest.app.darood.domain.DayTotals(it.mine + waiting, it.party + waiting) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Asks the server for today's numbers; offline, the last ones stay. */
    fun refreshToday() {
        viewModelScope.launch {
            todayIs = repository.today()
            val result = repository.fetchDayTotals(partyId, todayIs)
            if (result is SupabaseResult.Success) serverToday.value = result.data
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

    init {
        viewModelScope.launch {
            repository.observeParty(partyId).collect { party ->
                val target = party?.milestone
                val setAt = party?.milestoneSetAt?.toEpochMilli()
                if (party == null || target == null || setAt == null || party.isFinished) return@collect
                val seen = milestoneSeen?.seenAt(partyId)
                if (seen == null || setAt > seen) {
                    announcedSetAt = setAt
                    _milestoneBanner.value = target
                }
            }
        }
        refreshToday()
        // As entries come and go (posted here, sent, refused, or arriving live), today's numbers
        // follow, once things have settled for a moment.
        viewModelScope.launch {
            limit.flatMapLatest { repository.observeEntries(partyId, it) }
                .map { entries -> entries.map { (it.clientId ?: it.id.toString()) to it.state } }
                .distinctUntilChanged()
                .drop(1)
                .debounce(TODAY_DELAY_MS)
                .collect { refreshToday() }
        }
    }

    companion object {
        /** How long entries must settle before today's numbers are asked for again. */
        const val TODAY_DELAY_MS = 1_500L

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
    openWeekly: Boolean = false,
    viewModel: PartyChatViewModel = hiltViewModel(),
    weeklyViewModel: com.pixelquest.app.darood.ui.weekly.WeeklyRankingViewModel = hiltViewModel()
) {
    var showingWeekly by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(openWeekly) }
    if (showingWeekly) {
        com.pixelquest.app.darood.ui.weekly.WeeklyRankingSheet(
            state = weeklyViewModel.state.collectAsState().value,
            onDismiss = { showingWeekly = false }
        )
    }
    val party = viewModel.party.collectAsState().value
    val items = viewModel.items.collectAsState().value
    val loadingOlder = viewModel.loadingOlder.collectAsState().value
    val draft = viewModel.draft.collectAsState().value
    val confirming = viewModel.confirming.collectAsState().value
    val todayTotals = viewModel.today.collectAsState().value
    val simpleMode = viewModel.simpleMode.collectAsState().value
    val myStats = viewModel.myStats.collectAsState().value
    val consistency = viewModel.consistency.collectAsState().value
    val board = viewModel.board.collectAsState().value
    val milestoneBanner = viewModel.milestoneBanner.collectAsState().value
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
        canPost = party != null && party.myStatus == com.pixelquest.app.party.domain.MemberStatus.ACTIVE && !party.isFinished,
        draft = draft,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::send,
        confirming = confirming,
        onConfirmSend = viewModel::confirmSend,
        onCancelSend = viewModel::cancelSend,
        onRetry = viewModel::retry,
        todayTotals = todayTotals,
        milestone = party?.milestone,
        total = party?.total ?: 0,
        finished = party?.isFinished == true,
        simpleMode = simpleMode,
        myStats = myStats,
        consistency = consistency,
        onOpenStats = viewModel::refreshConsistency,
        board = board,
        milestoneBanner = milestoneBanner,
        onDismissBanner = viewModel::dismissMilestoneBanner,
        completedAt = party?.completedAt,
        startedAt = party?.createdAt,
        onOpenWeekly = { showingWeekly = true }
    )
}
