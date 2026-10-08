package com.pixelquest.app.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
import com.pixelquest.app.data.repository.RankedProfile
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class LeaderboardTab {
    TOP_STREAKS,
    TOP_LEVELS
}

/** AROUND_YOU shows the heroes just above and below you; TOP shows the full ranked list. */
enum class LeaderboardView {
    AROUND_YOU,
    TOP
}

sealed class LeaderboardAuthState {
    object NotSignedIn : LeaderboardAuthState()
    data class SignedInReadOnly(val userId: String) : LeaderboardAuthState()
    data class SignedInAndOptedIn(val userId: String, val displayName: String) : LeaderboardAuthState()
}

data class LeaderboardUiState(
    val authState: LeaderboardAuthState = LeaderboardAuthState.NotSignedIn,
    val selectedTab: LeaderboardTab = LeaderboardTab.TOP_STREAKS,
    val streakEntries: List<CloudProfileDto> = emptyList(),
    val levelEntries: List<CloudProfileDto> = emptyList(),
    val currentUserRank: UserLeaderboardRank? = null,
    val selectedView: LeaderboardView = LeaderboardView.AROUND_YOU,
    val aroundYouEntries: List<RankedProfile> = emptyList(),
    val isLoadingAroundYou: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val errorMessage: String? = null,
    val lastUpdatedTimestamp: String? = null,
    val reportMessage: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val pageSize = 20L

    // Per-tab bookkeeping, declared before init (whose collector may run at once). Both tabs used to
    // share isLoading and canLoadMore, so a load finishing for the tab the player had just left turned
    // the spinner off or hid LOAD MORE on the other one. A refresh starts a new generation, so a page
    // from before it that arrives late is dropped instead of written over (or appended to) the new list.
    private val loadingTabs = mutableSetOf<LeaderboardTab>()
    private val canLoadMoreByTab = mutableMapOf<LeaderboardTab, Boolean>()
    private val generation = mutableMapOf<LeaderboardTab, Int>()
    private var aroundYouRequest = 0

    init {
        observeAuthAndProfile()
    }

    private fun observeAuthAndProfile() {
        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                userProfileRepository.getProfile()
            ) { user, profile ->
                when {
                    user == null -> LeaderboardAuthState.NotSignedIn
                    profile?.leaderboardOptIn == true -> LeaderboardAuthState.SignedInAndOptedIn(
                        userId = user.id,
                        displayName = profile.leaderboardDisplayName ?: profile.username
                    )
                    else -> LeaderboardAuthState.SignedInReadOnly(userId = user.id)
                }
            }.collect { newAuthState ->
                val prevAuthState = _uiState.value.authState
                _uiState.value = _uiState.value.copy(authState = newAuthState)
                when (newAuthState) {
                    is LeaderboardAuthState.NotSignedIn -> {
                        // Pages still loading from before sign-out are dropped when they arrive.
                        LeaderboardTab.entries.forEach { generation[it] = (generation[it] ?: 0) + 1 }
                        loadingTabs.clear()
                        aroundYouRequest++
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingAroundYou = false,
                            streakEntries = emptyList(),
                            levelEntries = emptyList(),
                            currentUserRank = null,
                            aroundYouEntries = emptyList()
                        )
                    }
                    is LeaderboardAuthState.SignedInReadOnly -> {
                        _uiState.value = _uiState.value.copy(currentUserRank = null, aroundYouEntries = emptyList())
                        if (prevAuthState is LeaderboardAuthState.NotSignedIn) {
                            loadInitialData()
                        }
                    }
                    is LeaderboardAuthState.SignedInAndOptedIn -> {
                        if (prevAuthState is LeaderboardAuthState.NotSignedIn) {
                            loadInitialData()
                        } else if (prevAuthState !is LeaderboardAuthState.SignedInAndOptedIn) {
                            fetchCurrentUserRank()
                        }
                    }
                }
            }
        }
    }

    fun syncAuthState(authUiState: AuthUiState) {
        viewModelScope.launch {
            val profile = userProfileRepository.getProfile().firstOrNull()
            val newAuthState = when (authUiState) {
                is AuthUiState.SignedIn -> {
                    if (profile?.leaderboardOptIn == true) {
                        LeaderboardAuthState.SignedInAndOptedIn(
                            userId = authUiState.user.id,
                            displayName = profile.leaderboardDisplayName ?: profile.username
                        )
                    } else {
                        LeaderboardAuthState.SignedInReadOnly(userId = authUiState.user.id)
                    }
                }
                else -> LeaderboardAuthState.NotSignedIn
            }
            _uiState.value = _uiState.value.copy(authState = newAuthState)
            if (newAuthState is LeaderboardAuthState.SignedInAndOptedIn) {
                fetchCurrentUserRank()
            } else if (newAuthState is LeaderboardAuthState.SignedInReadOnly) {
                _uiState.value = _uiState.value.copy(currentUserRank = null)
            }
        }
    }

    private fun entriesOf(state: LeaderboardUiState, tab: LeaderboardTab) =
        if (tab == LeaderboardTab.TOP_STREAKS) state.streakEntries else state.levelEntries

    private fun LeaderboardUiState.withEntries(tab: LeaderboardTab, entries: List<CloudProfileDto>) =
        if (tab == LeaderboardTab.TOP_STREAKS) copy(streakEntries = entries) else copy(levelEntries = entries)

    /** isLoading and canLoadMore always describe the selected tab. */
    private fun LeaderboardUiState.withSelectedTabFlags() = copy(
        isLoading = selectedTab in loadingTabs,
        canLoadMore = canLoadMoreByTab[selectedTab] ?: true
    )

    fun selectTab(tab: LeaderboardTab) {
        if (_uiState.value.selectedTab == tab) return
        val targetList = entriesOf(_uiState.value, tab)
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            aroundYouEntries = emptyList(),
            errorMessage = null
        ).withSelectedTabFlags()
        if (targetList.isEmpty()) {
            loadInitialData()
        } else {
            fetchCurrentUserRank()
        }
    }

    fun selectView(view: LeaderboardView) {
        _uiState.value = _uiState.value.copy(selectedView = view)
    }

    fun refresh() {
        loadInitialData()
    }

    fun loadInitialData() {
        val currentTab = _uiState.value.selectedTab
        val myGeneration = (generation[currentTab] ?: 0) + 1
        generation[currentTab] = myGeneration
        loadingTabs += currentTab
        canLoadMoreByTab[currentTab] = true
        _uiState.value = _uiState.value.copy(errorMessage = null).withSelectedTabFlags()

        viewModelScope.launch {
            val result = when (currentTab) {
                LeaderboardTab.TOP_STREAKS -> leaderboardRepository.getTopByStreak(limit = pageSize, offset = 0)
                LeaderboardTab.TOP_LEVELS -> leaderboardRepository.getTopByLevel(limit = pageSize, offset = 0)
            }
            // A newer refresh of this tab has started since: its result wins.
            if (generation[currentTab] != myGeneration) return@launch
            loadingTabs -= currentTab

            when (result) {
                is SupabaseResult.Success -> {
                    val timestamp = LocalTime.now().format(timeFormatter)
                    canLoadMoreByTab[currentTab] = result.data.size >= pageSize
                    _uiState.value = _uiState.value
                        .withEntries(currentTab, result.data.distinctBy { it.id })
                        .copy(lastUpdatedTimestamp = timestamp)
                        .withSelectedTabFlags()
                    // Ranks for the tab on screen only: the player may have switched while this loaded.
                    if (_uiState.value.selectedTab == currentTab) fetchCurrentUserRank()
                }
                is SupabaseResult.NetworkError -> showLoadError(currentTab, "Network error: ${result.message}")
                is SupabaseResult.ServerError -> showLoadError(currentTab, "Leaderboard service unavailable (Code ${result.code}).")
                is SupabaseResult.AuthError -> showLoadError(currentTab, "Authentication error: ${result.message}")
                is SupabaseResult.UnknownError -> showLoadError(currentTab, "Failed to load leaderboard: ${result.message}")
            }
        }
    }

    /** A failed load for a tab the player has since left isn't theirs to see on this one. */
    private fun showLoadError(tab: LeaderboardTab, message: String) {
        val state = _uiState.value
        _uiState.value = (if (state.selectedTab == tab) state.copy(errorMessage = message) else state).withSelectedTabFlags()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return

        val tab = state.selectedTab
        val offset = entriesOf(state, tab).size.toLong()
        val myGeneration = generation[tab] ?: 0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val result = when (tab) {
                LeaderboardTab.TOP_STREAKS -> leaderboardRepository.getTopByStreak(limit = pageSize, offset = offset)
                LeaderboardTab.TOP_LEVELS -> leaderboardRepository.getTopByLevel(limit = pageSize, offset = offset)
            }
            // The list was refreshed meanwhile, so this page continues the old one.
            if ((generation[tab] ?: 0) != myGeneration) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
                return@launch
            }

            when (result) {
                is SupabaseResult.Success -> {
                    val newItems = result.data
                    canLoadMoreByTab[tab] = newItems.size >= pageSize
                    // Ranks shift between pages (someone climbs past a player on page one), so the same
                    // hero can come back on the next page; a repeated id crashed the list's keys.
                    val updatedList = (entriesOf(_uiState.value, tab) + newItems).distinctBy { it.id }
                    _uiState.value = _uiState.value
                        .withEntries(tab, updatedList)
                        .copy(isLoadingMore = false)
                        .withSelectedTabFlags()
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            }
        }
    }

    private fun fetchCurrentUserRank() {
        val authState = _uiState.value.authState
        val userId = when (authState) {
            is LeaderboardAuthState.SignedInAndOptedIn -> authState.userId
            // A spectator isn't on the board, so has no rank and nobody "around" them.
            is LeaderboardAuthState.SignedInReadOnly -> {
                _uiState.value = _uiState.value.copy(currentUserRank = null, aroundYouEntries = emptyList())
                return
            }
            LeaderboardAuthState.NotSignedIn -> return
        }

        viewModelScope.launch {
            val sortMode = if (_uiState.value.selectedTab == LeaderboardTab.TOP_STREAKS) {
                LeaderboardSortMode.STREAK
            } else {
                LeaderboardSortMode.LEVEL
            }

            val requestedTab = _uiState.value.selectedTab
            val myRequest = ++aroundYouRequest
            _uiState.value = _uiState.value.copy(isLoadingAroundYou = true)
            val result = leaderboardRepository.getPlayersAroundYou(sortMode, userId, radius = AROUND_YOU_RADIUS)
            // A newer request (the other tab's, or a refresh) owns the result and the spinner.
            if (myRequest != aroundYouRequest) return@launch
            // The player switched tabs meanwhile, so these ranks are in the other order.
            if (_uiState.value.selectedTab != requestedTab) {
                _uiState.value = _uiState.value.copy(isLoadingAroundYou = false)
                return@launch
            }
            when (result) {
                is SupabaseResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currentUserRank = result.data?.you,
                        aroundYouEntries = result.data?.entries.orEmpty(),
                        isLoadingAroundYou = false
                    )
                }
                else -> _uiState.value = _uiState.value.copy(isLoadingAroundYou = false)
            }
        }
    }

    companion object {
        /** Heroes shown above and below you in the AROUND YOU view. */
        const val AROUND_YOU_RADIUS = 3
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun reportProfile(profileId: String, reason: String = "Offensive display name") {
        viewModelScope.launch {
            when (val result = leaderboardRepository.reportProfile(profileId, reason)) {
                is SupabaseResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        reportMessage = "Report submitted. Thank you for keeping PixelQuest safe."
                    )
                }
                is SupabaseResult.AuthError -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Please sign in to report players."
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Could not submit report: check network connection."
                    )
                }
            }
        }
    }

    fun clearReportMessage() {
        _uiState.value = _uiState.value.copy(reportMessage = null)
    }
}
