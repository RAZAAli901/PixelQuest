package com.pixelquest.app.darood.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Keeps one open party current (docs/darood-design.md). While its screen is visible it catches up
 * with the server once, then follows the party live; pulling down refreshes it by hand, which is
 * also the way back when live updates can't get through. [offline] says the last refresh couldn't
 * reach the server, so the screen can say it's showing what the phone has.
 */
class PartyFollower(
    private val repository: DaroodPartyRepository,
    private val scope: CoroutineScope,
    private val partyId: String
) {
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    /** Catches up, then follows live until cancelled: call it while the screen is visible. */
    suspend fun followWhileVisible() {
        refreshNow()
        repository.followParty(partyId).collect { }
    }

    /** Pull-to-refresh: the party, its members and its newest entries, from the server. */
    fun refresh() {
        scope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        _refreshing.value = true
        val result = repository.refreshParty(partyId)
        _offline.value = result is SupabaseResult.NetworkError
        // Waiting entries go too, now there may be a connection.
        if (result is SupabaseResult.Success) repository.sendPending()
        _refreshing.value = false
    }
}

/**
 * Runs [follower] while this screen is at least STARTED, and stops it when the screen goes to the
 * background, so a party is followed live only while someone is looking at it.
 */
@Composable
fun FollowWhileStarted(follower: PartyFollower) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, follower) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { follower.followWhileVisible() }
    }
}

/** Pull-to-refresh around [content]: pulling down calls [onRefresh]; the spinner follows [refreshing]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    val latestRefresh = rememberUpdatedState(onRefresh)
    if (state.isRefreshing) {
        LaunchedEffect(Unit) { latestRefresh.value() }
    }
    LaunchedEffect(refreshing) {
        if (refreshing) state.startRefresh() else state.endRefresh()
    }
    // Clipped: the circle waits hidden just above the box, and must never draw over what sits there
    // (a study party's tabs), only slide down into the box when pulled.
    Box(modifier = modifier.clipToBounds().nestedScroll(state.nestedScrollConnection)) {
        content()
        PullToRefreshContainer(state = state, modifier = Modifier.align(Alignment.TopCenter))
    }
}
