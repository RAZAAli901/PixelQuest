package com.pixelquest.app.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.LeaderboardRepository
import com.pixelquest.app.data.repository.LeaderboardSortMode
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
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val errorMessage: String? = null,
    val lastUpdatedTimestamp: String? = null
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
                        _uiState.value = _uiState.value.copy(
                            streakEntries = emptyList(),
                            levelEntries = emptyList(),
                            currentUserRank = null
                        )
                    }
                    is LeaderboardAuthState.SignedInReadOnly -> {
                        _uiState.value = _uiState.value.copy(currentUserRank = null)
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

    fun selectTab(tab: LeaderboardTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.value = _uiState.value.copy(selectedTab = tab, errorMessage = null)
        val currentList = if (tab == LeaderboardTab.TOP_STREAKS) _uiState.value.streakEntries else _uiState.value.levelEntries
        if (currentList.isEmpty()) {
            loadInitialData()
        } else {
            fetchCurrentUserRank()
        }
    }

    fun refresh() {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                canLoadMore = true
            )

            val currentTab = _uiState.value.selectedTab
            val result = when (currentTab) {
                LeaderboardTab.TOP_STREAKS -> leaderboardRepository.getTopByStreak(limit = pageSize, offset = 0)
                LeaderboardTab.TOP_LEVELS -> leaderboardRepository.getTopByLevel(limit = pageSize, offset = 0)
            }

            when (result) {
                is SupabaseResult.Success -> {
                    val timestamp = LocalTime.now().format(timeFormatter)
                    _uiState.value = when (currentTab) {
                        LeaderboardTab.TOP_STREAKS -> _uiState.value.copy(
                            streakEntries = result.data,
                            isLoading = false,
                            canLoadMore = result.data.size >= pageSize,
                            lastUpdatedTimestamp = timestamp
                        )
                        LeaderboardTab.TOP_LEVELS -> _uiState.value.copy(
                            levelEntries = result.data,
                            isLoading = false,
                            canLoadMore = result.data.size >= pageSize,
                            lastUpdatedTimestamp = timestamp
                        )
                    }
                    fetchCurrentUserRank()
                }
                is SupabaseResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Network error: ${result.userMessage}"
                    )
                }
                is SupabaseResult.ServerError -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Leaderboard service unavailable (Code ${result.code})."
                    )
                }
                is SupabaseResult.AuthError -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Authentication error: ${result.userMessage}"
                    )
                }
                is SupabaseResult.UnknownError -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to load leaderboard: ${result.message}"
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return

        val currentList = if (state.selectedTab == LeaderboardTab.TOP_STREAKS) state.streakEntries else state.levelEntries
        val offset = currentList.size.toLong()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val result = when (state.selectedTab) {
                LeaderboardTab.TOP_STREAKS -> leaderboardRepository.getTopByStreak(limit = pageSize, offset = offset)
                LeaderboardTab.TOP_LEVELS -> leaderboardRepository.getTopByLevel(limit = pageSize, offset = offset)
            }

            when (result) {
                is SupabaseResult.Success -> {
                    val newItems = result.data
                    val updatedList = currentList + newItems
                    _uiState.value = when (state.selectedTab) {
                        LeaderboardTab.TOP_STREAKS -> _uiState.value.copy(
                            streakEntries = updatedList,
                            isLoadingMore = false,
                            canLoadMore = newItems.size >= pageSize
                        )
                        LeaderboardTab.TOP_LEVELS -> _uiState.value.copy(
                            levelEntries = updatedList,
                            isLoadingMore = false,
                            canLoadMore = newItems.size >= pageSize
                        )
                    }
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
            is LeaderboardAuthState.SignedInReadOnly -> authState.userId
            LeaderboardAuthState.NotSignedIn -> return
        }

        viewModelScope.launch {
            val sortMode = if (_uiState.value.selectedTab == LeaderboardTab.TOP_STREAKS) {
                LeaderboardSortMode.STREAK
            } else {
                LeaderboardSortMode.LEVEL
            }

            when (val rankResult = leaderboardRepository.getCurrentUserRank(sortMode, userId)) {
                is SupabaseResult.Success -> {
                    _uiState.value = _uiState.value.copy(currentUserRank = rankResult.data)
                }
                else -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
