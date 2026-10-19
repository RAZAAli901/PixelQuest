package com.pixelquest.app.darood.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The sync worker sends what's waiting, and asks to run again only while something can't go yet. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodSyncWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val server = FakeDaroodServer(me = "me").apply { usernames["me"] = "aria_k" }
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, { 1_000L }, { java.time.ZoneId.of("UTC") })

    @After
    fun tearDown() {
        db.close()
    }

    private fun worker() = TestListenableWorkerBuilder<DaroodSyncWorker>(context)
        .setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                DaroodSyncWorker(appContext, workerParameters, repository)
        })
        .build()

    @Test
    fun offline_itRetries_thenSendsEverythingInOrder() = runBlocking {
        val friday = server.seedParty("Friday circle", "me")
        repository.refreshParties()
        server.offline = true
        repository.addEntry(friday, 33)
        repository.addEntry(friday, 11)

        assertEquals(ListenableWorker.Result.retry(), worker().doWork())

        server.offline = false
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(listOf(33, 11), server.entries.map { it.amount })
    }

    @Test
    fun aRefusedEntry_doesntKeepItRetrying() = runBlocking {
        val friday = server.seedParty("Friday circle", "me")
        repository.refreshParties()
        server.offline = true
        repository.addEntry(friday, 100_000)
        server.parties[friday] = server.parties.getValue(friday).copy(completedAt = "2026-10-19T09:00:00+00:00")
        server.offline = false

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(0, server.entries.size)
    }
}
