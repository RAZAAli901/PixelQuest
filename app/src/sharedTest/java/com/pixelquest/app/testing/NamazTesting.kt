package com.pixelquest.app.testing

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.AutoMarkMissedUseCase
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.MarkPrayerUseCase
import com.pixelquest.app.domain.namaz.NamazPlaceResolver
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.domain.namaz.UndoPrayerUseCase
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.namaz.GeoPoint
import com.pixelquest.app.namaz.LocationProvider
import com.pixelquest.app.ui.screens.namaz.NamazViewModel
import java.time.LocalDateTime
import java.time.ZoneId

/** Lahore's time zone, where the Namaz tests' times are worked out. */
val LahoreZone: ZoneId = ZoneId.of("Asia/Karachi")

/** The phone's position for tests: [point], or none (no permission, or no fix). */
class FakeLocationProvider(var point: GeoPoint? = null) : LocationProvider {
    override fun hasPermission(): Boolean = point != null
    override suspend fun currentLocation(): GeoPoint? = point
}

/**
 * A NamazViewModel on fakes (or a real [repository]): the tracker on by default, a [clock] that stands
 * still (16:00 on 17 October 2026 by default, after Asr in Lahore), and [zone].
 */
fun namazViewModel(
    repository: NamazRepository = FakeNamazRepository(NamazSettingsEntity(enabled = true)),
    clock: FixedClock = FixedClock(LocalDateTime.of(2026, 10, 17, 16, 0)),
    zone: ZoneId = LahoreZone,
    location: FakeLocationProvider = FakeLocationProvider(),
    settings: SettingsRepository = FakeSettingsRepository()
): NamazViewModel {
    val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { zone }
    val nextPrayer = NextPrayerUseCase(provider)
    return NamazViewModel(
        repository, provider, nextPrayer,
        AutoMarkMissedUseCase(provider, repository, nextPrayer),
        MarkPrayerUseCase(provider, repository, nextPrayer),
        UndoPrayerUseCase(provider, repository, nextPrayer),
        NamazPlaceResolver(repository, location), settings, clock
    ) { zone }
}
