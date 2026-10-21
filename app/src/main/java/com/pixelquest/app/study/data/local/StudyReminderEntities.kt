package com.pixelquest.app.study.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.SyncState
import java.time.LocalDate

// Study reminders on the phone (docs/study-reminders.md, On the phone), from version 19: the server's
// rows, what's waiting to be sent, and which alerts this phone was told are done. Times are epoch
// millis; alert offsets are minutes, comma separated (null for the defaults).

/** A reminder the server has. */
@Entity(
    tableName = "study_reminder",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId")]
)
data class StudyReminderEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val createdBy: String?,
    val title: String,
    val kind: ReminderKind,
    val scope: ReminderScope,
    val dueAt: Long?,
    val weekStart: LocalDate?,
    val tz: String,
    val repeat: ReminderRepeat,
    val audience: ReminderAudience,
    val alertOffsets: String?,
    val createdAt: Long
)

/** What a pending reminder change is. */
enum class PendingReminderOp { CREATE, EDIT, DELETE }

/**
 * A reminder made, changed or deleted on this phone, not yet on the server. A CREATE has no
 * [reminderId] until the server gives it one; an EDIT or DELETE names the reminder. The draft's
 * fields are empty for a DELETE. [state] is PENDING until sent, or FAILED if refused ([refusal]).
 */
@Entity(
    tableName = "study_reminder_pending",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId")]
)
data class PendingStudyReminderEntity(
    @PrimaryKey val clientId: String,
    val op: PendingReminderOp,
    val partyId: String,
    val reminderId: String? = null,
    val title: String? = null,
    val kind: ReminderKind? = null,
    val scope: ReminderScope? = null,
    val dueAt: Long? = null,
    val weekStart: LocalDate? = null,
    val tz: String? = null,
    val repeat: ReminderRepeat? = null,
    val audience: ReminderAudience? = null,
    val alertOffsets: String? = null,
    val createdAt: Long,
    val state: SyncState = SyncState.PENDING,
    @ColumnInfo(defaultValue = "0") val attempts: Int = 0,
    val refusal: String? = null
)

/**
 * An occurrence of a reminder marked done on this phone (the notification's Mark done): the rest
 * of its alerts don't fire here. It isn't shared, and it isn't tied to the cached row, which a sync
 * replaces; old ones are pruned.
 */
@Entity(tableName = "study_reminder_done", primaryKeys = ["reminderId", "occurrenceAt"])
data class StudyReminderDoneEntity(
    val reminderId: String,
    val occurrenceAt: Long,
    val doneAt: Long
)
