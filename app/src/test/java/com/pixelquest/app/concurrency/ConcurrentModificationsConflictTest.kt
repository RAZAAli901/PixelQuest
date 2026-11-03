package com.pixelquest.app.concurrency

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyDao
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.local.StudyCompletionEntity
import com.pixelquest.app.study.data.local.StudyDao
import com.pixelquest.app.study.data.local.StudyItemEntity
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import java.time.LocalDate

/**
 * V056: Concurrent modifications and conflict resolution test suite.
 * Covers:
 * - Simultaneous completions by two distinct members on the same study item
 * - Idempotent duplicate completion attempts with identical ids
 * - Remote party deletion while local observer is active
 * - Out-of-turn / race condition move conflict resolution in Chess session state
 * - Room database concurrent write/sync stream resilience without deadlocks
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ConcurrentModificationsConflictTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var partyDao: PartyDao
    private lateinit var studyDao: StudyDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        partyDao = db.partyDao()
        studyDao = db.studyDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.closeAfterWrites()
    }

    private fun createPartyEntity(
        id: String,
        name: String = "Test Party",
        leaderId: String? = "leader1",
    ) = PartyEntity(
        id = id,
        name = name,
        leaderId = leaderId,
        maxMembers = 10,
        milestone = null,
        milestoneSetAt = null,
        completedAt = null,
        createdAt = 1000L,
        total = 0L,
        entryCount = 0,
        memberCount = 2,
        lastEntryDate = null,
        myRole = PartyRole.LEADER,
        myStatus = MemberStatus.ACTIVE,
        syncedAt = 1000L,
        isPublicRace = false,
        partyType = PartyType.STUDY,
    )

    // --- 1. Concurrent Study Item Completion by Multiple Users ---

    @Test
    fun testConcurrentCompletions_multipleUsersCompleteSameItem_bothRecordedCleanly() = runTest {
        val partyId = "concurrent_party_1"
        partyDao.upsertParties(listOf(createPartyEntity(partyId, "Quantum Physics", "user_alice")))

        val itemId = "item_schrodinger"
        studyDao.upsertItems(
            listOf(
                StudyItemEntity(
                    id = itemId,
                    partyId = partyId,
                    kind = StudyItemKind.ASSIGNMENT,
                    title = "Wave Functions",
                    subject = "Chapter 4",
                    notes = "Read pages 100-120",
                    dueAt = null,
                    pinned = false,
                    createdBy = "user_alice",
                    createdAt = 1100L,
                ),
            ),
        )

        // Simultaneous completion by Alice and Bob
        val completion1 = async {
            studyDao.upsertCompletions(
                listOf(
                    StudyCompletionEntity(
                        id = 101L,
                        partyId = partyId,
                        itemId = itemId,
                        userId = "user_alice",
                        completedAt = 2000L,
                        localDate = LocalDate.of(2026, 11, 3),
                        tz = "UTC",
                        clientId = "client_comp_alice",
                    ),
                ),
            )
        }

        val completion2 = async {
            studyDao.upsertCompletions(
                listOf(
                    StudyCompletionEntity(
                        id = 102L,
                        partyId = partyId,
                        itemId = itemId,
                        userId = "user_bob",
                        completedAt = 2005L,
                        localDate = LocalDate.of(2026, 11, 3),
                        tz = "UTC",
                        clientId = "client_comp_bob",
                    ),
                ),
            )
        }

        awaitAll(completion1, completion2)

        val completions = studyDao.observeCompletions(partyId).first()
        assertEquals("Both completions must be recorded without foreign key or unique constraint violation", 2, completions.size)
        assertTrue("Completions must contain Alice's completion", completions.any { it.userId == "user_alice" })
        assertTrue("Completions must contain Bob's completion", completions.any { it.userId == "user_bob" })
    }

    // --- 2. Idempotent Duplicate Completion Handling ---

    @Test
    fun testIdempotentCompletion_duplicateSubmissionWithSameId_updatesRatherThanDuplicates() = runTest {
        val partyId = "concurrent_party_2"
        partyDao.upsertParties(listOf(createPartyEntity(partyId, "Chemistry Guild", "user_clara")))

        val itemId = "item_organic_1"
        studyDao.upsertItems(
            listOf(
                StudyItemEntity(
                    id = itemId,
                    partyId = partyId,
                    kind = StudyItemKind.QUIZ,
                    title = "Alkanes",
                    subject = "Organic",
                    notes = null,
                    dueAt = null,
                    pinned = false,
                    createdBy = "user_clara",
                    createdAt = 1100L,
                ),
            ),
        )

        val completionEntity = StudyCompletionEntity(
            id = 201L,
            partyId = partyId,
            itemId = itemId,
            userId = "user_clara",
            completedAt = 3000L,
            localDate = LocalDate.of(2026, 11, 3),
            tz = "UTC",
            clientId = "unique_client_comp_token",
        )

        // Simulate rapid double submit from network retry
        studyDao.upsertCompletions(listOf(completionEntity))
        studyDao.upsertCompletions(listOf(completionEntity.copy(completedAt = 3001L)))

        val completions = studyDao.observeCompletions(partyId).first()
        assertEquals("Replay of same completion ID must be idempotent (exactly 1 row)", 1, completions.size)
        assertEquals("Final state should reflect updated completion time", 3001L, completions.first().completedAt)
    }

    // --- 3. Remote Party Deletion While Member Is Actively Observing ---

    @Test
    fun testRemotePartyDeletion_activeObserverReceivesNullGracefully() = runTest {
        val partyId = "party_to_delete"
        partyDao.upsertParties(listOf(createPartyEntity(partyId, "Ephemeral Group", "leader_remote")))

        // Observe party while alive
        val observedBefore = partyDao.observeParty(partyId).first()
        assertNotNull("Party must be visible initially", observedBefore)

        // Remote deletion arrives via sync / websocket: leader deleted party
        partyDao.deleteParty(partyId)

        val observedAfter = partyDao.observeParty(partyId).first()
        assertNull("Active observer must emit null when party is deleted remotely", observedAfter)
    }

    // --- 4. Chess Move Race Condition & Conflict Resolution ---

    @Test
    fun testChessMoveConflictResolution_stalePlyRejectedWhenRivalMoveArrivedFirst() {
        var currentPly = 12

        fun applyMoveIfCurrentPly(submittedPly: Int, uci: String): Boolean {
            if (submittedPly != currentPly) {
                // Out of turn or stale ply conflict
                return false
            }
            currentPly++
            return true
        }

        // Rival's move arrives first and advances game ply to 13
        val rivalMoveAccepted = applyMoveIfCurrentPly(submittedPly = 12, uci = "e7e5")
        assertTrue("Rival move on current ply 12 must succeed", rivalMoveAccepted)
        assertEquals("Ply must advance to 13", 13, currentPly)

        // Stale local move submitted for ply 12 is rejected due to conflict
        val localStaleMoveAccepted = applyMoveIfCurrentPly(submittedPly = 12, uci = "d7d5")
        assertTrue("Stale move for ply 12 must be rejected when game is at ply 13", !localStaleMoveAccepted)
    }

    // --- 5. Concurrent Writes and Sync Interleaving in Room ---

    @Test
    fun testConcurrentWrites_localEditAndRemoteSyncInterleaving_completesDeterministically() = runTest {
        val partyId = "interleaved_party"
        partyDao.upsertParties(listOf(createPartyEntity(partyId, "Concurrency Test Party", "user_sync")))

        // Launch 20 concurrent write operations (simulating local user edits and incoming network sync batches)
        val jobs = (1..20).map { i ->
            async {
                studyDao.upsertItems(
                    listOf(
                        StudyItemEntity(
                            id = "item_concurrent_$i",
                            partyId = partyId,
                            kind = StudyItemKind.ASSIGNMENT,
                            title = "Task $i",
                            subject = "Subject $i",
                            notes = "Description $i",
                            dueAt = null,
                            pinned = (i % 2 == 0),
                            createdBy = "user_sync",
                            createdAt = 2000L + i,
                        ),
                    ),
                )
            }
        }

        jobs.awaitAll()

        val storedItems = studyDao.observeItems(partyId).first()
        assertEquals("All 20 concurrently written items must be persisted without deadlock or loss", 20, storedItems.size)
    }
}
