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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
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
                    IconButton(onClick = { viewModel.refresh() }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Tab Selector Row: Top Streaks / Top Levels
            LeaderboardTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading && uiState.streakEntries.isEmpty() && uiState.levelEntries.isEmpty()) {
                    CircularProgressIndicator(
                        color = PixelGold,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Text(
                        text = when (uiState.selectedTab) {
                            LeaderboardTab.TOP_STREAKS -> "Top Streaks Tab Ready"
                            LeaderboardTab.TOP_LEVELS -> "Top Levels Tab Ready"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
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
