package com.pixelquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelAvatar
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqChip
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqProgressBar
import com.pixelquest.app.ui.components.ProgressRing
import com.pixelquest.app.ui.components.QuestRow
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.motion.ConfettiFall
import com.pixelquest.app.ui.motion.PerfectDayStamp
import com.pixelquest.app.ui.motion.RollingNumber
import com.pixelquest.app.ui.motion.StreakCounter
import com.pixelquest.app.ui.motion.XpGainChip
import com.pixelquest.app.ui.motion.thud
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.pqShape

/** Asks before skipping [quest] for today: it counts as not done, but can still be done later today. */
@Composable
internal fun SkipQuestDialog(quest: QuestUi, isSimpleMode: Boolean, onSkip: () -> Unit, onDismiss: () -> Unit) {
    PqConfirmDialog(
        title = stringResource(if (isSimpleMode) R.string.pq_skip_title_simple else R.string.pq_skip_title),
        message = stringResource(R.string.pq_skip_message, quest.title),
        confirm = stringResource(R.string.pq_skip_confirm),
        onConfirm = onSkip,
        onDismiss = onDismiss,
    )
}

/**
 * A quest still to do today (Home, and Quests' Today): its circle completes it; swiped left, or
 * with TalkBack's action, it asks to skip it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SkippableQuestRow(quest: QuestUi, tag: String, onComplete: () -> Unit, onSkip: () -> Unit, onClick: () -> Unit) {
    val skipLabel = stringResource(R.string.pq_skip_action)
    val swipe = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart) onSkip()
        false // the row stays: skipping asks first
    })
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(pqShape(PqDimens.Radius)).background(PqColors.Rose).padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Text(skipLabel.uppercase(), style = PqType.Button) }
        },
        modifier = Modifier,
    ) {
        QuestRow(
            quest = quest,
            onToggle = { if (!quest.done) onComplete() },
            onClick = onClick,
            modifier = Modifier
                .testTag(tag)
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(skipLabel) {
                            onSkip()
                            true
                        },
                    )
                },
        )
    }
}

@Composable
internal fun HomeHeader(state: HomeUiState, greeting: String, onAvatarClick: () -> Unit, onParties: () -> Unit, partiesDot: Boolean) {
    val mascotMood = when {
        state.isPerfectDay -> com.pixelquest.app.mascot.PixMood.Celebrate
        state.isStreakBroken -> com.pixelquest.app.mascot.PixMood.Sad
        state.streakDays >= 7 -> com.pixelquest.app.mascot.PixMood.OnFire
        state.progress >= 0.5f -> com.pixelquest.app.mascot.PixMood.Happy
        else -> com.pixelquest.app.mascot.PixMood.Focus
    }
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PixelAvatar(state.avatar, 46.dp, Modifier.comicPressable(onAvatarClick, radius = 8.dp, shadow = 0.dp).testTag(HomeTags.AVATAR))
        Column(Modifier.weight(1f)) {
            Text(greeting, style = PqType.Meta.copy(fontSize = 12.bsp))
            Text(
                if (state.isSimpleMode) state.heroName else stringResource(R.string.pq_home_name_level, state.heroName, state.level),
                style = PqType.BodyStrong.copy(fontSize = 16.bsp),
                maxLines = 1,
            )
        }
        com.pixelquest.app.mascot.PqMascot(mascotMood, size = 44.dp)
        Box {
            val parties = stringResource(R.string.pq_home_parties)
            val waiting = stringResource(R.string.pq_nav_something_waiting)
            PqIconButton(Icons.Rounded.Groups, if (partiesDot) "$parties, $waiting" else parties, onParties, Modifier.testTag(HomeTags.PARTIES))
            if (partiesDot) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(12.dp).clip(PqCircle)
                        .background(PqColors.Red).border(PqDimens.BorderThin, PqColors.Ink, PqCircle),
                )
            }
        }
        if (!state.isSimpleMode) {
            val streak = pluralStringResource(R.plurals.pq_home_streak_days, state.streakDays, state.streakDays)
            // M05: the flame flickers, and the chip pops with a rolling number when the streak grows.
            StreakCounter(state.streakDays, Modifier.height(30.dp).clearAndSetSemantics { contentDescription = streak })
        }
    }
}

@Composable
internal fun ProgressCard(state: HomeUiState) {
    PqCard(Modifier.testTag(HomeTags.PROGRESS), shadow = PqDimens.ShadowLg) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(if (state.isSimpleMode) R.string.pq_home_progress_simple else R.string.pq_home_progress), style = PqType.CardTitle)
                Text(
                    stringResource(R.string.pq_home_progress_line, state.doneCount, state.totalCount, state.targetPercent),
                    style = PqType.Body.copy(fontSize = 13.bsp, color = PqColors.Ink2),
                )
                // M01: when XP comes in, "+N XP" pops by the bar and today's total rolls up.
                var lastXp by remember { mutableIntStateOf(state.xpToday) }
                var gained by remember { mutableIntStateOf(0) }
                var gains by remember { mutableIntStateOf(0) }
                LaunchedEffect(state.xpToday) {
                    if (state.xpToday > lastXp) {
                        gained = state.xpToday - lastXp
                        gains++
                    }
                    lastXp = state.xpToday
                }
                Box {
                    PqProgressBar(state.progress, target = state.targetPercent / 100f)
                    if (!state.isSimpleMode) XpGainChip(gained, gains, Modifier.align(Alignment.CenterEnd))
                }
                if (!state.isSimpleMode) {
                    val context = LocalContext.current
                    RollingNumber(state.xpToday, PqType.MetaStrong.copy(color = PqColors.RedText), format = { context.getString(R.string.pq_home_xp_today, it) })
                }
            }
            ProgressRing(state.progress, stringResource(R.string.pq_percent, (state.progress * 100).toInt()))
        }
    }
}

@Composable
internal fun PerfectDayCard(state: HomeUiState, celebrate: Int) {
    // M08: "PERFECT!" stamps onto the card (after the XP chip, so the two don't overlap), the card
    // thuds, and confetti falls. A day that was already perfect just shows the stamp.
    var stamped by remember { mutableStateOf(celebrate == 0) }
    LaunchedEffect(celebrate) {
        if (celebrate > 0) {
            stamped = false
            kotlinx.coroutines.delay(700)
            stamped = true
        }
    }
    Box {
        PqCard(Modifier.testTag(HomeTags.PERFECT).thud(if (stamped) celebrate else 0), bg = PqColors.Gold, shadow = PqDimens.ShadowLg) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (state.doneCount == state.totalCount) {
                            pluralStringResource(if (state.isSimpleMode) R.plurals.pq_home_all_done_simple else R.plurals.pq_home_all_done, state.totalCount, state.totalCount)
                        } else {
                            stringResource(R.string.pq_home_some_done, state.doneCount, state.totalCount)
                        },
                        style = PqType.Label,
                    )
                    Text(
                        stringResource(R.string.pq_home_perfect_day),
                        style = PqType.Hero.copy(color = PqColors.Red, shadow = Shadow(PqColors.Ink, Offset(6f, 6f), 0f)),
                    )
                    Text(
                        if (state.isSimpleMode) stringResource(R.string.pq_home_perfect_body_simple) else stringResource(R.string.pq_home_perfect_body, state.level + 1),
                        style = PqType.Body,
                    )
                    if (!state.isSimpleMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PqChip(stringResource(R.string.pq_xp_earned, state.xpToday), PqColors.Paper)
                            PqChip(
                                pluralStringResource(R.plurals.pq_home_streak_days, state.streakDays, state.streakDays),
                                PqColors.Paper,
                                icon = Icons.Rounded.LocalFireDepartment,
                                iconTint = PqColors.Flame,
                            )
                        }
                    }
                }
                com.pixelquest.app.mascot.PqMascot(com.pixelquest.app.mascot.PixMood.Celebrate, size = 76.dp)
            }
        }
        PerfectDayStamp(stamped, Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp).clearAndSetSemantics { })
        if (stamped) ConfettiFall(celebrate, Modifier.matchParentSize())
    }
}

/** No quests today: what Home is for, and the screen's one red button. */
@Composable
internal fun EmptyDayCard(isSimpleMode: Boolean, onNewQuest: () -> Unit) {
    PqCard(Modifier.testTag(HomeTags.EMPTY), shadow = PqDimens.ShadowLg) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            com.pixelquest.app.mascot.PqMascot(com.pixelquest.app.mascot.PixMood.Sleepy, size = 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(if (isSimpleMode) R.string.pq_home_empty_title_simple else R.string.pq_home_empty_title), style = PqType.CardTitle)
                Text(stringResource(R.string.pq_home_empty_body), style = PqType.Body.copy(color = PqColors.Ink2))
                Spacer(Modifier.height(2.dp))
                PqButton(stringResource(if (isSimpleMode) R.string.pq_new_task else R.string.pq_new_quest), onNewQuest, Modifier.fillMaxWidth())
            }
        }
    }
}
