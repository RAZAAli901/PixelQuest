package com.pixelquest.app.ui.screens.leveling

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LevelHistoryScreen(
    viewModel: LevelHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.history.collectAsState()
    LevelHistoryScreenContent(
        history = historyList,
        modifier = modifier
    )
}

@Composable
fun LevelHistoryScreenContent(
    history: List<LevelHistoryEntity>,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📜 LEVEL HISTORY",
            style = PixelTypography.titleLarge,
            color = PixelGold
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No levels earned yet — complete perfect days to level up!",
                    style = PixelTypography.bodyMedium,
                    color = PixelTextWhite
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history) { entry ->
                    PixelCard(
                        variant = PixelPanelVariant.BLUE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "LEVEL ${entry.level}",
                                    style = PixelTypography.titleMedium,
                                    color = PixelGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "MODE: ${entry.difficultyAtTimeOfLevelUp}",
                                    style = PixelTypography.bodySmall,
                                    color = PixelTextWhite
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(entry.achievedDate)),
                                style = PixelTypography.bodySmall,
                                color = PixelTextWhite
                            )
                        }
                    }
                }
            }
        }
    }
}
