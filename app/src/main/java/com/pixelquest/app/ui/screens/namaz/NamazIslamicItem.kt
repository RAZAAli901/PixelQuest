package com.pixelquest.app.ui.screens.namaz

import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.islamic.IslamicItemProvider
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.util.AppClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject

/**
 * The Namaz Tracker on the Islamic hub (docs/islamic-hub.md). Shown while the feature is available,
 * whether or not the tracker is on, since its screen is where it's switched on (as the menu's entry
 * was). Its badge counts the prayers to mark.
 */
class NamazIslamicItem(
    private val settings: SettingsRepository,
    private val repository: NamazRepository,
    private val nextPrayer: NextPrayerUseCase,
    private val clock: AppClock,
    private val zone: () -> ZoneId
) : IslamicItemProvider {

    @Inject
    constructor(settings: SettingsRepository, repository: NamazRepository, nextPrayer: NextPrayerUseCase, clock: AppClock) :
        this(settings, repository, nextPrayer, clock, { ZoneId.systemDefault() })

    override val order: Int = 10

    override fun item(): Flow<IslamicItem> = combine(settings.namazAvailable, toMark()) { available, toMark ->
        IslamicItem(
            id = ID,
            titleRes = R.string.namaz_title,
            descriptionRes = R.string.islamic_namaz_description,
            icon = IslamicIcon("🕌", pixel = R.drawable.ic_namaz_pixel, comic = R.drawable.ic_namaz_comic),
            route = Screen.Namaz.route,
            enabled = available,
            badgeCount = toMark,
            badgeLabel = R.plurals.islamic_namaz_badge
        )
    }

    /**
     * While the tracker is on: the prayers of the current prayer day that have started and aren't
     * marked prayed, missed ones included, since they can still be marked late. Rechecked each
     * minute, as a prayer's time comes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun toMark(): Flow<Int> = combine(clock.minuteTicks(), repository.observeSettings()) { _, settings ->
        settings.enabled
    }.flatMapLatest { on ->
        if (!on) {
            flowOf(0)
        } else {
            val now = nextPrayer(clock.now().atZone(zone()))
            val started = Prayer.entries.take(now.current.prayer.ordinal + 1)
            repository.observeDay(now.prayerDay).map { logs ->
                started.count { prayer -> logs.none { it.prayer == prayer && it.status == PrayerStatus.PRAYED } }
            }
        }
    }.distinctUntilChanged()

    companion object {
        const val ID = "namaz"
    }
}
