package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DomainRulesCoverageTest {

    @Test
    fun cloudAvailability_identifiesRealAndPlaceholderUrls() {
        assertFalse(CloudAvailability.isRealProjectUrl("https://placeholder-project.supabase.co"))
        assertFalse(CloudAvailability.isRealProjectUrl(""))
        assertTrue(CloudAvailability.isRealProjectUrl("https://xyzcompany.supabase.co"))
        assertTrue(CloudAvailability.isRealProjectUrl("https://xyzcompany.supabase.co/"))

        // Local development stacks (plain http)
        assertTrue(CloudAvailability.isRealProjectUrl("http://127.0.0.1:54321", allowLocalHttp = true))
        assertTrue(CloudAvailability.isRealProjectUrl("http://localhost:54321", allowLocalHttp = true))
        assertTrue(CloudAvailability.isRealProjectUrl("http://10.0.2.2:54321", allowLocalHttp = true))
        assertFalse(CloudAvailability.isRealProjectUrl("http://127.0.0.1:54321", allowLocalHttp = false))
        assertFalse(CloudAvailability.isRealProjectUrl("http://malicious-site.com", allowLocalHttp = true))
    }

    @Test
    fun cloudAvailability_configurationValidation() {
        assertFalse(CloudAvailability.isConfigured("https://placeholder-project.supabase.co", "anon-key"))
        assertFalse(CloudAvailability.isConfigured("https://xyz.supabase.co", "placeholder-anon-key"))
        assertFalse(CloudAvailability.isConfigured("https://xyz.supabase.co", "  "))
        assertTrue(CloudAvailability.isConfigured("https://xyz.supabase.co", "real-anon-key-12345"))

        // Google config check
        assertFalse(
            CloudAvailability.isGoogleConfigured(
                "https://xyz.supabase.co",
                "real-anon-key",
                "placeholder-google-client-id",
            ),
        )
        assertFalse(
            CloudAvailability.isGoogleConfigured(
                "https://xyz.supabase.co",
                "real-anon-key",
                "invalid-client-id.other.com",
            ),
        )
        assertTrue(
            CloudAvailability.isGoogleConfigured(
                "https://xyz.supabase.co",
                "real-anon-key",
                "12345-abc.apps.googleusercontent.com",
            ),
        )
    }

    @Test
    fun heroName_rulesAndValidation() {
        assertEquals("ValidHero", HeroName.clamp("ValidHero"))
        assertEquals("NameWithTrimmedChars", HeroName.clamp("NameWithTrimmedChars"))

        // Blank validation
        assertNotNull(HeroName.error(""))
        assertNotNull(HeroName.error("   "))

        // Length validation
        val overLimit = "A".repeat(HeroName.MAX_LENGTH + 1)
        assertNotNull(HeroName.error(overLimit))

        val exactLimit = "A".repeat(HeroName.MAX_LENGTH)
        assertNull(HeroName.error(exactLimit))
        assertNull(HeroName.error("PixelKnight"))
    }

    @Test
    fun onboardingGate_evaluatesPlayedStatus() {
        assertFalse(OnboardingGate.hasPlayed(null))

        val freshProfile = UserProfileEntity(id = 1, username = "Newbie", avatarId = "a1", level = 1, totalXp = 0)
        assertFalse(OnboardingGate.hasPlayed(freshProfile))

        val leveledProfile = UserProfileEntity(id = 1, username = "Veteran", avatarId = "a1", level = 2, totalXp = 0)
        assertTrue(OnboardingGate.hasPlayed(leveledProfile))

        val xpProfile = UserProfileEntity(id = 1, username = "Grinder", avatarId = "a1", level = 1, totalXp = 50)
        assertTrue(OnboardingGate.hasPlayed(xpProfile))

        // isDone checks
        assertTrue(OnboardingGate.isDone(true, null))
        assertTrue(OnboardingGate.isDone(true, freshProfile))
        assertFalse(OnboardingGate.isDone(false, freshProfile))
        assertTrue(OnboardingGate.isDone(false, leveledProfile))
    }

    @Test
    fun questStart_calculatesStartDaysCorrectly() {
        val now = LocalDateTime.of(2026, 10, 23, 10, 0)

        // Time in future today
        val futureTime = LocalTime.of(15, 0)
        assertEquals(LocalDate.of(2026, 10, 23), QuestStart.firstDay(now, futureTime))
        assertFalse(QuestStart.startsTomorrow(now, futureTime))

        // Time in past today
        val pastTime = LocalTime.of(8, 0)
        assertEquals(LocalDate.of(2026, 10, 24), QuestStart.firstDay(now, pastTime))
        assertTrue(QuestStart.startsTomorrow(now, pastTime))
    }

    @Test
    fun weeklyDays_bitmaskRoundtripAndEffectiveDays() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val mask = WeeklyDays.toMask(days)
        val decoded = WeeklyDays.fromMask(mask)
        assertEquals(days, decoded)

        // All days
        val allDays = DayOfWeek.values().toSet()
        assertEquals(allDays, WeeklyDays.fromMask(WeeklyDays.toMask(allDays)))

        // Empty mask
        assertEquals(emptySet<DayOfWeek>(), WeeklyDays.fromMask(0))

        // Effective days when set is empty defaults to scheduled day
        val fallbackDate = LocalDate.of(2026, 10, 23) // Friday
        assertEquals(setOf(DayOfWeek.FRIDAY), WeeklyDays.effective(emptySet(), fallbackDate))
        assertEquals(days, WeeklyDays.effective(days, fallbackDate))
    }

    @Test
    fun safeErrorCopy_constantsIntegrity() {
        assertTrue(SafeErrorCopy.GOOGLE_FAILED.isNotBlank())
        assertTrue(SafeErrorCopy.SYNC_SERVER.isNotBlank())
        assertTrue(SafeErrorCopy.TASKS_FAILED.isNotBlank())
        assertTrue(SafeErrorCopy.BOARD_OFFLINE.isNotBlank())
    }
}
