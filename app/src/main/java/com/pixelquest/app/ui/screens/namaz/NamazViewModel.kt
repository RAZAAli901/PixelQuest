package com.pixelquest.app.ui.screens.namaz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.namaz.AutoMarkMissedUseCase
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.GetTodayNamazUseCase
import com.pixelquest.app.domain.namaz.NamazDay
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.PrayerSlot
import com.pixelquest.app.domain.namaz.PrayerTimeProvider
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.util.AppClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
 */
data class NamazUiState(
    val loading: Boolean = true,
    val enabled: Boolean = false,
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
    private val clock: AppClock,
    private val zone: () -> ZoneId
) : ViewModel() {

    @Inject
    constructor(
        repository: NamazRepository,
        provider: PrayerTimeProvider,
        nextPrayer: NextPrayerUseCase,
        autoMarkMissed: AutoMarkMissedUseCase,
        clock: AppClock
    ) : this(repository, provider, nextPrayer, autoMarkMissed, clock, { ZoneId.systemDefault() })

    init {
        viewModelScope.launch { autoMarkMissed(now()) }
    }

    private fun now(): ZonedDateTime = clock.now().atZone(zone())

    private val refreshRequests = MutableStateFlow(0)

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
                place = DefaultPrayerTimeProvider.placeOf(settings.latitude, settings.longitude, settings.city),
                day = GetTodayNamazUseCase.combine(times, nextFajr, logs, now),
                next = next
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NamazUiState())
}
