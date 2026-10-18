package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import com.pixelquest.app.worker.SyncScheduler
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * No Namaz detail leaves the phone. A prayer's XP joins the profile's total, as a quest's does,
 * and only that total (with the level and streaks) is synced to the leaderboard: never which
 * prayers were marked, or when.
 */
class NamazLeaderboardPrivacyTest {

    @Test
    fun theSyncedProfile_hasTotalsOnly_andNothingAboutPrayers() {
        val dto = CloudProfileDto(id = "user", displayName = "Aria", currentStreak = 3, longestStreak = 9, level = 4, totalXp = 1530)

        // How supabase-kt sends it: defaults left out except the @EncodeDefault numbers.
        val keys = Json { encodeDefaults = false }.parseToJsonElement(Json { encodeDefaults = false }.encodeToString(dto)).jsonObject.keys

        assertEquals(setOf("id", "display_name", "current_streak", "longest_streak", "level", "total_xp", "leaderboard_opt_in"), keys)
        assertTrue(keys.none { "namaz" in it || "prayer" in it })
    }

    @Test
    fun markingAPrayer_addsToTheXpTotal_andQueuesTheUsualProfileSync() = runBlocking {
        val day = LocalDate.of(2026, 10, 17)
        val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
        val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), FixedClock()) { LahoreZone }
        val profile = FakeUserProfileRepository()
        var syncs = 0
        val sync = object : SyncScheduler {
            override fun scheduleProfileSync(debounceMs: Long) {
                syncs++
            }
        }
        val mark = MarkPrayerUseCase(provider, repository, NextPrayerUseCase(provider), profile, sync)

        mark(day, Prayer.ASR, LocalDateTime.of(day, LocalTime.of(16, 0)).atZone(LahoreZone))

        assertEquals(10, profile.profile.value!!.totalXp)
        assertEquals(1, syncs)
    }
}
