package com.pixelquest.app.darood.data

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.local.DaroodDao
import com.pixelquest.app.darood.data.local.DaroodEntryEntity
import com.pixelquest.app.darood.data.local.PartyEntity
import com.pixelquest.app.darood.data.local.PartyMemberEntity
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.data.remote.InviteOutcome
import com.pixelquest.app.darood.data.remote.RemoteParty
import com.pixelquest.app.darood.data.remote.parseInstant
import com.pixelquest.app.darood.data.remote.roleOf
import com.pixelquest.app.darood.data.remote.statusOf
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Darood Parties for the screens (docs/darood-design.md). They watch the cache on the phone, so a
 * party opens at once and offline; the refresh calls bring the server's copy into it. A call that
 * changes something on the server refreshes what it changed. Nothing here reaches XP, levels,
 * streaks or the leaderboard.
 */
interface DaroodPartyRepository {
    /** The parties the account is in or invited to, invitations first. */
    fun observeMyParties(): Flow<List<Party>>

    fun observeParty(partyId: String): Flow<Party?>

    fun observeMembers(partyId: String): Flow<List<PartyMember>>

    /** Each poster's part of the party's total, largest first. */
    fun observeMemberStats(partyId: String): Flow<List<MemberStats>>

    /** The newest [limit] entries the phone has for a party, newest first. */
    fun observeEntries(partyId: String, limit: Int): Flow<List<DaroodEntry>>

    /** The account's parties, with their numbers, from the server. */
    suspend fun refreshParties(): SupabaseResult<Unit>

    /** One party from the server: itself, its members, their numbers and its newest entries. */
    suspend fun refreshParty(partyId: String): SupabaseResult<Unit>

    /** The page of entries before the oldest the phone has: true if there may be more. */
    suspend fun loadOlderEntries(partyId: String): SupabaseResult<Boolean>

    /** A new party led by the account: its id. */
    suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String>

    suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome>

    suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus>

    suspend fun leaveParty(partyId: String): SupabaseResult<Unit>

    suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit>

    suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long>
}

@Singleton
class DaroodPartyRepositoryImpl(
    private val remote: DaroodPartyRemoteDataSource,
    private val dao: DaroodDao,
    private val now: () -> Long
) : DaroodPartyRepository {

    @Inject
    constructor(remote: DaroodPartyRemoteDataSource, dao: DaroodDao) : this(remote, dao, System::currentTimeMillis)

    override fun observeMyParties(): Flow<List<Party>> = dao.observeParties().map { parties -> parties.map { it.toDomain() } }

    override fun observeParty(partyId: String): Flow<Party?> = dao.observeParty(partyId).map { it?.toDomain() }

    override fun observeMembers(partyId: String): Flow<List<PartyMember>> =
        dao.observeMembers(partyId).map { members -> members.map { it.toDomain() } }

    override fun observeMemberStats(partyId: String): Flow<List<MemberStats>> =
        dao.observeMembers(partyId).map { members ->
            members.filter { it.total > 0 }.sortedByDescending { it.total }.map {
                MemberStats(it.partyId, it.userId, it.username, it.total, it.activeDays, it.lastEntryDate)
            }
        }

    override fun observeEntries(partyId: String, limit: Int): Flow<List<DaroodEntry>> =
        dao.observeEntries(partyId, limit).map { entries -> entries.map { it.toDomain() } }

    override suspend fun refreshParties(): SupabaseResult<Unit> {
        val mine = when (val result = remote.fetchMyParties()) {
            is SupabaseResult.Success -> result.data
            else -> return result.failure()
        }
        val active = mine.filter { it.membership.status == "active" }.map { it.party.id }
        val stats = when (val result = remote.fetchStats(active)) {
            is SupabaseResult.Success -> result.data.associateBy { it.partyId }
            else -> return result.failure()
        }
        val syncedAt = now()
        dao.replaceParties(mine.map { it.toEntity(stats[it.party.id], syncedAt) })
        return SupabaseResult.Success(Unit)
    }

    override suspend fun refreshParty(partyId: String): SupabaseResult<Unit> {
        val parties = refreshParties()
        if (parties !is SupabaseResult.Success) return parties
        // No longer the account's party (left, removed, deleted): nothing more to bring.
        val party = dao.getParty(partyId) ?: return SupabaseResult.Success(Unit)
        if (party.myStatus != MemberStatus.ACTIVE) return SupabaseResult.Success(Unit)

        val members = when (val result = remote.fetchMembers(partyId)) {
            is SupabaseResult.Success -> result.data
            else -> return result.failure()
        }
        val memberStats = when (val result = remote.fetchMemberStats(partyId)) {
            is SupabaseResult.Success -> result.data.filter { it.userId != null }.associateBy { it.userId }
            else -> return result.failure()
        }
        val entries = when (val result = remote.fetchEntries(partyId)) {
            is SupabaseResult.Success -> result.data
            else -> return result.failure()
        }
        dao.replaceMembers(partyId, members.map { it.toEntity(memberStats[it.userId]) })
        dao.upsertEntries(entries.map { it.toEntity() })
        return SupabaseResult.Success(Unit)
    }

    override suspend fun loadOlderEntries(partyId: String): SupabaseResult<Boolean> {
        val oldest = dao.oldestEntryId(partyId) ?: return SupabaseResult.Success(false)
        return when (val result = remote.fetchEntries(partyId, beforeId = oldest)) {
            is SupabaseResult.Success -> {
                dao.upsertEntries(result.data.map { it.toEntity() })
                SupabaseResult.Success(result.data.size == DaroodPartyRemoteDataSource.PAGE_SIZE)
            }
            else -> result.failure()
        }
    }

    override suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String> =
        remote.createParty(name, maxMembers, milestone).alsoOnSuccess { refreshParties() }

    override suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome> =
        remote.invite(partyId, username).alsoOnSuccess { refreshParty(partyId) }

    override suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus> =
        remote.respondToInvite(partyId, accept).alsoOnSuccess { refreshParty(partyId) }

    override suspend fun leaveParty(partyId: String): SupabaseResult<Unit> =
        remote.leaveParty(partyId).alsoOnSuccess { refreshParties() }

    override suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit> =
        remote.removeMember(partyId, userId).alsoOnSuccess { refreshParty(partyId) }

    override suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long> =
        remote.setMilestone(partyId, target).alsoOnSuccess { refreshParties() }

    private suspend inline fun <T> SupabaseResult<T>.alsoOnSuccess(then: () -> Unit): SupabaseResult<T> {
        if (this is SupabaseResult.Success) then()
        return this
    }
}

