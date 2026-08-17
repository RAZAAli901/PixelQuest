package com.pixelquest.app.data.local.dao

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class StreakAndUserProfileDaoTest : BaseDaoTest() {

    private lateinit var streakDao: StreakDao
    private lateinit var userProfileDao: UserProfileDao

    @Before
    override fun createDb() {
        super.createDb()
        streakDao = database.streakDao()
        userProfileDao = database.userProfileDao()
    }

    @Test
    fun streakInsertAndUpdateFlowEmission() = runBlocking {
        val initial = StreakEntity(id = 1, currentStreak = 3, longestStreak = 5, perfectDaysCount = 2)
        streakDao.insertStreak(initial)

        val fetched1 = streakDao.getCurrentStreak().first()
        assertNotNull(fetched1)
        assertEquals(3, fetched1?.currentStreak)

        val updated = initial.copy(currentStreak = 4, longestStreak = 6, lastCompletedDate = LocalDate.now())
        streakDao.updateStreak(updated)

        val fetched2 = streakDao.getCurrentStreak().first()
        assertEquals(4, fetched2?.currentStreak)
        assertEquals(6, fetched2?.longestStreak)
    }

    @Test
    fun userProfileInsertAndUpdateFlowEmission() = runBlocking {
        val initialProfile = UserProfileEntity(id = 1, username = "PixelMaster", avatarId = "avatar_1", level = 1, totalXp = 100)
        userProfileDao.insertProfile(initialProfile)

        val profile1 = userProfileDao.getProfile().first()
        assertNotNull(profile1)
        assertEquals("PixelMaster", profile1?.username)
        assertEquals(1, profile1?.level)

        val updatedProfile = initialProfile.copy(level = 2, totalXp = 250)
        userProfileDao.updateProfile(updatedProfile)

        val profile2 = userProfileDao.getProfile().first()
        assertEquals(2, profile2?.level)
        assertEquals(250, profile2?.totalXp)
    }
}
