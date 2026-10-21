package com.pixelquest.app.study.data.remote

import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** A reminder's rows as the server sends and takes them (docs/study-reminders.md). */
class StudyReminderDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aRowFromTheServer_becomesAReminder() {
        val row = """{"id":"r1","party_id":"p1","created_by":"u1","title":"Quiz this week","kind":"quiz","scope":"week",
            "due_at":null,"week_start":"2026-10-19","tz":"Asia/Karachi","repeat":"weekly","audience":"party",
            "alert_offsets":[60,1440],"created_at":"2026-10-21T05:00:00+00:00"}"""
        val reminder = json.decodeFromString<StudyReminderDto>(row).toDomain()

        assertEquals(ReminderKind.QUIZ, reminder.kind)
        assertEquals(ReminderScope.WEEK, reminder.scope)
        assertEquals(LocalDate.of(2026, 10, 19), reminder.weekStart)
        assertNull(reminder.dueAt)
        assertEquals(ReminderRepeat.WEEKLY, reminder.repeat)
        assertEquals(ReminderAudience.PARTY, reminder.audience)
        assertEquals(listOf(60, 1440), reminder.alertOffsets)
        assertEquals(Instant.parse("2026-10-21T05:00:00Z"), reminder.createdAt)
    }

    @Test
    fun aDraft_isSentWithoutWhoOrWhen_andTrimmed() {
        val draft = StudyReminderDraft(
            title = "  Exam on Thursday ", kind = ReminderKind.EXAM, scope = ReminderScope.DAY,
            dueAt = Instant.parse("2026-10-22T19:00:00Z"), weekStart = null, tz = "Asia/Karachi", audience = ReminderAudience.ME
        )
        val sent = json.encodeToString(draft.toNewDto("p1"))

        assertEquals("Exam on Thursday", draft.toNewDto("p1").title)
        assertFalse("created_by is the server's", sent.contains("created_by"))
        assertFalse("created_at is the server's", sent.contains("created_at"))
        assertEquals("me", draft.toNewDto("p1").audience)
        // An edit never carries the party, creator or audience.
        val edit = json.encodeToString(draft.toEditDto())
        for (field in listOf("party_id", "created_by", "audience")) assertFalse(field, edit.contains(field))
    }

    @Test
    fun unknownWords_fallBackSafely() {
        assertEquals(ReminderKind.CUSTOM, ReminderKind.fromWire("lecture"))
        assertEquals(ReminderRepeat.NONE, ReminderRepeat.fromWire(null))
        assertEquals(ReminderAudience.PARTY, ReminderAudience.fromWire("everyone"))
    }
}
