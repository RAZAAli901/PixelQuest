package com.pixelquest.app.ui.screens.insight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.domain.ai.AiErrorCopy
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqProgressBar
import com.pixelquest.app.ui.components.PqTag
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The AI coach's own screen, in UI v2's language (docs/ui-v2.md): its top bar with a fresh brief
 * when one is allowed, the latest brief as one Coach Intel card (a preview first, the rest behind
 * Show more), the earlier briefs under it, and in the coach's other states a card that says why
 * there's no brief and what to do.
 */
@Composable
fun AiInsightScreen(
    modifier: Modifier = Modifier,
    viewModel: AiInsightViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToAccount: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSimpleMode by viewModel.isSimpleMode.collectAsState()
    val displayState by viewModel.displayState.collectAsState()
    val olderInsights by viewModel.olderInsights.collectAsState()

    AiInsightScreenContent(
        modifier = modifier,
        uiState = uiState,
        isSimpleMode = isSimpleMode,
        displayState = displayState,
        onRefresh = { viewModel.refreshInsight() },
        onToggleLatestExpanded = viewModel::toggleLatestExpanded,
        olderInsights = olderInsights,
        onToggleOlder = viewModel::toggleOlder,
        onNavigateBack = onNavigateBack,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAccount = onNavigateToAccount
    )
}

@Composable
fun AiInsightScreenContent(
    modifier: Modifier = Modifier,
    uiState: AiInsightUiState,
    isSimpleMode: Boolean = false,
    /** Compact or full, and what's expanded; the screen's tests pass their own. */
    displayState: CoachDisplayState = CoachDisplayState(),
    onRefresh: () -> Unit,
    onToggleLatestExpanded: () -> Unit = {},
    olderInsights: List<OlderInsight> = emptyList(),
    onToggleOlder: (Long) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToAccount: (() -> Unit)? = null
) {
    val sound = LocalSoundManager.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 32.dp)
    ) {
        PqTopBar(if (isSimpleMode) "Habit coach" else "AI coach", onBack = onNavigateBack) {
            // A fresh brief is asked for here, once there's one to replace.
            (uiState as? AiInsightUiState.Success)?.let { success ->
                CoachRefreshButton(success.canRefresh, success.remainingCooldownSeconds) {
                    sound?.playClickSound()
                    onRefresh()
                }
            }
        }
        when (uiState) {
            is AiInsightUiState.Success -> {
                CoachBrief(
                    state = uiState,
                    title = stringResource(if (isSimpleMode) R.string.pq_coach_title_simple else R.string.pq_coach_title),
                    expanded = !displayState.compact || displayState.latestExpanded,
                    onToggleExpanded = if (displayState.compact) onToggleLatestExpanded else null
                )
                EarlierBriefs(olderInsights, displayState.expandedOlderId, onToggleOlder)
            }
            is AiInsightUiState.SignInRequired -> AiInsightSignInRequiredState(uiState, onNavigateToAccount)
            is AiInsightUiState.Disabled -> AiInsightDisabledState(uiState, onNavigateToSettings)
            is AiInsightUiState.Loading -> AiInsightLoadingState()
            is AiInsightUiState.RateLimited -> AiInsightRateLimitedState(uiState, onRefresh)
            is AiInsightUiState.NotEnoughData -> AiInsightNotEnoughDataState(uiState)
            is AiInsightUiState.CapReached -> AiInsightCapReachedState(uiState)
            is AiInsightUiState.Error -> AiInsightErrorState(uiState, onRefresh)
        }
    }
}

/** "New", or "3h" while the 6-hour wait runs: a fresh brief, asked for from the top bar. */
@Composable
fun CoachRefreshButton(enabled: Boolean, remainingSeconds: Long, onClick: () -> Unit) {
    val hours = (remainingSeconds + 3599) / 3600
    val ready = enabled || hours <= 0L
    val description = if (enabled) {
        stringResource(R.string.coach_refresh_description)
    } else {
        LocalContext.current.resources.getQuantityString(R.plurals.coach_cooldown_description, hours.toInt(), hours.toInt())
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .comicPressable(onClick, bg = if (ready) PqColors.Gold else PqColors.Sunken, radius = 12.dp, shadow = if (ready) 2.dp else 0.dp, enabled = enabled, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 10.dp)
    ) {
        Icon(if (ready) Icons.Rounded.Refresh else Icons.Rounded.HourglassTop, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(18.dp))
        Text(
            text = (if (ready) stringResource(R.string.coach_refresh) else stringResource(R.string.coach_cooldown_hours, hours)).uppercase(),
            style = PqType.Button
        )
    }
}

