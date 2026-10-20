package com.pixelquest.app.darood.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import java.time.LocalDate

// Darood Party's cache on the phone (docs/darood-design.md): what the server last said, so the
// screens open at once and offline, plus the entries waiting to be sent. The server is the truth;
// a refresh replaces these rows. Times are epoch millis; days use the app's LocalDate converter.

/**
 * A party the signed-in account is active in or invited to, with its numbers.
 *
 * @param myRole the account's role in it; [myStatus] its status (active or invited).
 * @param syncedAt when this row last came from the server.
 */
@Entity(tableName = "darood_party")
data class PartyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val leaderId: String?,
    val maxMembers: Int,
    val milestone: Long?,
    val milestoneSetAt: Long?,
    val completedAt: Long?,
    val createdAt: Long,
    val total: Long,
    val entryCount: Int,
    val memberCount: Int,
    val lastEntryDate: LocalDate?,
    val myRole: PartyRole,
    val myStatus: MemberStatus,
    val syncedAt: Long,
    /** On the race board (from version 12). */
    @ColumnInfo(defaultValue = "0") val isPublicRace: Boolean = false
)

/** Someone in a party, with their part of its total (zero until they post). */
@Entity(
    tableName = "darood_party_member",
    primaryKeys = ["partyId", "userId"],
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId")]
)
data class PartyMemberEntity(
    val partyId: String,
    val userId: String,
    val username: String?,
    val role: PartyRole,
    val status: MemberStatus,
    val invitedAt: Long?,
    val joinedAt: Long?,
    @ColumnInfo(defaultValue = "0") val total: Long = 0,
    @ColumnInfo(defaultValue = "0") val activeDays: Int = 0,
    val lastEntryDate: LocalDate? = null,
    /** When their row last changed on the server (from version 11). */
    val statusChangedAt: Long? = null
)

/** An entry the server has, by its id. [clientId] is set for the account's own, matching its pending row. */
@Entity(
    tableName = "darood_entry",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["partyId", "id"]), Index(value = ["clientId"])]
)
data class DaroodEntryEntity(
    @PrimaryKey val id: Long,
    val partyId: String,
    val userId: String?,
    val amount: Int,
    val localDate: LocalDate,
    val tz: String,
    val createdAt: Long,
    val clientId: String? = null
)

/**
 * An entry posted on this phone and not yet on the server: saved first, then sent, so nothing
 * typed is lost to a bad connection. [state] is PENDING until it's sent, or FAILED if the server
 * refused it ([refusal], its token). [clientId] is its key on both sides, so a retry can't count it twice.
 */
@Entity(
    tableName = "darood_pending_entry",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("partyId")]
)
data class PendingEntryEntity(
    @PrimaryKey val clientId: String,
    val partyId: String,
    val amount: Int,
    val localDate: LocalDate,
    val tz: String,
    val createdAt: Long,
    val state: EntryState = EntryState.PENDING,
    @ColumnInfo(defaultValue = "0") val attempts: Int = 0,
    val refusal: String? = null
)
