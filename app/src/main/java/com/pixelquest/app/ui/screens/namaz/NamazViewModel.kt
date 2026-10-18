package com.pixelquest.app.ui.screens.namaz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.AsrMadhab
import com.pixelquest.app.domain.namaz.AutoMarkMissedUseCase
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.GetTodayNamazUseCase
import com.pixelquest.app.domain.namaz.MarkPrayerUseCase
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NamazDay
import com.pixelquest.app.domain.namaz.NamazMethod
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.domain.namaz.NamazPlaceResolver
import com.pixelquest.app.domain.namaz.NamazReminderOptions
import com.pixelquest.app.domain.namaz.NamazReminderOptionsStore
import com.pixelquest.app.domain.namaz.NamazStreak
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerSlot
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.namaz.PrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazSound
import com.pixelquest.app.domain.namaz.UndoPrayerUseCase
import com.pixelquest.app.domain.namaz.withReminder
import com.pixelquest.app.domain.namaz.adjustmentFor
import com.pixelquest.app.domain.namaz.withAdjustment
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.namaz.NamazAlarms
import com.pixelquest.app.util.AppClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * What the Namaz screen shows.
 *
 * @param day the shown day's five prayers with their statuses: the current prayer day, or an
 *   earlier one picked from the day strip (null while loading).
 * @param next the current prayer, the next one and the time until it.
 * @param settings the tracker's settings, for the setup and settings sheets.
 * @param today the current prayer day (before Fajr, still yesterday's date).
 * @param days the day strip: the last [STRIP_DAYS] prayer days, oldest first, [today] last.
 * @param week each strip day's five statuses, Fajr to Isha, for its row of dots.
 * @param streak prayer days in a row with all five prayed (NamazStreak).
 * @param isSimpleMode plain wording and looks, no game touches.
 */
data class NamazUiState(
    val loading: Boolean = true,
    val enabled: Boolean = false,
    val settings: NamazSettingsEntity = NamazSettingsEntity(),
    val place: NamazPlace = NamazPlace.LAHORE,
    val day: NamazDay? = null,
    val next: NextPrayer? = null,
    val today: LocalDate? = null,
    val days: List<LocalDate> = emptyList(),
    val week: Map<LocalDate, List<PrayerStatus>> = emptyMap(),
    val streak: Int = 0,
    val isSimpleMode: Boolean = false
) {
    val slots: List<PrayerSlot> get() = day?.slots.orEmpty()

    /** Prayers prayed on the shown day, 0 to [PRAYERS_PER_DAY]. */
    val progress: Int get() = day?.prayedCount ?: 0

    /** An earlier day from the strip is shown: read-only. */
    val isPastDay: Boolean get() = day != null && today != null && day.date != today

    companion object {
        const val PRAYERS_PER_DAY = 5
        const val STRIP_DAYS = 7
    }
}

/** One-off messages from the tracker to its screen. */
enum class NamazMessage { CLOCK_BEHIND }

/** Use my location, as it goes: looking, or no position to be had (no permission, location off, no fix). */
enum class LocationLookup { IDLE, LOOKING, UNAVAILABLE }

/**
 * The Namaz Tracker's state: the current prayer day's times and statuses, the next prayer and
 * the day's progress. It moves on every minute, so a prayer unlocks or turns missed on time and
 * the day rolls over at Fajr (before Fajr it's still the previous prayer day). Opening it settles
 * prayers left pending past their window, as the hourly NamazMissedWorker does.
 */
@HiltViewModel
class NamazViewModel(
    private val repository: NamazRepository,
    private val provider: PrayerTimeProvider,
    private val nextPrayer: NextPrayerUseCase,
    private val autoMarkMissed: AutoMarkMissedUseCase,
    private val markPrayer: MarkPrayerUseCase,
    private val undoPrayer: UndoPrayerUseCase,
    private val placeResolver: NamazPlaceResolver,
    private val settingsRepository: SettingsRepository,
    private val reminderOptions: NamazReminderOptionsStore,
    private val alarms: NamazAlarms,
    private val clock: AppClock,
    private val zone: () -> ZoneId
) : ViewModel() {

    @Inject
    constructor(
        repository: NamazRepository,
        provider: PrayerTimeProvider,
        nextPrayer: NextPrayerUseCase,
        autoMarkMissed: AutoMarkMissedUseCase,
        markPrayer: MarkPrayerUseCase,
        undoPrayer: UndoPrayerUseCase,
        placeResolver: NamazPlaceResolver,
        settingsRepository: SettingsRepository,
        reminderOptions: NamazReminderOptionsStore,
        alarms: NamazAlarms,
        clock: AppClock
    ) : this(
        repository, provider, nextPrayer, autoMarkMissed, markPrayer, undoPrayer, placeResolver, settingsRepository,
        reminderOptions, alarms, clock, { ZoneId.systemDefault() }
    )

    init {
        viewModelScope.launch { autoMarkMissed(now()) }
    }

    private fun now(): ZonedDateTime = clock.now().atZone(zone())

    private val refreshRequests = MutableStateFlow(0)

    /**
     * The master switch. Off hides the tracker and stops its workers writing; the prayers already
     * marked are kept, and come back when it's turned on again.
     */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSettings(repository.getSettings().copy(enabled = enabled))
            if (enabled && !settingsRepository.namazSetupShown.first()) _setupVisible.value = true
        }
    }

    private val _setupVisible = MutableStateFlow(false)

    /** The first-time setup sheet is open: once, when the switch is first turned on. */
    val setupVisible: StateFlow<Boolean> = _setupVisible.asStateFlow()

    /** The setup's choices: the calculation method, and a reminder at each prayer's start (or none). */
    fun finishSetup(method: NamazMethod, remind: Boolean) {
        viewModelScope.launch {
            repository.saveSettings(
                repository.getSettings().copy(
                    method = method.name,
                    remindFajr = remind, remindDhuhr = remind, remindAsr = remind, remindMaghrib = remind, remindIsha = remind
                )
            )
            closeSetup()
        }
    }

    /** Closed without Done: it isn't shown again, and the defaults stay. */
    fun dismissSetup() {
        viewModelScope.launch { closeSetup() }
    }

    private suspend fun closeSetup() {
        settingsRepository.setNamazSetupShown(true)
        _setupVisible.value = false
    }

    private val _locationLookup = MutableStateFlow(LocationLookup.IDLE)
    val locationLookup: StateFlow<LocationLookup> = _locationLookup.asStateFlow()

    /** Use my location, once it's allowed: the phone's rough position becomes the tracker's place. */
    fun useMyLocation() {
        viewModelScope.launch {
            _locationLookup.value = LocationLookup.LOOKING
            _locationLookup.value = if (placeResolver.useMyLocation() != null) LocationLookup.IDLE else LocationLookup.UNAVAILABLE
        }
    }

    /** The player picked [place] from the city list: the times follow it. */
    fun chooseCity(place: NamazPlace) {
        viewModelScope.launch {
            placeResolver.chooseCity(place)
            _locationLookup.value = LocationLookup.IDLE
        }
    }

    /** Location was refused: the times stay where they are, and the screen says so. */
    fun locationRefused() {
        _locationLookup.value = LocationLookup.UNAVAILABLE
    }

    /**
     * Marks [prayer] prayed on the current prayer day: on time, or late once its window has passed.
     * One whose time hasn't come isn't marked (MarkPrayerUseCase); the screen locks it anyway.
     */
    fun markPrayed(prayer: Prayer) {
        viewModelScope.launch {
            val now = now()
            val result = markPrayer(nextPrayer(now).prayerDay, prayer, now)
            if (result == com.pixelquest.app.domain.namaz.MarkPrayerResult.ClockBehind) _messages.tryEmit(NamazMessage.CLOCK_BEHIND)
        }
    }

    private val _messages = MutableSharedFlow<NamazMessage>(extraBufferCapacity = 1)

    /** Something the screen should say once, in its snackbar. */
    val messages: SharedFlow<NamazMessage> = _messages

    /** Takes [prayer]'s prayed mark back, on the current prayer day: pending again, or missed if its window passed. */
    fun undo(prayer: Prayer) {
        viewModelScope.launch {
            val now = now()
            undoPrayer(nextPrayer(now).prayerDay, prayer, now)
        }
    }

    private val _settingsVisible = MutableStateFlow(false)

    /** The settings sheet, from the top bar, is open. */
    val settingsVisible: StateFlow<Boolean> = _settingsVisible.asStateFlow()

    fun openSettings() {
        _settingsVisible.value = true
    }

    fun closeSettings() {
        _settingsVisible.value = false
    }

    // Each change is saved at once, so the times on the screen follow it.
    fun setMethod(method: NamazMethod) = updateSettings { it.copy(method = method.name) }

    fun setMadhab(madhab: AsrMadhab) = updateSettings { it.copy(asrMadhab = madhab.name) }

    /** The reminder options outside the tracker's settings (the nudge, and from C122 the sound). */
    val options: StateFlow<NamazReminderOptions> =
        reminderOptions.options.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NamazReminderOptions())

    fun setNudge(on: Boolean) {
        viewModelScope.launch { reminderOptions.update { it.copy(nudge = on) } }
    }

    /** [prayer]'s reminder on or off. */
    fun setReminder(prayer: Prayer, on: Boolean) = updateSettings { it.withReminder(prayer, on) }

    fun setSound(sound: NamazSound) {
        viewModelScope.launch { reminderOptions.update { it.copy(sound = sound) } }
    }

    /** Fajr's reminder through Do Not Disturb, as an alarm. */
    fun setFajrThroughDnd(on: Boolean) {
        viewModelScope.launch { reminderOptions.update { it.copy(fajrThroughDnd = on) } }
    }

    /**
     * Debug builds: the current prayer's reminder in [TEST_REMINDER_SECONDS], through a real alarm,
     * the receiver and the notification, with its buttons working. It shows even with the tracker
     * or that prayer's reminder off.
     */
    fun testReminder() {
        viewModelScope.launch {
            val now = now()
            val current = nextPrayer(now).current
            alarms.schedule(
                NamazAlarm(NamazAlarm.Kind.TEST, current.day, current.prayer, now.plusSeconds(TEST_REMINDER_SECONDS).toInstant().toEpochMilli())
            )
        }
    }

    /** Hides the battery-optimisation tip for good. */
    fun dismissBatteryTip() {
        viewModelScope.launch { reminderOptions.update { it.copy(batteryTipDismissed = true) } }
    }

    /** Reminders at each prayer's start, or [minutes] before it. */
    fun setReminderOffset(minutes: Int) = updateSettings { it.copy(reminderOffsetMin = minutes) }

    /** Moves [prayer]'s time by [minutes] (a minute at a time), up to [MAX_ADJUST_MINUTES] either way. */
    fun adjust(prayer: Prayer, minutes: Int) = updateSettings {
        it.withAdjustment(prayer, (it.adjustmentFor(prayer) + minutes).coerceIn(-MAX_ADJUST_MINUTES, MAX_ADJUST_MINUTES))
    }

    private fun updateSettings(change: (NamazSettingsEntity) -> NamazSettingsEntity) {
        viewModelScope.launch { repository.saveSettings(change(repository.getSettings())) }
    }

    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    /** Shows [date] from the day strip; earlier days are read-only. The current prayer day is the default. */
    fun selectDay(date: LocalDate) {
        selectedDay.value = date
    }

    /** Draws the screen again now, e.g. when it comes back to the front. */
    fun refresh() {
        refreshRequests.value++
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NamazUiState> = combine(
        clock.minuteTicks(), refreshRequests, repository.observeSettings(), selectedDay, settingsRepository.simpleModeEnabled
    ) { _, _, settings, selected, simple ->
        Inputs(now(), settings, selected, simple)
    }.flatMapLatest { (now, settings, selected, simple) ->
        val next = nextPrayer(now)
        val today = next.prayerDay
        val days = (NamazUiState.STRIP_DAYS - 1 downTo 0).map { today.minusDays(it.toLong()) }
        // A day picked before the strip moved on (at Fajr) that's no longer in it shows today again.
        val shown = selected?.takeIf { it in days } ?: today
        // Each strip day's times, and the day after's for Isha's window.
        val times = (days + today.plusDays(1)).associateWith { provider.timesFor(it) }
        combine(
            repository.observeRange(days.first(), today),
            repository.observePrayedPerDay(today.minusYears(STREAK_YEARS), today)
        ) { logs, prayedPerDay ->
            val byDay = logs.groupBy { it.date }
            val week = days.associateWith { date ->
                GetTodayNamazUseCase.combine(times.getValue(date), times.getValue(date.plusDays(1)).fajr, byDay[date].orEmpty(), now)
            }
            NamazUiState(
                loading = false,
                enabled = settings.enabled,
                settings = settings,
                place = DefaultPrayerTimeProvider.placeOf(settings.latitude, settings.longitude, settings.city),
                day = week.getValue(shown),
                next = next,
                today = today,
                days = days,
                week = week.mapValues { (_, namazDay) -> namazDay.slots.map { it.status } },
                streak = NamazStreak.current(prayedPerDay.associate { it.date to it.prayed }, today),
                isSimpleMode = simple
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NamazUiState())

    /** What the screen's state is worked out from, each time one of them changes. */
    private data class Inputs(val now: ZonedDateTime, val settings: NamazSettingsEntity, val selected: LocalDate?, val simple: Boolean)

    companion object {
        /** How far back a streak is counted: far longer than anyone's, and a small query. */
        private const val STREAK_YEARS = 10L

        /** A prayer's time can be moved this many minutes either way, to match a local mosque. */
        const val MAX_ADJUST_MINUTES = 30

        /** How soon the debug test reminder comes. */
        const val TEST_REMINDER_SECONDS = 10L
    }
}
