package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A sync the server refuses as it stands (a name its filter rejects, a forbidden write) isn't retried:
 * every attempt gets the same answer, and WorkManager used to retry it with backoff for as long as the
 * app was installed. Outages, rate limits and an expired session are still retried.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SyncRefusalTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun syncAnswered(push: SupabaseResult<Unit>): ListenableWorker.Result = runBlocking {
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Hero", avatarId = "avatar_hero", level = 2, totalXp = 400, supabaseUserId = "acct",
                leaderboardOptIn = true, leaderboardDisplayName = "Titan_Slayer")
        )
        val server = object : CloudProfileRepository {
            override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String) = push
            override suspend fun syncProfileToCloud() = push
            override suspend fun fetchCloudProfile(userId: String): SupabaseResult<CloudProfileDto?> = SupabaseResult.Success(null)
        }
        TestListenableWorkerBuilder<ProfileSyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    ProfileSyncWorker(appContext, workerParameters, profiles, server, FakeStreakRepository(StreakEntity()))
            })
            .build()
            .doWork()
    }

    @Test
    fun refusedAsItStands_isNotRetried() {
        for (status in listOf(400, 403, 404, 409, 422)) {
            assertEquals("$status", ListenableWorker.Result.failure(), syncAnswered(SupabaseResult.ServerError(status)))
        }
    }

    @Test
    fun outagesRateLimitsAndExpiredSessions_areRetried() {
        for (status in listOf(401, 408, 429, 500, 502, 503)) {
            assertEquals("$status", ListenableWorker.Result.retry(), syncAnswered(SupabaseResult.ServerError(status)))
        }
        assertEquals(ListenableWorker.Result.retry(), syncAnswered(SupabaseResult.ServerError(null)))
        assertEquals(ListenableWorker.Result.retry(), syncAnswered(SupabaseResult.NetworkError(java.io.IOException("offline"))))
    }
}
