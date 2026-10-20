package com.pixelquest.app.study.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.SyncState
import java.time.LocalDate

// A study party's own cache on the phone (docs/study-party.md): its items, completions, study time
// and scores, and what's waiting to be sent. The party and its members are in party.data.local, and
// every row here goes with its party. The server is the truth; a refresh replaces these rows. Times
// are epoch millis; days use the app's LocalDate converter.

/** An item the server has. */
@Entity(
    tableName = "study_item",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId")]
)
data class StudyItemEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val kind: StudyItemKind,
    val title: String,
    val subject: String?,
    val notes: String?,
    val dueAt: Long?,
    val pinned: Boolean,
    val createdBy: String?,
    val createdAt: Long
)

/**
 * A completion the server has, by its id: every member's, so the party sees who's done what.
 * [clientId] is set for the account's own, matching its pending row.
 */
@Entity(
    tableName = "study_completion",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId"), Index("itemId"), Index("clientId")]
)
data class StudyCompletionEntity(
    @PrimaryKey val id: Long,
    val partyId: String,
    val itemId: String,
    val userId: String?,
    val completedAt: Long,
    val localDate: LocalDate,
    val tz: String,
    val clientId: String? = null
)

/** Study time the server has, by its id: every member's. */
@Entity(
    tableName = "study_session",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId"), Index("clientId")]
)
data class StudySessionEntity(
    @PrimaryKey val id: Long,
    val partyId: String,
    val userId: String?,
    val minutes: Int,
    val localDate: LocalDate,
    val tz: String,
    val createdAt: Long,
    val clientId: String? = null
)

/** A member's points for one of their weeks, as the server counted them. */
@Entity(
    tableName = "study_week_score",
    primaryKeys = ["partyId", "userId", "weekStart"],
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)]
)
data class StudyWeekScoreEntity(
    val partyId: String,
    val userId: String,
    val weekStart: LocalDate,
    val points: Int,
    val itemPoints: Int,
    val bonusPoints: Int,
    val studyPoints: Int,
    val itemsCompleted: Int,
    val studyMinutes: Int
)

/** A closed week's winner (ties all win). */
@Entity(
    tableName = "study_week_winner",
    primaryKeys = ["partyId", "weekStart", "userId"],
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)]
)
data class StudyWeekWinnerEntity(
    val partyId: String,
    val weekStart: LocalDate,
    val userId: String,
    val points: Int
)

/** What a pending row is: a tick on an item, or study time. */
enum class PendingStudyKind { COMPLETION, SESSION }

/**
 * A tick or study time from this phone, not yet on the server: saved first, then sent, so nothing is
 * lost to a bad connection. A completion has its [itemId]; a session its [minutes]. [state] is
 * PENDING until it's sent, or FAILED if the server refused it ([refusal], its token). [clientId] is
 * its key on both sides, so a retry can't count it twice.
 */
@Entity(
    tableName = "study_pending",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId")]
)
data class PendingStudyEntity(
    @PrimaryKey val clientId: String,
    val kind: PendingStudyKind,
    val partyId: String,
    val itemId: String? = null,
    val minutes: Int? = null,
    val localDate: LocalDate,
    val tz: String,
    val createdAt: Long,
    val state: SyncState = SyncState.PENDING,
    @ColumnInfo(defaultValue = "0") val attempts: Int = 0,
    val refusal: String? = null
)
