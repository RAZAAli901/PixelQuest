package com.pixelquest.app.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqQuietButton
import com.pixelquest.app.ui.components.PqSegmented
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    // Signed out: UI v2's locked board (mock 2J), which signs in with Google right there.
    if (uiState.authState is LeaderboardAuthState.NotSignedIn) {
        com.pixelquest.app.ui.screens.LeaderboardLockedRoute(onEmailCode = onNavigateToAccount, onBack = onNavigateBack)
        return
    }
    LeaderboardContent(
        uiState = uiState,
        onTabSelected = { viewModel.selectTab(it) },
        onLoadMore = { viewModel.loadMore() },
        onRefresh = { viewModel.refresh() },
        onReportProfile = { id, name -> viewModel.reportProfile(id, "Flagged offensive display name: $name") },
        onViewSelected = { viewModel.selectView(it) },
        onNavigateBack = onNavigateBack,
        onNavigateToAccount = onNavigateToAccount,
        modifier = modifier,
    )
}

@Composable
fun LeaderboardContent(
    uiState: LeaderboardUiState,
    onTabSelected: (LeaderboardTab) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onReportProfile: (String, String) -> Unit = { _, _ -> },
    onViewSelected: (LeaderboardView) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reportingItem by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pullOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { 64.dp.toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && pullOffset > 0f) {
                    val consumed = available.y.coerceAtLeast(-pullOffset)
                    pullOffset += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0) {
                    pullOffset = (pullOffset + available.y * 0.4f).coerceAtMost(refreshThresholdPx * 1.5f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset >= refreshThresholdPx) {
                    onRefresh()
                }
                pullOffset = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .nestedScroll(nestedScrollConnection)
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 12.dp),
    ) {
        PqTopBar("Leaderboard", onBack = onNavigateBack) {
            PqIconButton(Icons.Rounded.Refresh, "Refresh the leaderboard", onRefresh)
        }

        // Pulled down, or updating: said in a small line.
        if (pullOffset > 0f || uiState.isLoading) {
            Text(
                text = when {
                    uiState.isLoading -> "Updating the board…"
                    pullOffset >= refreshThresholdPx -> "Let go to refresh"
                    else -> "Pull down to refresh"
                },
                style = PqType.MetaStrong,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(PqColors.Sunken, pqShape(8.dp)).padding(vertical = 6.dp),
            )
        }

        PqSegmented(
            options = listOf("Top streaks", "Top levels"),
            selected = if (uiState.selectedTab == LeaderboardTab.TOP_STREAKS) 0 else 1,
            onSelect = { onTabSelected(if (it == 0) LeaderboardTab.TOP_STREAKS else LeaderboardTab.TOP_LEVELS) },
        )

        val isOnBoard = uiState.authState is LeaderboardAuthState.SignedInAndOptedIn
        val showAroundYou = isOnBoard && uiState.selectedView == LeaderboardView.AROUND_YOU
        if (isOnBoard) {
            PqSegmented(
                options = listOf("Around you", "Top heroes"),
                selected = if (uiState.selectedView == LeaderboardView.AROUND_YOU) 0 else 1,
                onSelect = { onViewSelected(if (it == 0) LeaderboardView.AROUND_YOU else LeaderboardView.TOP) },
            )
        }

        if (uiState.lastUpdatedTimestamp != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Updated ${uiState.lastUpdatedTimestamp}", style = PqType.Meta, modifier = Modifier.weight(1f))
                Text(text = if (uiState.isLoading) "Syncing…" else "Online", style = PqType.MetaStrong.copy(color = if (uiState.isLoading) PqColors.RedText else PqColors.Live))
            }
        }

        val currentUserId = when (val auth = uiState.authState) {
            is LeaderboardAuthState.SignedInAndOptedIn -> auth.userId
            is LeaderboardAuthState.SignedInReadOnly -> auth.userId
            LeaderboardAuthState.NotSignedIn -> null
        }
        val currentEntries = remember(uiState.selectedTab, uiState.streakEntries, uiState.levelEntries) {
            when (uiState.selectedTab) {
                LeaderboardTab.TOP_STREAKS -> uiState.streakEntries
                LeaderboardTab.TOP_LEVELS -> uiState.levelEntries
            }
        }

        // A refresh or a page that failed while the cached board still shows.
        if (uiState.errorMessage != null && currentEntries.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .comic(bg = PqColors.Paper, radius = PqDimens.RadiusSm, border = PqDimens.BorderThin, shadow = 0.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(text = uiState.errorMessage, style = PqType.Meta.copy(color = PqColors.Error), modifier = Modifier.weight(1f))
                PqQuietButton("Retry", onRefresh)
            }
        }
        if (uiState.reportMessage != null) {
            Text(
                text = uiState.reportMessage,
                style = PqType.Meta.copy(color = PqColors.Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .comic(bg = PqColors.Sky, radius = PqDimens.RadiusSm, border = PqDimens.BorderThin, shadow = 0.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (showAroundYou) {
            AroundYouList(
                entries = uiState.aroundYouEntries,
                selectedTab = uiState.selectedTab,
                currentUserId = currentUserId,
                isLoading = uiState.isLoadingAroundYou || uiState.isLoading,
                onReport = { id, name -> reportingItem = id to name },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (uiState.isLoading && currentEntries.isEmpty()) {
                    // M12: shimmering rows where the board will be.
                    com.pixelquest.app.ui.motion.PqSkeleton(Modifier.align(Alignment.TopCenter).padding(top = 8.dp), rows = 6, rowHeight = 56.dp)
                } else if (uiState.errorMessage != null && currentEntries.isEmpty()) {
                    LeaderboardErrorState(errorMessage = uiState.errorMessage, onRetry = onRefresh, modifier = Modifier.fillMaxWidth())
                } else if (currentEntries.isEmpty()) {
                    Text(
                        text = "No heroes ranked yet. Join from Account to claim the top spot.",
                        style = PqType.Body.copy(color = PqColors.Ink2),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                    val shouldLoadMore = remember {
                        androidx.compose.runtime.derivedStateOf {
                            val totalItems = listState.layoutInfo.totalItemsCount
                            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            totalItems > 0 && lastVisibleItemIndex >= totalItems - 2
                        }
                    }
                    androidx.compose.runtime.LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value && uiState.canLoadMore && !uiState.isLoading && !uiState.isLoadingMore) {
                            onLoadMore()
                        }
                    }
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(count = currentEntries.size, key = { index -> currentEntries[index].id }) { index ->
                            val item = currentEntries[index]
                            val isCurrentUser = currentUserId != null && item.id == currentUserId
                            BoardRow(
                                rank = index + 1,
                                profile = item,
                                tab = uiState.selectedTab,
                                isCurrentUser = isCurrentUser,
                                onReport = if (!isCurrentUser) {
                                    { reportingItem = item.id to item.displayName }
                                } else {
                                    null
                                },
                            )
                        }
                        if (uiState.canLoadMore) {
                            item(key = "load_more_indicator") {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(color = PqColors.Ink, modifier = Modifier.size(24.dp))
                                    } else {
                                        PqButton("Load more", onLoadMore, style = PqButtonStyle.Secondary, height = 40.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // The player's own rank, pinned under the board (Around you already shows it), or how to join.
        when (uiState.authState) {
            is LeaderboardAuthState.SignedInReadOnly -> SpectatorModeBanner(onNavigateToAccount = onNavigateToAccount)
            is LeaderboardAuthState.SignedInAndOptedIn -> {
                val userRank = uiState.currentUserRank
                if (userRank != null && !showAroundYou) {
                    PqGroupLabel("Your ranking")
                    BoardRow(rank = userRank.rank, profile = userRank.profile, tab = uiState.selectedTab, isCurrentUser = true, onReport = null)
                }
            }
            LeaderboardAuthState.NotSignedIn -> Unit
        }
    }

    if (reportingItem != null) {
        val (profileId, name) = reportingItem!!
        PqConfirmDialog(
            title = "Report this name?",
            message = "Report '$name' for an offensive or inappropriate public display name?",
            confirm = "Report",
            dismiss = "Cancel",
            onConfirm = {
                onReportProfile(profileId, name)
                reportingItem = null
            },
            onDismiss = { reportingItem = null },
        )
    }
}

/** A hero's row for the board's tab: their streak, or their level and XP. */
@Composable
private fun BoardRow(
    rank: Int,
    profile: com.pixelquest.app.data.remote.model.CloudProfileDto,
    tab: LeaderboardTab,
    isCurrentUser: Boolean,
    onReport: (() -> Unit)?,
) {
    when (tab) {
        LeaderboardTab.TOP_STREAKS -> PixelLeaderboardRow(
            rank = rank,
            displayName = profile.displayName,
            statLabel = "Day streak",
            statValue = profile.currentStreak.toString(),
            isCurrentUser = isCurrentUser,
            onReportClicked = onReport?.let { report -> { _ -> report() } },
        )
        LeaderboardTab.TOP_LEVELS -> PixelLeaderboardRow(
            rank = rank,
            displayName = profile.displayName,
            statLabel = "${profile.totalXp} XP",
            statValue = "Lvl ${profile.level}",
            isCurrentUser = isCurrentUser,
            onReportClicked = onReport?.let { report -> { _ -> report() } },
        )
    }
}

/**
 * The heroes ranked just above and below you, with their real leaderboard ranks and your row
 * highlighted.
 */
@Composable
fun AroundYouList(
    entries: List<com.pixelquest.app.data.repository.RankedProfile>,
    selectedTab: LeaderboardTab,
    currentUserId: String?,
    isLoading: Boolean,
    onReport: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            entries.isEmpty() && isLoading -> CircularProgressIndicator(color = PqColors.Ink, modifier = Modifier.size(36.dp))
            entries.isEmpty() -> Text(
                text = "You're not on the board yet. Your rank appears after your profile syncs. Pull down to refresh.",
                style = PqType.Body.copy(color = PqColors.Ink2),
                textAlign = TextAlign.Center,
            )
            else -> androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "around_you_header") { PqGroupLabel("Above and below you") }
                items(count = entries.size, key = { index -> "around_" + entries[index].profile.id }) { index ->
                    val entry = entries[index]
                    val profile = entry.profile
                    val isCurrentUser = currentUserId != null && profile.id == currentUserId
                    BoardRow(
                        rank = entry.rank,
                        profile = profile,
                        tab = selectedTab,
                        isCurrentUser = isCurrentUser,
                        onReport = if (isCurrentUser) {
                            null
                        } else {
                            { onReport(profile.id, profile.displayName) }
                        },
                    )
                }
            }
        }
    }
}

/** Signed in but not on the board: what that means, and the way to join. */
@Composable
fun SpectatorModeBanner(onNavigateToAccount: () -> Unit, modifier: Modifier = Modifier) {
    PqCard(bg = PqColors.Sky, radius = PqDimens.Radius, shadow = 0.dp, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
            Text(text = "You're watching", style = PqType.BodyStrong)
            Text(text = "You see the board, but you're not on it. Join from Account to appear on the leaderboard.", style = PqType.Meta.copy(color = PqColors.Ink))
            PqButton("Join the leaderboard", onNavigateToAccount, Modifier.fillMaxWidth())
        }
    }
}

/** The board couldn't load: why, a way to try again, and that nothing else is affected. */
@Composable
fun LeaderboardErrorState(errorMessage: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    PqCard(radius = PqDimens.RadiusLg, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(pqShape(10.dp)).background(PqColors.Rose), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
                }
                Text(text = "Couldn't reach the leaderboard", style = PqType.BodyStrong)
            }
            Text(text = errorMessage, style = PqType.Body.copy(color = PqColors.Ink2))
            PqButton("Try again", onRetry, Modifier.fillMaxWidth())
            Text(text = "Your quests, streaks and XP keep counting on this phone meanwhile.", style = PqType.Meta)
        }
    }
}