/** A failure, as a result of any type. */
@Suppress("UNCHECKED_CAST")
internal fun <T> SupabaseResult<*>.failure(): SupabaseResult<T> = this as SupabaseResult<T>

internal fun RemoteParty.toEntity(stats: PartyStats?, syncedAt: Long) = PartyEntity(
    id = party.id,
    name = party.name,
    leaderId = party.leaderId,
    maxMembers = party.maxMembers,
    milestone = party.milestone,
    milestoneSetAt = party.milestoneSetAt?.let { parseInstant(it).toEpochMilli() },
    completedAt = party.completedAt?.let { parseInstant(it).toEpochMilli() },
    createdAt = parseInstant(party.createdAt).toEpochMilli(),
    total = stats?.total ?: 0,
    entryCount = stats?.entryCount ?: 0,
    memberCount = stats?.memberCount ?: 0,
    lastEntryDate = stats?.lastEntryDate,
    myRole = roleOf(membership.role),
    myStatus = statusOf(membership.status),
    syncedAt = syncedAt
)

internal fun PartyEntity.toDomain() = Party(
    id = id,
    name = name,
    leaderId = leaderId,
    maxMembers = maxMembers,
    milestone = milestone,
    milestoneSetAt = milestoneSetAt?.let(Instant::ofEpochMilli),
    completedAt = completedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    total = total,
    memberCount = memberCount,
    myStatus = myStatus
)

internal fun PartyMember.toEntity(stats: MemberStats?) = PartyMemberEntity(
    partyId = partyId,
    userId = userId,
    username = username ?: stats?.username,
    role = role,
    status = status,
    invitedAt = invitedAt?.toEpochMilli(),
    joinedAt = joinedAt?.toEpochMilli(),
    total = stats?.total ?: 0,
    activeDays = stats?.activeDays ?: 0,
    lastEntryDate = stats?.lastEntryDate
)

internal fun PartyMemberEntity.toDomain() = PartyMember(
    partyId = partyId,
    userId = userId,
    username = username,
    role = role,
    status = status,
    invitedAt = invitedAt?.let(Instant::ofEpochMilli),
    joinedAt = joinedAt?.let(Instant::ofEpochMilli)
)

internal fun DaroodEntry.toEntity() = DaroodEntryEntity(
    id = requireNotNull(id) { "Only entries the server has are cached by id" },
    partyId = partyId,
    userId = userId,
    amount = amount,
    localDate = localDate,
    tz = tz,
    createdAt = createdAt.toEpochMilli(),
    clientId = clientId
)

internal fun DaroodEntryEntity.toDomain() = DaroodEntry(
    id = id,
    clientId = clientId,
    partyId = partyId,
    userId = userId,
    amount = amount,
    localDate = localDate,
    tz = tz,
    createdAt = Instant.ofEpochMilli(createdAt),
    state = EntryState.SYNCED
)
