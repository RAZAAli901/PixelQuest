package com.pixelquest.ui.screens

import com.pixelquest.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.ui.components.*
import com.pixelquest.ui.theme.PixelQuestTheme
import com.pixelquest.ui.theme.PqColors
import com.pixelquest.ui.theme.PqDimens
import com.pixelquest.ui.theme.PqType

enum class QuestTab(val label: String) { Today("Today"), All("All quests"), History("History") }

data class QuestsUiState(
    val tab: QuestTab,
    val todayCount: Int,
    val groups: List<QuestGroupUi>,   // e.g. "Morning · 2 done", "Evening · 2 left"
)

/** Replaces the old Quest Log + the drawer's Quest History. Swipe-to-edit can wrap QuestRow in SwipeToDismissBox. */
@Composable
fun QuestsScreen(
    state: QuestsUiState,
    onTabChange: (QuestTab) -> Unit,
    onToggleQuest: (QuestUi) -> Unit,
    onQuestClick: (QuestUi) -> Unit,
    onNewQuest: () -> Unit,
    onSearch: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Box(Modifier.fillMaxSize().background(PqColors.Ground)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PqTopBar("Quests") { PqIconButton(Icons.Rounded.Search, "Search", onSearch) } }
            item {
                val labels = QuestTab.entries.map { if (it == QuestTab.Today) "Today · ${state.todayCount}" else it.label }
                PqSegmented(labels, state.tab.ordinal, { onTabChange(QuestTab.entries[it]) })
            }
            state.groups.forEach { g ->
                item(key = "h_${g.title}") { PqGroupLabel(g.title) }
                items(g.quests, key = { it.id }) { q -> QuestRow(q, onToggle = { onToggleQuest(q) }, onClick = { onQuestClick(q) }) }
            }
        }
        // Extended FAB — says what it does
        Row(
            Modifier.align(Alignment.BottomEnd)
                .padding(end = PqDimens.ScreenPad, bottom = contentPadding.calculateBottomPadding() + 18.dp)
                .height(52.dp)
                .comicPressable(onNewQuest, bg = PqColors.Red, radius = 16.dp, shadow = PqDimens.ShadowLg)
                .padding(start = 14.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = PqColors.OnRed, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
            Text("NEW QUEST", style = PqType.Button.copy(color = PqColors.OnRed))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun QuestsPreview() = PixelQuestTheme {
    QuestsScreen(
        QuestsUiState(
            QuestTab.Today, 4,
            listOf(
                QuestGroupUi("Morning · 2 done", listOf(
                    QuestUi("a", "20 Push-ups", "08:00", QuestCategory.Fitness, done = true),
                    QuestUi("b", "Ring_Test", "11:18", QuestCategory.Fitness, done = true),
                )),
                QuestGroupUi("Evening · 2 left", listOf(
                    QuestUi("c", "Go to Gym", "17:30", QuestCategory.Fitness, overdue = true),
                    QuestUi("d", "Read 15 Pages", "21:00", QuestCategory.Learning),
                )),
            ),
        ),
        {}, {}, {}, {}, {},
    )
}