/**
 * The latest brief, as Coach Intel: the situation always, and with it expanded, the move to make and
 * a word of encouragement. Live when just written; when it was written at the foot.
 */
@Composable
private fun CoachBrief(state: AiInsightUiState.Success, title: String, expanded: Boolean, onToggleExpanded: (() -> Unit)?) {
    val insight = state.insight
    val preview = onToggleExpanded != null && !expanded
    PqCard(shadow = PqDimens.ShadowLg) {
        Column(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(36.dp).clip(pqShape(10.dp)).background(PqColors.Ink), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = PqColors.Gold, modifier = Modifier.size(20.dp))
                    }
                    Text(text = title, style = PqType.CardTitle, modifier = Modifier.weight(1f).semantics { heading() })
                    if (!state.isCached) PqTag(stringResource(R.string.pq_coach_live), color = PqColors.Live)
                }
                BriefSection(stringResource(R.string.pq_coach_situation), insight.summary, maxLines = if (preview) PREVIEW_LINES else Int.MAX_VALUE)
                Expandable(visible = !preview) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BriefSection("The move", insight.suggestion)
                        BriefSection("Keep going", insight.encouragement)
                    }
                }
                if (onToggleExpanded != null) ExpandToggle(expanded, onToggleExpanded)
            }
            Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
            Text(
                text = (if (state.isCached) "Saved brief" else "New brief") + " · " + dateOf(insight.generatedAt),
                style = PqType.Meta,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

/** A brief's lines are shortened to this many while it's a preview. */
private const val PREVIEW_LINES = 4

@Composable
private fun BriefSection(label: String, body: String, maxLines: Int = Int.MAX_VALUE) {
    if (body.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label.uppercase(), style = PqType.Label.copy(color = PqColors.Muted))
        Text(text = body, style = PqType.Body, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

/** Show more / Show less under a previewed brief. */
@Composable
private fun ExpandToggle(expanded: Boolean, onToggle: () -> Unit) {
    val label = stringResource(if (expanded) R.string.coach_collapse else R.string.coach_expand)
    Text(
        text = label.uppercase() + if (expanded) " ▴" else " ▾",
        style = PqType.MetaStrong.copy(color = PqColors.RedText),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onToggle)
            .padding(vertical = 10.dp)
    )
}

/** What only shows when expanded: it slides open, unless the player asked for less motion. */
@Composable
private fun Expandable(visible: Boolean, content: @Composable () -> Unit) {
    val still = LocalReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = if (still) EnterTransition.None else expandVertically() + fadeIn(),
        exit = if (still) ExitTransition.None else shrinkVertically() + fadeOut()
    ) { content() }
}

/** The briefs before the latest: their day and first line, one opened at a time. */
@Composable
private fun EarlierBriefs(items: List<OlderInsight>, expandedId: Long?, onToggle: (Long) -> Unit) {
    if (items.isEmpty()) return
    PqGroupLabel(stringResource(R.string.coach_earlier), Modifier.semantics { heading() })
    items.forEach { item ->
        val open = item.id == expandedId
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .comicPressable({ onToggle(item.id) }, radius = PqDimens.Radius, shadow = 0.dp, onClickLabel = stringResource(if (open) R.string.coach_collapse else R.string.coach_expand), role = Role.Button)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(text = dateOf(item.insight.generatedAt), style = PqType.MetaStrong)
            if (open) {
                listOf(item.insight.summary, item.insight.suggestion, item.insight.encouragement).filter { it.isNotBlank() }
                    .forEach { Text(text = it, style = PqType.Body) }
            } else {
                Text(text = item.insight.summary, style = PqType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** "Oct 9, 8:48 AM", or "recently" for a brief without a time. */
@Composable
private fun dateOf(timestamp: Long): String = remember(timestamp) {
    if (timestamp <= 0L) "recently" else SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
}

// ---------- The coach's other states ----------

/**
 * Why there's no brief, and what to do: an icon on its colour, a heading, the reason, and a button
 * where there's something to do.
 */
@Composable
private fun CoachStateCard(
    icon: ImageVector,
    tint: Color,
    heading: String,
    body: String,
    button: String? = null,
    onClick: (() -> Unit)? = null,
    extra: @Composable () -> Unit = {}
) {
    PqCard(radius = PqDimens.RadiusLg) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(pqShape(10.dp)).background(tint), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
                }
                Text(text = heading, style = PqType.BodyStrong, modifier = Modifier.weight(1f).semantics { heading() })
            }
            Text(text = body, style = PqType.Body.copy(color = PqColors.Ink2))
            extra()
            if (button != null && onClick != null) PqButton(button, onClick, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AiInsightLoadingState() {
    PqCard(radius = PqDimens.RadiusLg) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            CircularProgressIndicator(color = PqColors.Ink, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            Column {
                Text(text = "Writing your brief…", style = PqType.BodyStrong)
                Text(text = "Reading your week of quests.", style = PqType.Meta)
            }
        }
    }
}

@Composable
fun AiInsightRateLimitedState(state: AiInsightUiState.RateLimited, onRefresh: () -> Unit) {
    val hours = (state.retryAfterSeconds + 3599) / 3600
    CoachStateCard(Icons.Rounded.HourglassTop, PqColors.Sky, "The next brief is on its way", state.message) {
        Text(text = "Next one in about $hours hour${if (hours == 1L) "" else "s"}", style = PqType.MetaStrong)
    }
}

@Composable
fun AiInsightNotEnoughDataState(state: AiInsightUiState.NotEnoughData) {
    CoachStateCard(Icons.Rounded.Timeline, PqColors.Lilac, "A few more days first", state.message) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PqProgressBar(state.progressRatio)
            Text(text = "${state.daysLogged} / ${state.minimumRequiredDays} days logged", style = PqType.MetaStrong)
            Text(text = state.encouragingTip, style = PqType.Meta)
        }
    }
}

@Composable
fun AiInsightCapReachedState(state: AiInsightUiState.CapReached) {
    CoachStateCard(Icons.Rounded.Bolt, PqColors.Gold, if (state.isMonthly) "This month's briefs are used up" else "That's all for today", state.message)
}

/**
 * The error card's heading. A build without AI set up, or a used-up day, isn't a glitch, so those get
 * their own headings.
 */
internal fun aiErrorHeading(message: String): String = when (message) {
    AiErrorCopy.NOT_CONFIGURED -> "Coach not set up"
    AiErrorCopy.DAILY_LIMIT -> "That's all for today"
    else -> "Couldn't get a brief"
}

@Composable
fun AiInsightErrorState(state: AiInsightUiState.Error, onRetry: () -> Unit) {
    CoachStateCard(
        Icons.Rounded.CloudOff, PqColors.Rose, aiErrorHeading(state.message), state.message,
        button = if (state.canRetry && state.message != AiErrorCopy.NOT_CONFIGURED) "Try again" else null,
        onClick = onRetry
    )
}

@Composable
fun AiInsightDisabledState(state: AiInsightUiState.Disabled, onNavigateToSettings: (() -> Unit)?) {
    CoachStateCard(Icons.Rounded.Lock, PqColors.Amber, "The coach is off", state.message, button = "Turn on in Settings", onClick = onNavigateToSettings)
}

@Composable
fun AiInsightSignInRequiredState(state: AiInsightUiState.SignInRequired, onNavigateToAccount: (() -> Unit)?) {
    CoachStateCard(Icons.Rounded.Lock, PqColors.Amber, "Sign in to unlock the coach", state.message, button = "Sign in", onClick = onNavigateToAccount)
}
