package com.pixelquest.app.data.local

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.data.local.PartyMemberEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.quran.data.local.QuranAyahEntity
import com.pixelquest.app.study.chat.data.local.ChatMessageEntity
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.data.local.StudyCompletionEntity
import com.pixelquest.app.study.data.local.StudyItemEntity
import com.pixelquest.app.study.domain.StudyItemKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * V059: Database migration and Room schema integrity suite.
 * Covers:
 * - Foreign key constraints and CASCADE deletions (Party -> Members, Study Items, Completions, Chat)
 * - Foreign key constraint enforcement preventing orphan child insertion
 * - Unique index constraints (QuranAyah composite uniqueness, NamazLog daily prayer uniqueness)
 * - Complete contiguous version migration coverage (v1 to v23)
 * - In-memory schema stability and DAO accessibility
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RoomSchemaIntegrityTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            })
            .build()

        // Explicitly enable foreign keys on the active connection
        db.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = ON;")
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- 1. Migration Sequence Completeness ---

    @Test
    fun testAllMigrations_contiguousCoverageWithoutGaps() {
        val migrations = AppDatabase.ALL_MIGRATIONS
        assertEquals("Database version 23 must have 22 contiguous migrations", 22, migrations.size)

        for (i in 1 until 23) {
            val migration = migrations[i - 1]
            assertEquals("Migration start version mismatch at step $i", i, migration.startVersion)
            assertEquals("Migration end version mismatch at step $i", i + 1, migration.endVersion)
        }
    }

    // --- 2. DAO Accessibility on Clean In-Memory Database ---

    @Test
    fun testAllDaos_instantiatedAndAccessibleOnInMemoryDatabase() {
        assertNotNull(db.taskDao())
        assertNotNull(db.streakDao())
        assertNotNull(db.userProfileDao())
        assertNotNull(db.difficultySettingsDao())
        assertNotNull(db.taskCompletionLogDao())
        assertNotNull(db.levelHistoryDao())
        assertNotNull(db.insightCacheDao())
        assertNotNull(db.namazLogDao())
        assertNotNull(db.namazSettingsDao())
        assertNotNull(db.daroodDao())
        assertNotNull(db.partyDao())
        assertNotNull(db.quranDao())
        assertNotNull(db.quranBookmarkDao())
        assertNotNull(db.studyDao())
        assertNotNull(db.studyReminderDao())
        assertNotNull(db.chatDao())
        assertNotNull(db.chessDao())
    }

    // --- 3. Foreign Key Cascading Deletions ---

    @Test
    fun testForeignKeyCascade_deletingPartyCascadesToAllChildTables() = runBlocking {
        val partyId = "party_cascade_room_101"

        val party = PartyEntity(
            id = partyId,
            name = "Calculus Study Group",
            leaderId = "leader_alex",
            maxMembers = 20,
            milestone = null,
            milestoneSetAt = null,
            completedAt = null,
            createdAt = 1_700_000_000_000L,
            total = 0L,
            entryCount = 0,
            memberCount = 2,
            lastEntryDate = null,
            myRole = PartyRole.LEADER,
            myStatus = MemberStatus.ACTIVE,
            syncedAt = 1_700_000_000_000L,
            isPublicRace = false,
            partyType = PartyType.STUDY,
        )
        db.partyDao().upsertParties(listOf(party))

        val member = PartyMemberEntity(
            partyId = partyId,
            userId = "user_ben",
            username = "Ben",
            role = PartyRole.MEMBER,
            status = MemberStatus.ACTIVE,
            invitedAt = null,
            joinedAt = 1_700_000_000_000L,
        )
        db.partyDao().insertMembers(listOf(member))

        val studyItem = StudyItemEntity(
            id = "item_limits_1",
            partyId = partyId,
            kind = StudyItemKind.ASSIGNMENT,
            title = "Limits Problem Set",
            subject = "Math",
            notes = "Problems 1-15",
            dueAt = 1_700_100_000_000L,
            pinned = true,
            createdBy = "leader_alex",
            createdAt = 1_700_000_000_000L,
        )
        db.studyDao().upsertItems(listOf(studyItem))

        val completion = StudyCompletionEntity(
            id = 555L,
            partyId = partyId,
            itemId = "item_limits_1",
            userId = "user_ben",
            completedAt = 1_700_050_000_000L,
            localDate = LocalDate.of(2026, 11, 4),
            tz = "UTC",
            clientId = null,
        )
        db.studyDao().upsertCompletions(listOf(completion))

        val chatMessage = ChatMessageEntity(
            id = "msg_chat_1",
            partyId = partyId,
            senderId = "user_ben",
            kind = ChatMessageKind.TEXT,
            body = "Finished problems 1-10!",
            fileId = null,
            replyTo = null,
            clientId = null,
            createdAt = 1_700_060_000_000L,
            deletedAt = null,
            refId = null,
        )
        db.chatDao().upsertMessages(listOf(chatMessage))

        // Verify all entities exist prior to deletion
        assertNotNull(db.partyDao().getParty(partyId))
        assertEquals(1, db.partyDao().observeMembers(partyId).first().size)
        assertEquals(1, db.studyDao().observeItems(partyId).first().size)
        assertEquals(1, db.studyDao().observeCompletions(partyId).first().size)
        assertEquals(1, db.chatDao().messageCount(partyId))

        // Execute party deletion
        db.partyDao().deleteParty(partyId)

        // Verify CASCADE deletion propagated through foreign keys
        assertNull("Party must be deleted", db.partyDao().getParty(partyId))
        assertTrue("Party members must cascade delete", db.partyDao().observeMembers(partyId).first().isEmpty())
        assertTrue("Study items must cascade delete", db.studyDao().observeItems(partyId).first().isEmpty())
        assertTrue("Study completions must cascade delete", db.studyDao().observeCompletions(partyId).first().isEmpty())
        assertEquals("Chat messages must cascade delete", 0, db.chatDao().messageCount(partyId))
    }

    // --- 4. Foreign Key Orphan Prevention ---

    @Test
    fun testForeignKeyOrphanPrevention_insertingChildWithoutParentPartyThrows() = runBlocking {
        val nonExistentPartyId = "orphan_party_404"

        val studyItem = StudyItemEntity(
            id = "orphan_item_1",
            partyId = nonExistentPartyId,
            kind = StudyItemKind.QUIZ,
            title = "Orphan Quiz",
            subject = null,
            notes = null,
            dueAt = null,
            pinned = false,
            createdBy = null,
            createdAt = 1_700_000_000_000L,
        )

        try {
            db.studyDao().upsertItems(listOf(studyItem))
            fail("Inserting study item referencing non-existent partyId must throw SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            assertTrue("Exception message should indicate foreign key constraint", e.message?.contains("FOREIGN KEY", ignoreCase = true) == true)
        }
    }

    // --- 5. Unique Index Constraints ---

    @Test
    fun testUniqueConstraint_quranAyah_throwsOnDuplicateSurahAndAyah() = runBlocking {
        val ayah1 = QuranAyahEntity(
            id = 1,
            surah = 1,
            ayah = 1,
            arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            translationId = "en.sahih",
            translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
            arabicPlain = "بسم الله الرحمن الرحيم",
        )
        db.quranDao().insertAyahs(listOf(ayah1))

        // Duplicate surah=1 and ayah=1 with different id
        val ayahDuplicate = QuranAyahEntity(
            id = 2,
            surah = 1,
            ayah = 1,
            arabic = "بِسْمِ اللَّهِ",
            translationId = "en.sahih",
            translation = "Duplicate test",
            arabicPlain = "بسم الله",
        )

        try {
            db.quranDao().insertAyahs(listOf(ayahDuplicate))
            fail("Inserting ayah with duplicate (surah, ayah) must fail with SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            assertTrue("Exception message should indicate unique constraint", e.message?.contains("UNIQUE", ignoreCase = true) == true)
        }
    }

    @Test
    fun testUniqueConstraint_namazLog_upsertReplacesOnDuplicatePrayerAndDate() = runBlocking {
        val today = LocalDate.of(2026, 11, 4)

        val initialEntry = NamazLogEntity(
            id = 0,
            date = today,
            prayer = Prayer.ASR,
            status = PrayerStatus.MISSED,
            prayedAt = 1_700_000_000_000L,
            xpAwarded = 10,
            bonusXp = 0,
        )
        db.namazLogDao().upsert(initialEntry)

        val retrieved1 = db.namazLogDao().get(today, Prayer.ASR)
        assertNotNull(retrieved1)
        assertEquals(PrayerStatus.MISSED, retrieved1?.status)

        // Updated entry for the same day and prayer with new status
        val updatedEntry = NamazLogEntity(
            id = 0,
            date = today,
            prayer = Prayer.ASR,
            status = PrayerStatus.PRAYED,
            prayedAt = 1_700_001_000_000L,
            xpAwarded = 20,
            bonusXp = 5,
        )
        db.namazLogDao().upsert(updatedEntry)

        val logs = db.namazLogDao().getForDate(today)
        assertEquals("There must be exactly one entry for ASR prayer on the same day", 1, logs.size)
        assertEquals(PrayerStatus.PRAYED, logs.first().status)
        assertEquals(20, logs.first().xpAwarded)
        assertEquals(5, logs.first().bonusXp)
    }
}
