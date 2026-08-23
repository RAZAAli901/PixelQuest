package com.pixelquest.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.UserProfileRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarSelectionPersistenceTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun avatarSelection_persistsCorrectlyInRoomDatabase() = runBlocking {
        val repository = UserProfileRepositoryImpl(db.userProfileDao(), db.levelHistoryDao())
        val defaultProfile = UserProfileEntity(id = 1, username = "PixelHero", avatarId = "avatar_hero", level = 1)
        repository.insertProfile(defaultProfile)

        // Select new avatar
        val updatedProfile = defaultProfile.copy(avatarId = "avatar_mage")
        repository.updateProfile(updatedProfile)

        val fetchedProfile = repository.getProfile().first()
        assertEquals("avatar_mage", fetchedProfile?.avatarId)
    }
}
