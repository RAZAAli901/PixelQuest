package com.pixelquest.app.party.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import java.time.LocalDate

// Every party's cache on the phone (docs/party-core.md): the parties of any kind the account is in or
// invited to, and their members, as the server last said. The tables keep the names Darood Party gave
// them, so nothing was copied when the party system became shared. Times are epoch millis.

/**
 * A party of any kind the signed-in account is active in or invited to, with its numbers (Darood's
 * own, which stay empty for a study party). Its table keeps its first name, darood_party.
 *
 * @param myRole the account's role in it; [myStatus] its status (active or invited).
 * @param syncedAt when this row last came from the server.
 * @param partyType its kind (from version 17): every party cached before then is a Darood Party.
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
    @ColumnInfo(defaultValue = "0") val isPublicRace: Boolean = false,
    @ColumnInfo(defaultValue = "'DAROOD'") val partyType: PartyType = PartyType.DAROOD
)

/** Someone in a party of any kind, with their part of a Darood Party's total (zero until they post, and in a study party). Its table keeps its first name, darood_party_member. */
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
