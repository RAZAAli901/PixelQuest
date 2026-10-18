package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerSlot
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.haptics.PixelHaptics
import com.pixelquest.app.ui.theme.PixelTheme
import kotlinx.coroutines.launch

/** Test tags for the Namaz screen's parts. */
object NamazTestTags {
    const val SCREEN = "namaz_screen"
    const val SWITCH = "namaz_switch"
    const val EXPLAINER = "namaz_explainer"
    const val SETUP = "namaz_setup"
    const val NEXT = "namaz_next"
    const val COUNTDOWN = "namaz_countdown"
    const val LOCKED_TOOLTIP = "namaz_locked_tooltip"
    const val SNACKBAR = "namaz_snackbar"
    const val PROGRESS = "namaz_progress"

    /** A prayer's button. */
    fun prayer(prayer: Prayer) = "namaz_prayer_${prayer.name}"
}

/** The Namaz Tracker, opened from the side menu (docs/namaz-design.md). */
@Composable
fun NamazScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: NamazViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val setupVisible by viewModel.setupVisible.collectAsState()
    val locationLookup by viewModel.locationLookup.collectAsState()
    val locationRequest = rememberLocationPermissionRequest { granted ->
        if (granted) viewModel.useMyLocation() else viewModel.locationRefused()
    }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    NamazContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onEnabledChange = viewModel::setEnabled,
        onRefresh = viewModel::refresh,
        snackbarHostState = snackbarHostState,
        onPrayerClick = { slot ->
            when (PrayerButtonState.of(slot)) {
                // Missed can still be marked: it's prayed late.
                PrayerButtonState.PENDING, PrayerButtonState.MISSED -> {
                    PixelHaptics.performLightTap(haptic)
                    viewModel.markPrayed(slot.prayer)
                }
                // Taking a mark back is asked first, so a stray tap can't undo it.
                PrayerButtonState.PRAYED -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.namaz_undo_question, context.getString(slot.prayer.displayName)),
                        actionLabel = context.getString(R.string.namaz_undo_action),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(slot.prayer)
                }
                PrayerButtonState.LOCKED -> Unit
            }
        }
    )

    if (setupVisible) {
        NamazSetupSheet(
            settings = state.settings,
            place = state.place,
            locationLookup = locationLookup,
            onUseMyLocation = locationRequest::launch,
            onDone = viewModel::finishSetup,
            onDismiss = viewModel::dismissSetup
        )
    }
}

/**
 * The screen for [state]: the top bar with back and the title, the master switch, then the
 * tracker's sections. While the tracker is off, only a short explainer shows under the switch.
 */
@Composable
fun NamazContent(
    state: NamazUiState,
    onNavigateBack: (() -> Unit)? = null,
    onEnabledChange: (Boolean) -> Unit = {},
    onRefresh: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onPrayerClick: (PrayerSlot) -> Unit = {}
) {
    val colors = PixelTheme.colors
    val formatter = rememberPrayerTimeFormatter()
    Scaffold(
        containerColor = colors.background,
        topBar = { NamazTopBar(onNavigateBack = onNavigateBack) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                NamazSnackbar(
                    message = data.visuals.message,
                    actionLabel = data.visuals.actionLabel,
                    onAction = { data.performAction() },
                    modifier = Modifier.padding(16.dp).testTag(NamazTestTags.SNACKBAR)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding)
                .testTag(NamazTestTags.SCREEN),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nothing until the settings are read, so a tracker that's on doesn't flash the explainer.
            if (state.loading) return@LazyColumn
            item(key = "switch") { NamazSwitchCard(enabled = state.enabled, onEnabledChange = onEnabledChange) }
            if (!state.enabled) {
                item(key = "explainer") { NamazExplainerCard() }
                return@LazyColumn
            }
            state.next?.let { next ->
                item(key = "next") { NextPrayerCard(next = next, onCountdownEnd = onRefresh) }
            }
            item(key = "prayers_title") {
                Text(
                    text = stringResource(R.string.namaz_prayers_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryText
                )
            }
            item(key = "progress") { NamazProgress(prayed = state.progress) }
            // Five separate buttons, in the day's order.
            items(state.slots, key = { it.prayer.name }) { slot ->
                val buttonState = PrayerButtonState.of(slot)
                PrayerButton(
                    prayer = slot.prayer,
                    time = slot.time.format(formatter),
                    state = buttonState,
                    onClick = { onPrayerClick(slot) },
                    // A long press on a prayed one asks to take it back, like a tap.
                    onLongClick = if (buttonState == PrayerButtonState.PRAYED) ({ onPrayerClick(slot) }) else null,
                    modifier = Modifier.testTag(NamazTestTags.prayer(slot.prayer))
                )
            }
        }
    }
}

/** How many of the day's five prayers are prayed: "3 / 5 prayed today", over a bar. */
@Composable
private fun NamazProgress(prayed: Int) {
    Column(modifier = Modifier.testTag(NamazTestTags.PROGRESS), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.namaz_progress, prayed, NamazUiState.PRAYERS_PER_DAY),
            style = MaterialTheme.typography.bodyMedium,
            color = PixelTheme.colors.onSurface
        )
        PixelProgressBar(progress = prayed / NamazUiState.PRAYERS_PER_DAY.toFloat(), height = 14.dp)
    }
}

@Composable
private fun NamazTopBar(onNavigateBack: (() -> Unit)?) {
    val colors = PixelTheme.colors
    PixelTopAppBar(
        title = stringResource(R.string.namaz_title),
        navigationIcon = onNavigateBack?.let { back ->
            {
                IconButton(onClick = back) {
                    SymbolIcon(
                        symbol = "◀",
                        contentDescription = stringResource(R.string.navigate_back),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText
                    )
                }
            }
        }
    )
}
