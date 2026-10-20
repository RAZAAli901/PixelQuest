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
import com.pixelquest.app.party.data.local.PartyEntity

// Darood Party's own cache on the phone (docs/darood-design.md): its entries, and those waiting to be
// sent. Every party's rows (the parties and their members) are in party.data.local. The server is
// the truth; a refresh replaces these rows. Times are epoch millis; days use the app's LocalDate converter.

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
