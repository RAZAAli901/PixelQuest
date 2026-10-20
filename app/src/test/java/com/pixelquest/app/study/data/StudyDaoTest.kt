package com.pixelquest.app.study.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.local.PendingStudyEntity
import com.pixelquest.app.study.data.local.PendingStudyKind
import com.pixelquest.app.study.data.local.StudyCompletionEntity
import com.pixelquest.app.study.data.local.StudyItemEntity
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The study cache: the order items show in, rows going with their party, and the queue. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyDaoTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val dao = db.studyDao()
    private val day = LocalDate.of(2026, 10, 21)

    @After
    fun tearDown() = db.closeAfterWrites()

    private fun party(id: String) = PartyEntity(
        id, "Physics group", "aria", 10, null, null, null, 0, 0, 0, 2, null, PartyRole.LEADER, MemberStatus.ACTIVE, 0, partyType = PartyType.STUDY
    )

    private fun item(id: String, dueAt: Long?, pinned: Boolean = false, createdAt: Long = 0) =
        StudyItemEntity(id, "p1", StudyItemKind.ASSIGNMENT, "Item $id", null, null, dueAt, pinned, "aria", createdAt)

    @Test
    fun items_pinnedFirst_thenSoonestDue_undatedLast() = runBlocking {
        db.partyDao().upsertParties(listOf(party("p1")))
        assertTrue(dao.replaceItems("p1", listOf(item("late", 300), item("none", null), item("soon", 100), item("pin", 900, pinned = true))))
        assertEquals(listOf("pin", "soon", "late", "none"), dao.observeItems("p1").first().map { it.id })
    }

    @Test
    fun nothingIsWritten_forAPartyNotOnThePhone_andEverythingGoesWithIt() = runBlocking {
        assertFalse(dao.replaceItems("p1", listOf(item("a", null))))
        db.partyDao().upsertParties(listOf(party("p1")))
        dao.replaceItems("p1", listOf(item("a", null)))
        dao.insertPending(PendingStudyEntity("c1", PendingStudyKind.COMPLETION, "p1", itemId = "a", localDate = day, tz = "UTC", createdAt = 1))
        // A refresh of the party keeps its study rows.
        db.partyDao().replaceParties(listOf(party("p1").copy(name = "Physics")))
        assertEquals(1, dao.observeItems("p1").first().size)
        db.partyDao().replaceParties(emptyList())
        assertTrue(dao.observeItems("p1").first().isEmpty())
        assertTrue(dao.pendingToSend().isEmpty())
    }

    @Test
    fun theQueue_sendsInOrder_andASentTickReplacesItsPendingRow() = runBlocking {
        db.partyDao().upsertParties(listOf(party("p1")))
        dao.replaceItems("p1", listOf(item("a", null)))
        dao.insertPending(PendingStudyEntity("c2", PendingStudyKind.SESSION, "p1", minutes = 25, localDate = day, tz = "UTC", createdAt = 5))
        dao.insertPending(PendingStudyEntity("c1", PendingStudyKind.COMPLETION, "p1", itemId = "a", localDate = day, tz = "UTC", createdAt = 1))
        assertEquals(listOf("c2", "c1"), dao.pendingToSend().map { it.clientId })
        assertEquals("c1", dao.pendingCompletionOf("a")?.clientId)

        dao.confirmCompletionSent("c1", StudyCompletionEntity(7, "p1", "a", "aria", 10, day, "UTC", "c1"))
        assertEquals(listOf(7L), dao.observeCompletions("p1").first().map { it.id })
        assertEquals(listOf("c2"), dao.pendingToSend().map { it.clientId })

        // An item gone takes its ticks with it.
        dao.removeItem("a")
        assertTrue(dao.observeCompletions("p1").first().isEmpty())
    }
}
