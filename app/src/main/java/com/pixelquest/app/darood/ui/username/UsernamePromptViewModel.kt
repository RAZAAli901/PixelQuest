package com.pixelquest.app.darood.ui.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.darood.data.UsernamePromptStore
import com.pixelquest.app.darood.data.UsernameRepository
import com.pixelquest.app.data.remote.SupabaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Asks a signed-in account without a username to choose one, once on this phone: after signing
 * in, or at the first launch that finds such an account signed in (after an update, say). The
 * app's frame opens the username screen while [prompt] is on, then calls [promptShown]. If the
 * check can't reach the server, it's asked at a later launch instead.
 *
 * The check runs at every launch and sign-in, asked or not: it also refreshes the name the phone
 * keeps ([com.pixelquest.app.darood.data.UsernameCache]), say after a change on another phone.
 */
@HiltViewModel
class UsernamePromptViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val usernames: UsernameRepository,
    private val store: UsernamePromptStore
) : ViewModel() {

    private val _prompt = MutableStateFlow(false)
    val prompt: StateFlow<Boolean> = _prompt.asStateFlow()

    private var pendingAccount: String? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.map { it?.id }.distinctUntilChanged().collectLatest { accountId ->
                pendingAccount = null
                _prompt.value = false
                if (accountId == null) return@collectLatest
                val mine = usernames.fetchMyUsername() as? SupabaseResult.Success ?: return@collectLatest
                if (store.wasPrompted(accountId)) return@collectLatest
                if (mine.data == null) {
                    pendingAccount = accountId
                    _prompt.value = true
                } else {
                    // It has a name (chosen on another phone, say): nothing to ask, now or later.
                    store.markPrompted(accountId)
                }
            }
        }
    }

    /** The username screen opened: not again for this account on this phone. */
    fun promptShown() {
        pendingAccount?.let(store::markPrompted)
        pendingAccount = null
        _prompt.value = false
    }
}
