package com.pixelquest.app.ui.screens.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class AvatarSelectionViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    val currentAvatarId: StateFlow<String> = userProfileRepository.getProfile()
        .map { profile -> profile?.avatarId ?: "avatar_hero" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "avatar_hero"
        )

    fun selectAvatar(avatarId: String) {
        viewModelScope.launch {
            userProfileRepository.getProfile().firstOrNull()?.let { profile ->
                userProfileRepository.updateProfile(profile.copy(avatarId = avatarId))
            }
        }
    }
}
