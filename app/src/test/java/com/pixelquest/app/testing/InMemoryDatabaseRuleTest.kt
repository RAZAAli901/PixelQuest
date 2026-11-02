package com.pixelquest.app.testing

import android.app.Application
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class InMemoryDatabaseRuleTest {

    @get:Rule
    val dbRule = InMemoryDatabaseRule()

    @Test
    fun seedNewUser_seedsZeroStreakAndDefaultProfile() = runBlocking {
        val seededProfile = dbRule.seedNewUser()
        assertEquals("NewAdventurer", seededProfile.username)

        val profileInDb = dbRule.db.userProfileDao().getProfile().first()
        assertNotNull(profileInDb)
        assertEquals(1, profileInDb?.level)
        assertNull(profileInDb?.supabaseUserId)

        val streak = dbRule.db.streakDao().getCurrentStreak().first()
        assertNotNull(streak)
        assertEquals(0, streak?.currentStreak)
    }

    @Test
    fun seed30DayStreakUser_seeds30DayStreakAndLogs() = runBlocking {
        val seededProfile = dbRule.seed30DayStreakUser()
        assertEquals(5, seededProfile.level)
        assertEquals(2500, seededProfile.totalXp)

        val streak = dbRule.db.streakDao().getCurrentStreak().first()
        assertEquals(30, streak?.currentStreak)
        assertEquals(30, streak?.perfectDaysCount)

        val allLogs = dbRule.db.taskCompletionLogDao().getAllLogs().first()
        assertEquals(30, allLogs.size)
    }

    @Test
    fun seedExpiredSession_seedsProfileWithStaleSupabaseUser() = runBlocking {
        val seededProfile = dbRule.seedExpiredSession()
        assertEquals("expired-auth-token-user-999", seededProfile.supabaseUserId)

        val profileInDb = dbRule.db.userProfileDao().getProfile().first()
        assertEquals("ExpiredAdventurer", profileInDb?.username)
        assertEquals("expired-auth-token-user-999", profileInDb?.supabaseUserId)
    }
}
