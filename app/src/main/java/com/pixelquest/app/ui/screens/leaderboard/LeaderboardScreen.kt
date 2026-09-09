package com.pixelquest.app.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelSurfaceBorder
import com.pixelquest.app.ui.theme.PixelSurfaceDark
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    LeaderboardContent(
        uiState = uiState,
        onTabSelected = { viewModel.selectTab(it) },
        onLoadMore = { viewModel.loadMore() },
        onRefresh = { viewModel.refresh() },
        onNavigateBack = onNavigateBack,
        onNavigateToAccount = onNavigateToAccount,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardContent(
    uiState: LeaderboardUiState,
    onTabSelected: (LeaderboardTab) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PixelBackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LEADERBOARD",
                        style = MaterialTheme.typography.titleLarge,
                        color = PixelGold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_back),
                            contentDescription = "Back",
                            tint = PixelGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Refresh Leaderboard",
                            tint = PixelCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PixelSurfaceDark
                )
            )
        }
    ) { innerPadding ->
        if (uiState.authState is LeaderboardAuthState.NotSignedIn) {
            NotSignedInLeaderboardState(
                onNavigateToAccount = onNavigateToAccount,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Pull-to-refresh banner indicator
                if (pullOffset > 0f || uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PixelSurfaceDark)
                            .border(1.dp, PixelCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                uiState.isLoading -> "🔄 UPDATING HALL OF FAME..."
                                pullOffset >= refreshThresholdPx -> "⚡ RELEASE TO REFRESH ⚡"
                                else -> "▼ PULL TO REFRESH ▼"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pullOffset >= refreshThresholdPx) PixelGold else PixelCyan,
                            fontSize = 8.sp
                        )
                    }
                }

                // Tab Selector Row: Top Streaks / Top Levels
                LeaderboardTabRow(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Last updated timestamp & status banner
                if (uiState.lastUpdatedTimestamp != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UPDATED: ${uiState.lastUpdatedTimestamp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PixelTextMuted,
                            fontSize = 7.sp
                        )
                        Text(
                            text = if (uiState.isLoading) "SYNCING..." else "ONLINE ●",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isLoading) PixelGold else PixelGreen,
                            fontSize = 7.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

            // Tab Content: LazyColumn of PixelLeaderboardRows
            val currentUserId = when (val auth = uiState.authState) {
                is LeaderboardAuthState.SignedInAndOptedIn -> auth.userId
                is LeaderboardAuthState.SignedInReadOnly -> auth.userId
                LeaderboardAuthState.NotSignedIn -> null
            }

            val currentEntries = when (uiState.selectedTab) {
                LeaderboardTab.TOP_STREAKS -> uiState.streakEntries
                LeaderboardTab.TOP_LEVELS -> uiState.levelEntries
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading && currentEntries.isEmpty()) {
                    CircularProgressIndicator(
                        color = PixelGold,
                        modifier = Modifier.size(36.dp)
                    )
                } else if (uiState.errorMessage != null && currentEntries.isEmpty()) {
                    LeaderboardErrorState(
                        errorMessage = uiState.errorMessage,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (currentEntries.isEmpty()) {
                    Text(
                        text = "NO HEROES RANKED YET\nOpt in from Account Settings to claim the top spot!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                } else {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                    val shouldLoadMore = androidx.compose.runtime.remember {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = currentEntries.size,
                            key = { index -> currentEntries[index].id }
                        ) { index ->
                            val item = currentEntries[index]
                            val rank = index + 1
                            val isCurrentUser = currentUserId != null && item.id == currentUserId

                            when (uiState.selectedTab) {
                                LeaderboardTab.TOP_STREAKS -> {
                                    PixelLeaderboardRow(
                                        rank = rank,
                                        displayName = item.displayName,
                                        statLabel = "DAYS STREAK",
                                        statValue = "${item.currentStreak} 🔥",
                                        isCurrentUser = isCurrentUser
                                    )
                                }
                                LeaderboardTab.TOP_LEVELS -> {
                                    PixelLeaderboardRow(
                                        rank = rank,
                                        displayName = item.displayName,
                                        statLabel = "XP: ${item.totalXp}",
                                        statValue = "LVL ${item.level} ⚔️",
                                        isCurrentUser = isCurrentUser
                                    )
                                }
                            }
                        }

                        // Pagination Load More trigger item
                        if (uiState.canLoadMore) {
                            item(key = "load_more_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            color = PixelGold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PixelSurfaceDark)
                                                .border(1.dp, PixelCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .clickable { onLoadMore() }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "▼ LOAD MORE HEROES ▼",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = PixelCyan,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer: Pinned Current User Rank (Opted In) OR Spectator Mode Banner (Read Only)
            when (uiState.authState) {
                is LeaderboardAuthState.SignedInReadOnly -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    SpectatorModeBanner(
                        onNavigateToAccount = onNavigateToAccount
                    )
                }
                is LeaderboardAuthState.SignedInAndOptedIn -> {
                    val userRank = uiState.currentUserRank
                    if (userRank != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PixelSurfaceDark)
                                .border(1.dp, PixelGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "★ YOUR RANKING",
                                style = MaterialTheme.typography.labelSmall,
                                color = PixelGold,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                            when (uiState.selectedTab) {
                                LeaderboardTab.TOP_STREAKS -> {
                                    PixelLeaderboardRow(
                                        rank = userRank.rank,
                                        displayName = userRank.profile.displayName,
                                        statLabel = "DAYS STREAK",
                                        statValue = "${userRank.profile.currentStreak} 🔥",
                                        isCurrentUser = true
                                    )
                                }
                                LeaderboardTab.TOP_LEVELS -> {
                                    PixelLeaderboardRow(
                                        rank = userRank.rank,
                                        displayName = userRank.profile.displayName,
                                        statLabel = "XP: ${userRank.profile.totalXp}",
                                        statValue = "LVL ${userRank.profile.level} ⚔️",
                                        isCurrentUser = true
                                    )
                                }
                            }
                        }
                    }
                }
                LeaderboardAuthState.NotSignedIn -> Unit
            }
        }
    }
}

@Composable
fun LeaderboardTabRow(
    selectedTab: LeaderboardTab,
    onTabSelected: (LeaderboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PixelSurfaceDark)
            .border(1.dp, PixelSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LeaderboardTabButton(
            title = "🔥 TOP STREAKS",
            isSelected = selectedTab == LeaderboardTab.TOP_STREAKS,
            onClick = { onTabSelected(LeaderboardTab.TOP_STREAKS) },
            modifier = Modifier.weight(1f)
        )

        LeaderboardTabButton(
            title = "⚔️ TOP LEVELS",
            isSelected = selectedTab == LeaderboardTab.TOP_LEVELS,
            onClick = { onTabSelected(LeaderboardTab.TOP_LEVELS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun LeaderboardTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)
    val backgroundColor = if (isSelected) PixelGold.copy(alpha = 0.2f) else Color.Transparent
    val borderColor = if (isSelected) PixelGold else Color.Transparent
    val textColor = if (isSelected) PixelGold else PixelTextMuted

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontSize = 9.sp
        )
    }
}

@Composable
fun NotSignedInLeaderboardState(
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PixelSurfaceDark)
                .border(2.dp, PixelGold, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "🔒",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "HALL OF FAME LOCKED",
                style = MaterialTheme.typography.titleLarge,
                color = PixelGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Sign in with your Google Account to view live global rankings, streaks, and top heroes across the realm.",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelTextWhite,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            com.pixelquest.app.ui.components.PixelButton(
                text = "🔑 SIGN IN VIA ACCOUNT",
                onClick = onNavigateToAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SpectatorModeBanner(
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PixelSurfaceDark)
            .border(1.dp, PixelCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "👁️ SPECTATOR MODE",
                    style = MaterialTheme.typography.titleMedium,
                    color = PixelCyan,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "You are viewing the leaderboard in read-only spectator mode. Opt in from Account Settings to appear on the leaderboard!",
                style = MaterialTheme.typography.bodySmall,
                color = PixelTextMuted,
                textAlign = TextAlign.Center,
                fontSize = 8.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            com.pixelquest.app.ui.components.PixelButton(
                text = "⚔️ JOIN LEADERBOARD",
                onClick = onNavigateToAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LeaderboardErrorState(
    errorMessage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PixelSurfaceDark)
                .border(2.dp, PixelRed, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "📡",
                fontSize = 36.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "REALM TRANSMISSION FAILED",
                style = MaterialTheme.typography.titleMedium,
                color = PixelRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = PixelTextWhite,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "🛡️ Offline Mode Safe: Local quests, streaks, and XP continue tracking uninterrupted on your device.",
                style = MaterialTheme.typography.labelSmall,
                color = PixelTextMuted,
                textAlign = TextAlign.Center,
                fontSize = 8.sp,
                lineHeight = 14.sp
            )
        }
    }
}

