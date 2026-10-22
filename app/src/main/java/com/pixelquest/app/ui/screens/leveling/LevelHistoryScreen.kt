package com.pixelquest.app.ui.screens.leveling

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Every level the hero reached (Hero > Level history), the newest first as the database keeps them. */
@Composable
fun LevelHistoryScreen(
    viewModel: LevelHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    val historyList by viewModel.history.collectAsState()
    val loaded = historyList
    if (loaded == null) {
        Box(modifier.fillMaxSize().background(PqColors.Ground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PqColors.Ink, modifier = Modifier.size(36.dp))
        }
    } else {
        LevelHistoryScreenContent(history = loaded, modifier = modifier, onNavigateBack = onNavigateBack)
    }
}

@Composable
fun LevelHistoryScreenContent(
    history: List<LevelHistoryEntity>,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp),
    ) {
        PqTopBar("Level history", onBack = onNavigateBack)
        if (history.isEmpty()) {
            PqCard(radius = PqDimens.RadiusLg) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    CategoryTile(Icons.Rounded.EmojiEvents, PqColors.Gold)
                    Text(text = stringResource(R.string.pq_level_history_empty), style = PqType.Body.copy(color = PqColors.Ink2))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .comic(radius = 12.dp, border = PqDimens.BorderThin, shadow = 0.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Box(Modifier.size(40.dp).comic(bg = PqColors.Gold, radius = 20.dp, border = PqDimens.BorderThin, shadow = 0.dp), contentAlignment = Alignment.Center) {
                            Text(text = entry.level.toString(), style = PqType.number(20))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.pq_level_number, entry.level), style = PqType.BodyStrong)
                            Text(text = dateFormat.format(Date(entry.achievedDate)), style = PqType.Meta)
                        }
                        Text(text = entry.difficultyAtTimeOfLevelUp.lowercase().replaceFirstChar { it.uppercase() }, style = PqType.MetaStrong)
                    }
                }
            }
        }
    }
}
