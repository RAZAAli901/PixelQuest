package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 16: AiInsightScreen scaffolding with native theme-dispatching UI architecture.
 * Evaluates the active [ThemeMode] (Pixel, Light, or Comic) and routes presentation
 * through dedicated theme renderers.
 */
@Composable
fun AiInsightScreen(
    modifier: Modifier = Modifier,
    viewModel: AiInsightViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSimpleMode by viewModel.isSimpleMode.collectAsState()

    AiInsightScreenContent(
        modifier = modifier,
        uiState = uiState,
        isSimpleMode = isSimpleMode,
        onRefresh = { viewModel.refreshInsight() },
        onNavigateBack = onNavigateBack,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
fun AiInsightScreenContent(
    modifier: Modifier = Modifier,
    uiState: AiInsightUiState,
    isSimpleMode: Boolean = false,
    onRefresh: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val themeMode = PixelTheme.mode
    val colors = PixelTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PixelTopAppBar(
                title = when {
                    isSimpleMode -> "HABIT COACH"
                    themeMode == ThemeMode.Comic -> "AI QUESTMASTER"
                    themeMode == ThemeMode.Light -> "Habit Insights"
                    else -> "AI COACH"
                },
                navigationIcon = if (onNavigateBack != null) {
                    {
                        androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                            com.pixelquest.app.ui.components.SymbolIcon(
                                symbol = "◀",
                                contentDescription = "Back",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                color = colors.primaryText
                            )
                        }
                    }
                } else null
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            when (themeMode) {
                ThemeMode.Comic -> {
                    AiInsightComicDispatch(
                        uiState = uiState,
                        onRefresh = onRefresh,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
                ThemeMode.Light -> {
                    AiInsightLightDispatch(
                        uiState = uiState,
                        onRefresh = onRefresh,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
                else -> {
                    AiInsightPixelDispatch(
                        uiState = uiState,
                        onRefresh = onRefresh,
                        onNavigateToSettings = onNavigateToSettings,
                        isSimpleMode = isSimpleMode
                    )
                }
            }
        }
    }
}

@Composable
fun AiInsightPixelDispatch(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?,
    isSimpleMode: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelAiInsightView(
            uiState = uiState,
            onRefresh = onRefresh,
            onNavigateToSettings = onNavigateToSettings,
            isSimpleMode = isSimpleMode
        )
    }
}

@Composable
fun AiInsightLightDispatch(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightAiInsightView(
            uiState = uiState,
            onRefresh = onRefresh,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

@Composable
fun AiInsightComicDispatch(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ComicAiInsightView(
            uiState = uiState,
            onRefresh = onRefresh,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

@Composable
fun AiInsightStateRouter(
    uiState: AiInsightUiState,
    themeMode: ThemeMode,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?
) {
    when (uiState) {
        is AiInsightUiState.Loading -> {
            AiInsightLoadingState(
                themeMode = themeMode
            )
        }
        is AiInsightUiState.Success -> {
            AiInsightCard(
                insight = uiState.insight,
                isCached = uiState.isCached,
                canRefresh = uiState.canRefresh,
                remainingCooldownSeconds = uiState.remainingCooldownSeconds,
                onRefresh = onRefresh
            )
        }
        is AiInsightUiState.RateLimited -> {
            AiInsightRateLimitedState(
                state = uiState,
                themeMode = themeMode,
                onRefresh = onRefresh
            )
        }
        is AiInsightUiState.NotEnoughData -> {
            AiInsightNotEnoughDataState(
                state = uiState,
                themeMode = themeMode
            )
        }
        is AiInsightUiState.Disabled -> {
            AiInsightDisabledState(
                state = uiState,
                themeMode = themeMode,
                onNavigateToSettings = onNavigateToSettings
            )
        }
        is AiInsightUiState.CapReached -> {
            AiInsightCapReachedState(
                state = uiState,
                themeMode = themeMode
            )
        }
        is AiInsightUiState.Error -> {
            AiInsightErrorState(
                state = uiState,
                themeMode = themeMode,
                onRetry = onRefresh
            )
        }
    }
}
