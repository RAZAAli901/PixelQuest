package com.pixelquest.app.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.sync.DaroodSyncWorker
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.data.repository.PendingLeaderboardChoice
import com.pixelquest.app.data.repository.PendingProgressReset
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.chat.data.ChatSend
import com.pixelquest.app.study.chat.data.StudyChatRepositoryImpl
import com.pixelquest.app.study.chat.sync.StudyChatSendWorker
import com.pixelquest.app.study.data.ReminderSave
import com.pixelquest.app.study.data.StudyReminderRepositoryImpl
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.StudyReminderDraft
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.sync.StudyReminderSyncWorker
import com.pixelquest.app.study.sync.StudySyncWorker
import com.pixelquest.app.testing.FakeChatServer
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyReminderServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.worker.FakeOfflineUserProfileRepo
import com.pixelquest.app.worker.ProfileSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

/**
 * V051: Comprehensive test suite for offline and background sync behaviour
 * across all screens and domains that queue work (Darood, Study items, Study reminders,
 * Study chat, and Profile/Leaderboard).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OfflineAndSyncBehaviorTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private val zone = ZoneId.of("UTC")

    private val daroodServer = FakeDaroodServer(me = "player1").apply {
        usernames["player1"] = "Knight1"
        usernames["player2"] = "Knight2"
    }
    private val studyServer = FakeStudyServer(daroodServer)
    private val reminderServer = FakeStudyReminderServer(me = "player1")
    private val chatServer = FakeChatServer(me = "player1")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.closeAfterWrites()
    }

    @Test
    fun daroodCircle_offlineEntryQueuesLocally_syncWorkerRetries_drainsInOrderWhenReconnected() = runBlocking {
        val partyId = daroodServer.seedParty("Circle of Light", "player1")
        val repo = DaroodPartyRepositoryImpl(
            remote = daroodServer,
            dao = db.daroodDao(),
            account = { daroodServer.me },
            now = { daroodServer.now.toEpochMilli() },
            zone = { zone },
        )
        repo.refreshParties()

        // 1. Enter offline state
        daroodServer.offline = true

        // 2. Queue multiple entries while disconnected
        repo.addEntry(partyId, 33)
        repo.addEntry(partyId, 66)

        // Verify pending rows in local database
        val pendingRows = db.daroodDao().pendingToSend()
        assertEquals(2, pendingRows.size)
        assertEquals(33, pendingRows[0].amount)
        assertEquals(66, pendingRows[1].amount)

        // 3. Worker triggers while still offline -> yields Retry
        val workerOffline = TestListenableWorkerBuilder<DaroodSyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    DaroodSyncWorker(appContext, params, repo)
            }).build()

        assertEquals(ListenableWorker.Result.retry(), workerOffline.doWork())
        // Pending rows must still remain waiting
        assertEquals(2, db.daroodDao().pendingToSend().size)

        // 4. Network restored -> Worker yields Success and drains queue in FIFO order
        daroodServer.offline = false
        val workerOnline = TestListenableWorkerBuilder<DaroodSyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    DaroodSyncWorker(appContext, params, repo)
            }).build()

        assertEquals(ListenableWorker.Result.success(), workerOnline.doWork())
        assertEquals(0, db.daroodDao().pendingToSend().size)
        assertEquals(listOf(33, 66), daroodServer.entries.map { it.amount })
    }

    @Test
    fun studyPartyItems_offlineCompletion_stagesLocally_syncWorkerRetries_syncsWhenOnline() = runBlocking {
        val partyId = daroodServer.seedParty("Math Study Circle", "player1", type = PartyType.STUDY)
        // Ensure local Room has party row so foreign keys are satisfied
        db.partyDao().upsertParties(
            listOf(
                PartyEntity(
                    id = partyId, name = "Math Study Circle", leaderId = "player1", maxMembers = 10,
                    milestone = null, milestoneSetAt = null, completedAt = null, createdAt = 0, total = 0,
                    entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.LEADER,
                    myStatus = MemberStatus.ACTIVE, syncedAt = 1000L, isPublicRace = false, partyType = PartyType.STUDY,
                ),
            ),
        )
        val studyItem = studyServer.seedItem(partyId, "player1", "Calculus Chapter 1", StudyItemKind.ASSIGNMENT)

        val repo = StudyRepositoryImpl(
            remote = studyServer,
            dao = db.studyDao(),
            parties = db.partyDao(),
            account = { daroodServer.me },
            clock = { daroodServer.now.toEpochMilli() },
            timeZone = { zone },
        )
        repo.refresh(partyId)

        // 1. Disconnect
        daroodServer.offline = true

        // 2. Mark item completed offline
        repo.complete(studyItem)

        // Locally recorded and staged in study_pending
        val pendingBefore = db.studyDao().pendingToSend()
        assertEquals(1, pendingBefore.size)
        assertEquals(studyItem.id, pendingBefore[0].itemId)

        // 3. SyncWorker while offline returns retry
        val workerOffline = TestListenableWorkerBuilder<StudySyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    StudySyncWorker(appContext, params, repo)
            }).build()

        assertEquals(ListenableWorker.Result.retry(), workerOffline.doWork())
        assertEquals(1, db.studyDao().pendingToSend().size)

        // 4. Restore connectivity -> Worker succeeds and flushes pending queue
        daroodServer.offline = false
        val workerOnline = TestListenableWorkerBuilder<StudySyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    StudySyncWorker(appContext, params, repo)
            }).build()

        assertEquals(ListenableWorker.Result.success(), workerOnline.doWork())
        assertEquals(0, db.studyDao().pendingToSend().size)
        assertTrue(studyServer.completions.any { it.itemId == studyItem.id && it.userId == "player1" })
    }

    @Test
    fun studyReminders_offlineCreation_armsLocalAlarm_syncWorkerRetries_syncsWhenOnline() = runBlocking {
        val partyId = "study_party_reminders"
        db.partyDao().upsertParties(
            listOf(
                PartyEntity(
                    id = partyId, name = "Reminder Circle", leaderId = "player1", maxMembers = 10,
                    milestone = null, milestoneSetAt = null, completedAt = null, createdAt = 0, total = 0,
                    entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.LEADER,
                    myStatus = MemberStatus.ACTIVE, syncedAt = 1000L, isPublicRace = false, partyType = PartyType.STUDY,
                ),
            ),
        )

        var sendsScheduled = 0
        val repo = StudyReminderRepositoryImpl(
            remote = reminderServer,
            dao = db.studyReminderDao(),
            account = { reminderServer.me },
            clock = { reminderServer.now.toEpochMilli() },
            sendLater = { sendsScheduled++ },
        )

        // 1. Server offline
        reminderServer.online = false

        val draft = StudyReminderDraft(
            title = "Physics Midterm",
            kind = ReminderKind.EXAM,
            scope = ReminderScope.TIME,
            dueAt = Instant.parse("2026-11-10T09:00:00Z"),
            weekStart = null,
            tz = "UTC",
        )

        val saveResult = repo.create(partyId, draft)
        assertTrue(saveResult is ReminderSave.Waiting)
        assertTrue(sendsScheduled > 0)

        // Staged locally in study_reminder_pending
        val pendingCount = db.studyReminderDao().pendingToSend().size
        assertEquals(1, pendingCount)

        // 2. StudyReminderSyncWorker retries when offline
        val workerOffline = TestListenableWorkerBuilder<StudyReminderSyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    StudyReminderSyncWorker(appContext, params, repo)
            }).build()

        assertEquals(ListenableWorker.Result.retry(), workerOffline.doWork())

        // 3. Online restored -> Worker succeeds and syncs reminder to remote
        reminderServer.online = true
        val workerOnline = TestListenableWorkerBuilder<StudyReminderSyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    StudyReminderSyncWorker(appContext, params, repo)
            }).build()

        assertEquals(ListenableWorker.Result.success(), workerOnline.doWork())
        assertEquals(0, db.studyReminderDao().pendingToSend().size)
        assertTrue(reminderServer.rows.any { it.title == "Physics Midterm" })
    }

    @Test
    fun studyChat_offlineText_storedInPendingOutbox_syncWorkerRetries_sendsChronologically() = runBlocking {
        val partyId = "study_party_chat"
        chatServer.members[partyId] = mutableSetOf("player1", "player2")
        db.partyDao().upsertParties(
            listOf(
                PartyEntity(
                    id = partyId, name = "Chat Party", leaderId = "player1", maxMembers = 10,
                    milestone = null, milestoneSetAt = null, completedAt = null, createdAt = 0, total = 0,
                    entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.LEADER,
                    myStatus = MemberStatus.ACTIVE, syncedAt = 1000L, isPublicRace = false, partyType = PartyType.STUDY,
                ),
            ),
        )

        var sendsScheduled = 0
        val repo = StudyChatRepositoryImpl(
            remote = chatServer,
            dao = db.chatDao(),
            account = { chatServer.me },
            clock = { System.currentTimeMillis() },
            sendLater = { sendsScheduled++ },
        )

        // 1. Offline chat messages
        chatServer.online = false

        val send1 = repo.send(partyId, "First offline note")
        val send2 = repo.send(partyId, "Second offline note")
        assertEquals(ChatSend.Waiting, send1)
        assertEquals(ChatSend.Waiting, send2)

        // Messages visible locally in observeMessages with sync = PENDING
        val messages = repo.observeMessages(partyId, limit = 50).first()
        assertEquals(2, messages.size)
        assertEquals("First offline note", messages[0].body)
        assertEquals("Second offline note", messages[1].body)
        assertEquals(SyncState.PENDING, messages[0].sync)
        assertEquals(SyncState.PENDING, messages[1].sync)

        // 2. Chat worker retries while offline
        val workerOffline = TestListenableWorkerBuilder<StudyChatSendWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    StudyChatSendWorker(appContext, params, repo)
            }).build()

        assertEquals(ListenableWorker.Result.retry(), workerOffline.doWork())

        // 3. Online restored -> Worker succeeds and messages are confirmed on server
        chatServer.online = true
        val workerOnline = TestListenableWorkerBuilder<StudyChatSendWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    StudyChatSendWorker(appContext, params, repo)
            }).build()

        assertEquals(ListenableWorker.Result.success(), workerOnline.doWork())
        val serverMsgs = chatServer.messages.filter { it.partyId == partyId }
        assertEquals(2, serverMsgs.size)
        assertEquals("First offline note", serverMsgs[0].body)
        assertEquals("Second offline note", serverMsgs[1].body)

        // Outbox pending items cleared
        val remainingPending = db.chatDao().pendingToSend()
        assertEquals(0, remainingPending.size)
    }

    @Test
    fun profileAndLeaderboard_offlineOptInOutAndReset_stagedInPendingPrefs_workerSyncsWhenOnline() = runBlocking {
        val sharedPrefs = context.getSharedPreferences("test_sync_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        val pendingChoice = PendingLeaderboardChoice(sharedPrefs)
        val pendingReset = PendingProgressReset(sharedPrefs)

        val accountId = "uuid-player-42"
        val fakeUserRepo = FakeOfflineUserProfileRepo().apply {
            profile = profile.copy(supabaseUserId = accountId, leaderboardOptIn = true, leaderboardDisplayName = "OfflineWarrior")
            flow.value = profile
        }

        var isCloudOffline = true
        var cloudSyncCalls = 0
        var cloudOptInCalls = 0
        val fakeCloudRepo = object : CloudProfileRepository {
            override suspend fun syncProfileToCloud(): SupabaseResult<Unit> {
                cloudSyncCalls++
                return if (isCloudOffline) {
                    SupabaseResult.NetworkError(IOException("Device offline"), "No internet")
                } else {
                    SupabaseResult.Success(Unit)
                }
            }

            override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String): SupabaseResult<Unit> {
                cloudOptInCalls++
                return if (isCloudOffline) {
                    SupabaseResult.NetworkError(IOException("Device offline"), "No internet")
                } else {
                    pendingChoice.confirmed(accountId, optIn)
                    SupabaseResult.Success(Unit)
                }
            }

            override suspend fun sendProgressAfterReset(): SupabaseResult<Unit> = syncProfileToCloud()
        }

        val fakeStreakRepo = object : StreakRepository {
            private val flow = MutableStateFlow<StreakEntity?>(StreakEntity(id = 1, currentStreak = 10, longestStreak = 15, lastCompletedDate = null))
            override fun getCurrentStreak(): Flow<StreakEntity?> = flow
            override suspend fun insertStreak(streak: StreakEntity) {
                flow.value = streak
            }
            override suspend fun updateStreak(streak: StreakEntity) {
                flow.value = streak
            }
        }

        // 1. User changes choice and resets progress while offline
        pendingChoice.choose(accountId, optIn = true, displayName = "OfflineWarrior")
        pendingReset.mark(accountId)

        assertEquals(true, pendingChoice.pendingFor(accountId)?.optIn)
        assertTrue(pendingReset.isPendingFor(accountId))

        // 2. ProfileSyncWorker runs while offline -> returns Retry
        val workerOffline = TestListenableWorkerBuilder<ProfileSyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    ProfileSyncWorker(appContext, params, fakeUserRepo, fakeCloudRepo, fakeStreakRepo, pendingChoice, pendingReset)
            }).build()

        assertEquals(ListenableWorker.Result.retry(), workerOffline.doWork())
        // Choices must remain pending while offline
        assertNotNull(pendingChoice.pendingFor(accountId))
        assertTrue(pendingReset.isPendingFor(accountId))

        // 3. Connection restored -> Worker returns Success, cloud updated, pending markers cleared
        isCloudOffline = false
        val workerOnline = TestListenableWorkerBuilder<ProfileSyncWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    ProfileSyncWorker(appContext, params, fakeUserRepo, fakeCloudRepo, fakeStreakRepo, pendingChoice, pendingReset)
            }).build()

        assertEquals(ListenableWorker.Result.success(), workerOnline.doWork())
        // Pending choice must be confirmed and cleared
        assertNull(pendingChoice.pendingFor(accountId))
    }
}
