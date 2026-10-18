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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerSlot
import com.pixelquest.app.domain.namaz.remindsFor
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
    const val DAY_STRIP = "namaz_day_strip"
    const val STREAK = "namaz_streak"
    const val SETTINGS = "namaz_settings"
    const val LOCATION = "namaz_location"
    const val HIJRI = "namaz_hijri"
    const val HOME_CARD = "namaz_home_card"
    const val EXACT_ALARM_CARD = "namaz_exact_alarm_card"
    const val NOTIFICATIONS_CARD = "namaz_notifications_card"
    const val BATTERY_CARD = "namaz_battery_card"
    const val TEST_REMINDER = "namaz_test_reminder"
    const val XP_POPUP = "namaz_xp_popup"
    const val ALL_FIVE = "namaz_all_five"
    const val CITY_PICKER = "namaz_city_picker"

    /** A prayer's adjustment row in the settings sheet. */
    fun adjust(prayer: Prayer) = "namaz_adjust_${prayer.name}"

    /** A day in the day strip. */
    fun day(date: java.time.LocalDate) = "namaz_day_$date"

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
    val settingsVisible by viewModel.settingsVisible.collectAsState()
    val reminderOptions by viewModel.options.collectAsState()
    val xpPopup by viewModel.xpPopup.collectAsState()
    val pendingLevelUp by viewModel.pendingLevelUp.collectAsState()
    val allFive by viewModel.allFive.collectAsState()
    val locationLookup by viewModel.locationLookup.collectAsState()
    val locationRequest = rememberLocationPermissionRequest { granted ->
        if (granted) viewModel.useMyLocation() else viewModel.locationRefused()
    }
    var pickingCity by rememberSaveable { mutableStateOf(false) }
    // Reminders chosen in the setup: Android's notification prompt right then, while it's clear why.
    val askForNotifications = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            when (message) {
                NamazMessage.CLOCK_BEHIND -> snackbarHostState.showSnackbar(context.getString(R.string.namaz_clock_behind))
            }
        }
    }

    NamazContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onEnabledChange = viewModel::setEnabled,
        onRefresh = viewModel::refresh,
        onDaySelected = viewModel::selectDay,
        onOpenSettings = viewModel::openSettings,
        locationLookup = locationLookup,
        onUseMyLocation = locationRequest::launch,
        onPickCity = { pickingCity = true },
        xpPopup = xpPopup,
        onXpPopupShown = viewModel::xpPopupShown,
        reminderChecks = {
            NamazReminderChecks(
                batteryTipDismissed = reminderOptions.batteryTipDismissed,
                onDismissBatteryTip = viewModel::dismissBatteryTip
            )
        },
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

    allFive?.let { NamazAllFiveCard(allFive = it, showXp = state.showXp, onDismiss = viewModel::allFiveSeen) }

    // A level reached overnight and not yet celebrated: the game's own celebration (Pixel or Comic).
    pendingLevelUp?.let { level ->
        com.pixelquest.app.ui.screens.leveling.LevelUpCelebrationScreen(level = level, onDismiss = viewModel::levelUpCelebrated)
    }

    if (pickingCity) {
        NamazCityPicker(
            onPick = { city ->
                viewModel.chooseCity(city)
                pickingCity = false
            },
            onDismiss = { pickingCity = false }
        )
    }

    if (settingsVisible) {
        NamazSettingsSheet(
            settings = state.settings,
            onMethod = viewModel::setMethod,
            onMadhab = viewModel::setMadhab,
            onAdjust = viewModel::adjust,
            onDismiss = viewModel::closeSettings,
            onReminderOffset = viewModel::setReminderOffset,
            nudge = reminderOptions.nudge,
            onNudge = viewModel::setNudge,
            onReminder = viewModel::setReminder,
            sound = reminderOptions.sound,
            onSound = viewModel::setSound,
            fajrThroughDnd = reminderOptions.fajrThroughDnd,
            onFajrThroughDnd = viewModel::setFajrThroughDnd,
            onTestReminder = if (com.pixelquest.app.BuildConfig.DEBUG) viewModel::testReminder else null,
            onXpEnabled = viewModel::setXpEnabled,
            isSimpleMode = state.isSimpleMode
        )
    }

    if (setupVisible) {
        NamazSetupSheet(
            settings = state.settings,
            place = state.place,
            locationLookup = locationLookup,
            onUseMyLocation = locationRequest::launch,
            onDone = { method, remind ->
                viewModel.finishSetup(method, remind)
                if (remind && needsNotificationPermission(context)) {
                    askForNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
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
    onDaySelected: (java.time.LocalDate) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    locationLookup: LocationLookup = LocationLookup.IDLE,
    onUseMyLocation: () -> Unit = {},
    onPickCity: () -> Unit = {},
    xpPopup: NamazXpPopup? = null,
    onXpPopupShown: () -> Unit = {},
    // The phone's side of the reminders (permissions, battery). A slot: it reads Android's state.
    reminderChecks: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onPrayerClick: (PrayerSlot) -> Unit = {}
) {
    CompositionLocalProvider(LocalNamazSimpleMode provides state.isSimpleMode) {
        NamazScaffold(
            state = state,
            onNavigateBack = onNavigateBack,
            onEnabledChange = onEnabledChange,
            onRefresh = onRefresh,
            onDaySelected = onDaySelected,
            onOpenSettings = onOpenSettings,
            locationLookup = locationLookup,
            onUseMyLocation = onUseMyLocation,
            onPickCity = onPickCity,
            xpPopup = xpPopup,
            onXpPopupShown = onXpPopupShown,
            reminderChecks = reminderChecks,
            snackbarHostState = snackbarHostState,
            onPrayerClick = onPrayerClick
        )
    }
}

@Composable
private fun NamazScaffold(
    state: NamazUiState,
    onNavigateBack: (() -> Unit)?,
    onEnabledChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDaySelected: (java.time.LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    locationLookup: LocationLookup,
    onUseMyLocation: () -> Unit,
    onPickCity: () -> Unit,
    xpPopup: NamazXpPopup?,
    onXpPopupShown: () -> Unit,
    reminderChecks: @Composable () -> Unit,
    snackbarHostState: SnackbarHostState,
    onPrayerClick: (PrayerSlot) -> Unit
) {
    val colors = PixelTheme.colors
    val formatter = rememberPrayerTimeFormatter()
    val playful = namazStyle().playful
    Scaffold(
        containerColor = colors.background,
        topBar = {
            NamazTopBar(
                onNavigateBack = onNavigateBack,
                // Settings are for the tracker in use: hidden while it's off.
                onOpenSettings = if (state.enabled) onOpenSettings else null
            )
        },
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
      androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .testTag(NamazTestTags.SCREEN),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nothing until the settings are read, so a tracker that's on doesn't flash the explainer.
            if (state.loading) return@LazyColumn
            if (state.enabled && state.today != null) {
                item(key = "hijri") { HijriDateLine(date = state.today) }
            }
            item(key = "switch") { NamazSwitchCard(enabled = state.enabled, onEnabledChange = onEnabledChange) }
            if (!state.enabled) {
                item(key = "explainer") { NamazExplainerCard() }
                return@LazyColumn
            }
            // Only while there's a reminder for the phone to get in the way of.
            if (Prayer.entries.any { state.settings.remindsFor(it) }) {
                item(key = "reminder_checks") { reminderChecks() }
            }
            state.next?.let { next ->
                item(key = "next") { NextPrayerCard(next = next, onCountdownEnd = onRefresh) }
            }
            item(key = "location") {
                NamazLocationCard(
                    place = state.place,
                    locationLookup = locationLookup,
                    onUseMyLocation = onUseMyLocation,
                    onPickCity = onPickCity
                )
            }
            if (state.days.isNotEmpty() && state.day != null) {
                item(key = "day_strip") {
                    NamazDayStrip(
                        days = state.days,
                        today = state.today,
                        selected = state.day.date,
                        onSelect = onDaySelected,
                        week = state.week
                    )
                }
                item(key = "streak") {
                    // TalkBack gets the plain words: "fire" isn't part of the sentence.
                    val plain = if (state.streak > 0) {
                        pluralStringResource(R.plurals.namaz_streak_plain, state.streak, state.streak)
                    } else {
                        stringResource(R.string.namaz_streak_none)
                    }
                    Text(
                        text = if (state.streak > 0 && playful) {
                            pluralStringResource(R.plurals.namaz_streak, state.streak, state.streak)
                        } else {
                            plain
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        modifier = Modifier.testTag(NamazTestTags.STREAK).semantics { contentDescription = plain }
                    )
                }
            }
            item(key = "prayers_title") {
                NamazHeading(
                    text = if (state.isPastDay) {
                        stringResource(R.string.namaz_prayers_on, state.day!!.date.format(DayTitleFormatter))
                    } else {
                        stringResource(R.string.namaz_prayers_title)
                    }
                )
                if (state.isPastDay) {
                    Text(
                        text = stringResource(R.string.namaz_past_read_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            item(key = "progress") { NamazProgress(prayed = state.progress, pastDay = state.isPastDay) }
            // Five separate buttons, in the day's order.
            items(state.slots, key = { it.prayer.name }) { slot ->
                val buttonState = PrayerButtonState.of(slot)
                PrayerButton(
                    prayer = slot.prayer,
                    time = slot.time.format(formatter),
                    state = buttonState,
                    onClick = { onPrayerClick(slot) },
                    // A long press on a prayed one asks to take it back, like a tap.
                    onLongClick = if (buttonState == PrayerButtonState.PRAYED && !state.isPastDay) ({ onPrayerClick(slot) }) else null,
                    // An earlier day is read-only: nothing to tap, and TalkBack offers nothing.
                    readOnly = state.isPastDay,
                    modifier = Modifier.testTag(NamazTestTags.prayer(slot.prayer))
                )
            }
        }
        // XP just earned, over the list near the top.
        xpPopup?.let { popup ->
            NamazXpPopupView(
                popup = popup,
                onFinished = onXpPopupShown,
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter).padding(top = 96.dp)
            )
        }
      }
    }
}

/** The prayer day's Hijri date, under the title: "🌙 6 Jumada al-Ula 1448 AH". */
@Composable
private fun HijriDateLine(date: java.time.LocalDate) {
    val hijri = remember(date) { HijriDate.of(date) }
    val month = stringArrayResource(R.array.namaz_hijri_months)[hijri.month - 1]
    val text = stringResource(R.string.namaz_hijri_date, hijri.day, month, hijri.year)
    Text(
        text = "🌙 $text",
        style = MaterialTheme.typography.bodyMedium,
        color = PixelTheme.colors.onSurfaceVariant,
        // Without the moon's name read first.
        modifier = Modifier.testTag(NamazTestTags.HIJRI).semantics { contentDescription = text }
    )
}

/** "Mon 12 Oct", for an earlier day's heading. */
private val DayTitleFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM")

/** How many of the day's five prayers are prayed: "3 / 5 prayed today", over a bar. */
@Composable
private fun NamazProgress(prayed: Int, pastDay: Boolean) {
    Column(modifier = Modifier.testTag(NamazTestTags.PROGRESS), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = pluralStringResource(
                if (pastDay) R.plurals.namaz_progress_past else R.plurals.namaz_progress,
                prayed,
                prayed,
                NamazUiState.PRAYERS_PER_DAY
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = PixelTheme.colors.onSurface
        )
        PixelProgressBar(progress = prayed / NamazUiState.PRAYERS_PER_DAY.toFloat(), height = 14.dp)
    }
}

@Composable
private fun NamazTopBar(onNavigateBack: (() -> Unit)?, onOpenSettings: (() -> Unit)?) {
    val colors = PixelTheme.colors
    PixelTopAppBar(
        title = stringResource(R.string.namaz_title),
        actions = onOpenSettings?.let { open ->
            {
                IconButton(onClick = open) {
                    SymbolIcon(
                        symbol = "⚙️",
                        contentDescription = stringResource(R.string.namaz_settings_open),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryText
                    )
                }
            }
        },
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
