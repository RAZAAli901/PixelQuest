package com.pixelquest.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.CoachCard
import com.pixelquest.app.ui.components.CoachUi
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.screens.insight.AiInsightErrorState
import com.pixelquest.app.ui.screens.insight.AiInsightLoadingState
import com.pixelquest.app.ui.screens.insight.AiInsightNotEnoughDataState
import com.pixelquest.app.ui.screens.insight.AiInsightRateLimitedState
import com.pixelquest.app.ui.screens.insight.AiInsightUiState
import com.pixelquest.app.ui.screens.insight.AiInsightViewModel
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.pqShape
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Home's coach card and teaser, for tests. */
object CoachTags {
    const val CARD = "home_coach_card"
    const val TEASER = "home_ai_teaser"
}

/**
 * The coach on Home (mock 4A): with a brief, one Coach Intel card (the situation, when it was written
 * and when the next one comes, and the way into the coach); before AI is on, or signed out, the AI
 * teaser. Its other states (loading, waiting, not enough days yet, a problem) say so as before.
 */
@Composable
fun TodayAiInsightSection(
    onNavigateToAiInsight: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToAccount: () -> Unit = onNavigateToSettings,
    viewModel: AiInsightViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSimpleMode by viewModel.isSimpleMode.collectAsState()
    val themeMode = PixelTheme.mode

    Box(modifier = modifier.fillMaxWidth()) {
        when (val state = uiState) {
            is AiInsightUiState.Success -> CoachCard(
                coach = coachUiOf(state),
                // The whole brief is the coach's own screen.
                onOpen = onNavigateToAiInsight,
                onExpand = onNavigateToAiInsight,
                title = stringResource(if (isSimpleMode) R.string.pq_coach_title_simple else R.string.pq_coach_title),
                modifier = Modifier.testTag(CoachTags.CARD)
            )
            is AiInsightUiState.Loading -> AiInsightLoadingState(themeMode = themeMode)
            is AiInsightUiState.RateLimited -> AiInsightRateLimitedState(state = state, themeMode = themeMode, onRefresh = { viewModel.refreshInsight() })
            is AiInsightUiState.NotEnoughData -> AiInsightNotEnoughDataState(state = state, themeMode = themeMode)
            // No teaser in a build that can't reach Gemini: the opt-in isn't offered there.
            is AiInsightUiState.Disabled -> if (com.pixelquest.app.domain.ai.AiAvailability.inThisBuild) {
                AiTeaser(isSimpleMode = isSimpleMode, needsSignIn = false, onUnlock = onNavigateToSettings)
            }
            is AiInsightUiState.SignInRequired -> if (com.pixelquest.app.domain.ai.AiAvailability.inThisBuild) {
                AiTeaser(isSimpleMode = isSimpleMode, needsSignIn = true, onUnlock = onNavigateToAccount)
            }
            is AiInsightUiState.CapReached -> com.pixelquest.app.ui.screens.insight.AiInsightCapReachedState(state = state, themeMode = themeMode)
            is AiInsightUiState.Error -> AiInsightErrorState(state = state, themeMode = themeMode, onRetry = { viewModel.refreshInsight() })
        }
    }
}

/** The card's words from the brief: live when just written, its summary, its time, and the wait for the next. */
fun coachUiOf(state: AiInsightUiState.Success, zone: ZoneId = ZoneId.systemDefault()): CoachUi = CoachUi(
    live = !state.isCached,
    situation = state.insight.summary,
    updatedAt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(Instant.ofEpochMilli(state.insight.generatedAt)),
    nextBriefIn = waitText(state.remainingCooldownSeconds)
)

/** "~6h", "~25m", or null once a new brief can be asked for. */
fun waitText(seconds: Long): String? = when {
    seconds <= 0 -> null
    seconds >= 3600 -> "~${(seconds + 1800) / 3600}h"
    else -> "~${((seconds + 59) / 60).coerceAtLeast(1)}m"
}

/**
 * Before the AI coach is on (mock 4A): an amber line that says what it is and how to get it, signing
 * in first if the player isn't, and one button to it.
 */
@Composable
fun AiTeaser(isSimpleMode: Boolean, needsSignIn: Boolean, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val shape = pqShape(PqDimens.Radius)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(CoachTags.TEASER)
            .clip(shape)
            .background(PqColors.Amber)
            .border(PqDimens.Border, PqColors.Ink, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(Modifier.size(40.dp).clip(pqShape(10.dp)).background(PqColors.Ink), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = PqColors.Gold, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(if (isSimpleMode) R.string.pq_ai_teaser_title_simple else R.string.pq_ai_teaser_title),
                style = PqType.BodyStrong.copy(fontSize = 14.bsp)
            )
            Text(
                text = stringResource(if (needsSignIn) R.string.pq_ai_teaser_sign_in else R.string.pq_ai_teaser_settings),
                style = PqType.Meta.copy(color = PqColors.Ink)
            )
        }
        PqButton(stringResource(R.string.pq_ai_teaser_unlock), onUnlock)
    }
}
