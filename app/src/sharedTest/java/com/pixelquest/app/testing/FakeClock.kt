package com.pixelquest.app.testing

import com.pixelquest.app.util.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

/**
 * Deterministic [Clock] and [AppClock] implementation for tests.
 * Initialized by default to a fixed baseline instant (2026-01-01T00:00:00Z) so tests
 * do not depend on the real system wall-clock.
 */
class FakeClock(
    private var currentInstant: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    private val zoneId: ZoneId = ZoneId.of("UTC"),
) : Clock() {

    private val accessCounter = AtomicInteger(0)

    val accessCount: Int
        get() = accessCounter.get()

    val wasAccessed: Boolean
        get() = accessCounter.get() > 0

    private val ticksFlow = MutableStateFlow(LocalDateTime.ofInstant(currentInstant, zoneId))
    private val todayFlow = MutableStateFlow(LocalDate.ofInstant(currentInstant, zoneId))

    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = FakeClock(currentInstant, zone)

    override fun instant(): Instant {
        accessCounter.incrementAndGet()
        return currentInstant
    }

    override fun millis(): Long {
        accessCounter.incrementAndGet()
        return currentInstant.toEpochMilli()
    }

    fun set(instant: Instant) {
        currentInstant = instant
        notifyFlows()
    }

    fun set(localDateTime: LocalDateTime) {
        currentInstant = localDateTime.atZone(zoneId).toInstant()
        notifyFlows()
    }

    fun advanceBy(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
        notifyFlows()
    }

    fun advanceBy(millis: Long) {
        advanceBy(Duration.ofMillis(millis))
    }

    fun resetAccessCount() {
        accessCounter.set(0)
    }

    private fun notifyFlows() {
        ticksFlow.value = LocalDateTime.ofInstant(currentInstant, zoneId)
        todayFlow.value = LocalDate.ofInstant(currentInstant, zoneId)
    }

    /**
     * Converts this [FakeClock] to an [AppClock] that emits test flows without infinite delays.
     */
    fun asAppClock(): AppClock = object : AppClock(this) {
        override fun now(): LocalDateTime {
            accessCounter.incrementAndGet()
            return LocalDateTime.ofInstant(currentInstant, zoneId)
        }

        override fun todayDate(): LocalDate {
            accessCounter.incrementAndGet()
            return LocalDate.ofInstant(currentInstant, zoneId)
        }

        override fun millis(): Long = this@FakeClock.millis()

        override fun clock(): Clock = this@FakeClock

        override fun minuteTicks(): Flow<LocalDateTime> = ticksFlow.asStateFlow()

        override fun today(): Flow<LocalDate> = todayFlow.asStateFlow()
    }
}
