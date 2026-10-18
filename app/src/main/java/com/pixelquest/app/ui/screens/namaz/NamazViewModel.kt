package com.pixelquest.app.ui.screens.namaz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.AutoMarkMissedUseCase
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.GetTodayNamazUseCase
import com.pixelquest.app.domain.namaz.NamazDay
import com.pixelquest.app.domain.namaz.NamazMethod
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.domain.namaz.NamazPlaceResolver
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.PrayerSlot
import com.pixelquest.app.domain.namaz.PrayerTimeProvider
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.util.AppClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * What the Namaz screen shows.
 *
 * @param day the current prayer day's five prayers with their statuses (null while loading).
 * @param next the current prayer, the next one and the time until it.
 * @param settings the tracker's settings, for the setup and settings sheets.
 */
data class NamazUiState(
    val loading: Boolean = true,
    val enabled: Boolean = false,
    val settings: NamazSettingsEntity = NamazSettingsEntity(),
    val place: NamazPlace = NamazPlace.LAHORE,
    val day: NamazDay? = null,
    val next: NextPrayer? = null
) {
    val slots: List<PrayerSlot> get() = day?.slots.orEmpty()

    /** Prayers prayed on the current prayer day, 0 to [PRAYERS_PER_DAY]. */
    val progress: Int get() = day?.prayedCount ?: 0

    companion object {
        const val PRAYERS_PER_DAY = 5
    }
}

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
    private val placeResolver: NamazPlaceResolver,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
    private val zone: () -> ZoneId
) : ViewModel() {

    @Inject
    constructor(
        repository: NamazRepository,
        provider: PrayerTimeProvider,
        nextPrayer: NextPrayerUseCase,
        autoMarkMissed: AutoMarkMissedUseCase,
        placeResolver: NamazPlaceResolver,
        settingsRepository: SettingsRepository,
        clock: AppClock
    ) : this(repository, provider, nextPrayer, autoMarkMissed, placeResolver, settingsRepository, clock, { ZoneId.systemDefault() })

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

    /** Location was refused: the times stay where they are, and the screen says so. */
    fun locationRefused() {
        _locationLookup.value = LocationLookup.UNAVAILABLE
    }

    /** Draws the screen again now, e.g. when it comes back to the front. */
    fun refresh() {
        refreshRequests.value++
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NamazUiState> = combine(clock.minuteTicks(), refreshRequests, repository.observeSettings()) { _, _, settings ->
        now() to settings
    }.flatMapLatest { (now, settings) ->
        val next = nextPrayer(now)
        val times = provider.timesFor(next.prayerDay)
        val nextFajr = provider.timesFor(next.prayerDay.plusDays(1)).fajr
        repository.observeDay(next.prayerDay).map { logs ->
            NamazUiState(
                loading = false,
                enabled = settings.enabled,
                settings = settings,
                place = DefaultPrayerTimeProvider.placeOf(settings.latitude, settings.longitude, settings.city),
                day = GetTodayNamazUseCase.combine(times, nextFajr, logs, now),
                next = next
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NamazUiState())
}
