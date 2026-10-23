package com.pixelquest.ui.screens

import com.pixelquest.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.ui.components.*
import com.pixelquest.ui.theme.PixelQuestTheme
import com.pixelquest.ui.theme.PqColors
import com.pixelquest.ui.theme.PqDimens
import com.pixelquest.ui.theme.PqType

data class HomeUiState(
    val greeting: String,
    val heroName: String,
    val level: Int,
    val avatar: AvatarClass,
    val streakDays: Int,
    val doneCount: Int,
    val totalCount: Int,
    val targetPercent: Int,           // difficulty threshold, e.g. 70
    val xpToday: Int,
    val nextPrayer: NextPrayerUi?,    // null when all five are prayed
    val prayedToday: Int,
    val nextFajr: String? = null,
    val upNext: List<QuestUi>,
    val doneToday: List<QuestUi>,
    val aiUnlocked: Boolean,
    val coach: CoachUi? = null,       // shown once AI Questmaster is unlocked
) {
    val progress: Float get() = if (totalCount == 0) 0f else doneCount / totalCount.toFloat()
    val isPerfectDay: Boolean get() = totalCount > 0 && doneCount == totalCount
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleQuest: (QuestUi) -> Unit,
    onQuestClick: (QuestUi) -> Unit,
    onSeeAllQuests: () -> Unit,
    onPrayerClick: () -> Unit,
    onUnlockAi: () -> Unit,
    onAvatarClick: () -> Unit,
    onParties: () -> Unit = {},
    onOpenCoach: () -> Unit = {},
    onExpandCoach: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        Modifier.fillMaxSize().background(PqColors.Ground),
        contentPadding = PaddingValues(
            start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HomeHeader(state, onAvatarClick, onParties) }
        item { if (state.isPerfectDay) PerfectDayCard(state) else ProgressCard(state) }
        state.coach?.let { c -> item { CoachCard(c, onOpen = onOpenCoach, onExpand = onExpandCoach) } }
        item { PrayerCard(state, onPrayerClick) }
        if (!state.isPerfectDay) {
            item { PqSectionHeader("Up next", action = "See all", onAction = onSeeAllQuests) }
            items(state.upNext, key = { it.id }) { q -> QuestRow(q, onToggle = { onToggleQuest(q) }, onClick = { onQuestClick(q) }) }
            if (!state.aiUnlocked) item { AiTeaser(onUnlockAi) }
        } else {
            item { PqSectionHeader("Done today · ${state.doneToday.size}") }
            items(state.doneToday, key = { it.id }) { q -> QuestRowCompact(q) }
        }
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, onAvatarClick: () -> Unit, onParties: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PixelAvatar(state.avatar, 46.dp, Modifier.clickable(onClick = onAvatarClick))
        Column(Modifier.weight(1f)) {
            Text(state.greeting, style = PqType.Meta.copy(fontSize = 12.bsp))
            Text("${state.heroName} · LVL ${state.level}", style = PqType.BodyStrong.copy(fontSize = 16.bsp))
        }
        PqIconButton(Icons.Rounded.Groups, "Parties", onParties)
        PqChip("${state.streakDays}", PqColors.Gold, icon = Icons.Rounded.LocalFireDepartment, iconTint = PqColors.Flame)
    }
}

@Composable
private fun ProgressCard(state: HomeUiState) {
    PqCard(shadow = PqDimens.ShadowLg) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("QUEST PROGRESS", style = PqType.CardTitle)
                Text("${state.doneCount} of ${state.totalCount} done · target ${state.targetPercent}%", style = PqType.Body.copy(fontSize = 13.bsp, color = PqColors.Ink2))
                PqProgressBar(state.progress, target = state.targetPercent / 100f)
                Text("+${state.xpToday} XP today", style = PqType.MetaStrong.copy(color = PqColors.RedText))
            }
            ProgressRing(state.progress, "${(state.progress * 100).toInt()}%")
        }
    }
}

@Composable
private fun PerfectDayCard(state: HomeUiState) {
    PqCard(bg = PqColors.Gold, shadow = PqDimens.ShadowLg) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ALL ${state.totalCount} QUESTS DONE", style = PqType.Label)
            Text(
                "PERFECT DAY!",
                style = PqType.Hero.copy(color = PqColors.Red, shadow = Shadow(PqColors.Ink, Offset(6f, 6f), 0f)),
            )
            Text("Streak protected. Every perfect day moves you toward LVL ${state.level + 1}.", style = PqType.Body)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PqChip("+${state.xpToday} XP", PqColors.Paper)
                PqChip("${state.streakDays} day streak", PqColors.Paper, icon = Icons.Rounded.LocalFireDepartment, iconTint = PqColors.Flame)
            }
        }
    }
}

@Composable
private fun PrayerCard(state: HomeUiState, onClick: () -> Unit) {
    PqCard(bg = PqColors.Sky, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryTile(Icons.Rounded.Bedtime, PqColors.Paper)
            Column(Modifier.weight(1f)) {
                val p = state.nextPrayer
                Text(if (p != null) "NEXT PRAYER · ${state.prayedToday}/5 TODAY" else "PRAYERS · 5/5 TODAY", style = PqType.Label)
                Text(if (p != null) "${p.name} · ${p.time}" else "All five prayed", style = PqType.BodyStrong.copy(fontSize = 16.bsp))
                Text(if (p != null) p.countdown else "Next: Fajr ${state.nextFajr.orEmpty()}", style = PqType.MetaStrong)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = PqColors.Ink)
        }
    }
}

@Composable
private fun AiTeaser(onUnlock: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(pqShape(PqDimens.Radius)).background(PqColors.Amber)
            .border(PqDimens.Border, PqColors.Ink, pqShape(PqDimens.Radius)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(pqShape(10.dp)).background(PqColors.Ink), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = PqColors.Gold, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("AI Questmaster", style = PqType.BodyStrong.copy(fontSize = 14.bsp))
            Text("Sign in for daily debriefs", style = PqType.Meta.copy(color = PqColors.Ink))
        }
        PqButton("Unlock", onUnlock, height = 36.dp)
    }
}

// ---------- Preview ----------

private val sampleQuests = listOf(
    QuestUi("1", "Go to Gym", "17:30", QuestCategory.Fitness, overdue = true),
    QuestUi("2", "Read 15 Pages", "21:00", QuestCategory.Learning),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun HomePreview() = PixelQuestTheme {
    HomeScreen(
        HomeUiState(
            "Good evening", "CBF", 1, AvatarClass.Hero, 3, 2, 4, 70, 40,
            NextPrayerUi("Isha", "6:46 PM", "in 36 min"), 4, upNext = sampleQuests, doneToday = emptyList(), aiUnlocked = false,
        ),
        {}, {}, {}, {}, {}, {},
    )
}
