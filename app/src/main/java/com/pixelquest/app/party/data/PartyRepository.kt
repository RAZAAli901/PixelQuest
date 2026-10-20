package com.pixelquest.app.party.data

import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.PartyStats
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.data.local.PartyDao
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.data.local.PartyMemberEntity
import com.pixelquest.app.party.data.remote.InviteOutcome
import com.pixelquest.app.party.data.remote.PartyRemoteDataSource
import com.pixelquest.app.party.data.remote.RemoteParty
import com.pixelquest.app.party.data.remote.parseInstant
import com.pixelquest.app.party.data.remote.roleOf
import com.pixelquest.app.party.data.remote.statusOf
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.party.sync.PartyLiveSource
import kotlinx.coroutines.flow.channelFlow

/**
 * Every party, of any kind, for the screens (docs/party-core.md): the parties the account is in or
 * invited to, their members, and what anyone does about membership. They watch the cache on the
 * phone, so a party opens at once and offline; the refresh calls bring the server's copy into it. A
 * call that changes something on the server refreshes what it changed. Nothing here reaches XP,
 * levels, streaks or the leaderboard.
 */
interface PartyRepository {
    /** The parties the account is in or invited to, invitations first. */
    fun observeMyParties(): Flow<List<Party>>

    /** The account's parties of one kind, invitations first: a kind's own list shows only its own. */
    fun observeMyParties(type: PartyType): Flow<List<Party>> = observeMyParties().map { parties -> parties.filter { it.type == type } }

    fun observeParty(partyId: String): Flow<Party?>

    fun observeMembers(partyId: String): Flow<List<PartyMember>>

    /** The account's parties, with their numbers, from the server. */
    suspend fun refreshParties(): SupabaseResult<Unit>

    /** One party from the server: itself, its members, their numbers and its newest entries. */
    suspend fun refreshParty(partyId: String): SupabaseResult<Unit>

    suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome>

    suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus>

    suspend fun leaveParty(partyId: String): SupabaseResult<Unit>

    suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit>

    suspend fun renameParty(partyId: String, name: String): SupabaseResult<Unit>

    suspend fun transferLeadership(partyId: String, userId: String): SupabaseResult<Unit>

    suspend fun deleteParty(partyId: String): SupabaseResult<Unit>

    /** A new party of [type] led by the account, with its name and size only: its id. */
    suspend fun createParty(name: String, maxMembers: Int, type: PartyType): SupabaseResult<String>

    /**
     * The account's invitations, live, while collected: whenever one arrives or is taken back (to a
     * party of any kind), the parties refresh, and it passes the news on. Collect it while a screen
     * shows the invitations. Nothing when signed out, or with no live source.
     */
    fun followInvitations(): Flow<Unit>
}

/**
 * The shared core's repository. A kind with data of its own extends it ([refreshOwn]): Darood
 * Party's brings its entries when one of its parties refreshes.
 */
@Singleton
open class PartyRepositoryImpl(
    private val remote: PartyRemoteDataSource,
    private val dao: PartyDao,
    private val now: () -> Long,
    private val live: PartyLiveSource? = null,
    private val accountId: () -> String? = { null }
) : PartyRepository {

    @Inject
    constructor(remote: PartyRemoteDataSource, dao: PartyDao, live: PartyLiveSource, account: DaroodAccount) :
        this(remote, dao, System::currentTimeMillis, live, account::id)

    override fun followInvitations(): Flow<Unit> = channelFlow {
        val source = live ?: return@channelFlow
        val me = accountId() ?: return@channelFlow
        // Listening has started, or something changed: either way, the parties as the server has them now.
        source.invitations(me).collect {
            refreshParties()
            send(Unit)
        }
    }

    override fun observeMyParties(): Flow<List<Party>> = dao.observeParties().map { parties -> parties.map { it.toDomain() } }

    override fun observeParty(partyId: String): Flow<Party?> = dao.observeParty(partyId).map { it?.toDomain() }

    override fun observeMembers(partyId: String): Flow<List<PartyMember>> =
        dao.observeMembers(partyId).map { members -> members.map { it.toDomain() } }

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
        // The party may have gone while these were on their way (its leader deleted it): then they're dropped.
        if (!dao.replaceMembersIfKept(partyId, members.map { it.toEntity(memberStats[it.userId]) })) return SupabaseResult.Success(Unit)
        return refreshOwn(party)
    }

    /** What a party's kind brings when the party refreshes, once its members are in: nothing, for the core. */
    protected open suspend fun refreshOwn(party: PartyEntity): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    override suspend fun createParty(name: String, maxMembers: Int, type: PartyType): SupabaseResult<String> =
        remote.createParty(name, maxMembers, type).alsoOnSuccess { refreshParties() }

    override suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome> =
        remote.invite(partyId, username).alsoOnSuccess { refreshParty(partyId) }

    override suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus> =
        remote.respondToInvite(partyId, accept).alsoOnSuccess { refreshParty(partyId) }

    override suspend fun leaveParty(partyId: String): SupabaseResult<Unit> =
        remote.leaveParty(partyId).alsoOnSuccess { refreshParties() }

    override suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit> =
        remote.removeMember(partyId, userId).alsoOnSuccess { refreshParty(partyId) }

    override suspend fun renameParty(partyId: String, name: String): SupabaseResult<Unit> =
        remote.renameParty(partyId, name).alsoOnSuccess { refreshParties() }

    override suspend fun transferLeadership(partyId: String, userId: String): SupabaseResult<Unit> =
        remote.transferLeadership(partyId, userId).alsoOnSuccess { refreshParty(partyId) }

    // The server no longer lists it, so the refresh takes it off the phone too.
    override suspend fun deleteParty(partyId: String): SupabaseResult<Unit> =
        remote.deleteParty(partyId).alsoOnSuccess { refreshParties() }

    private suspend inline fun <T> SupabaseResult<T>.alsoOnSuccess(then: () -> Unit): SupabaseResult<T> {
        if (this is SupabaseResult.Success) then()
        return this
    }
}

/** A failure, as a result of any type. */
@Suppress("UNCHECKED_CAST")
private fun <T> SupabaseResult<*>.failure(): SupabaseResult<T> = this as SupabaseResult<T>

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
    syncedAt = syncedAt,
    isPublicRace = party.isPublicRace,
    partyType = PartyType.fromWire(party.partyType)
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
    myStatus = myStatus,
    isPublicRace = isPublicRace,
    type = partyType
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
    lastEntryDate = stats?.lastEntryDate,
    statusChangedAt = statusChangedAt?.toEpochMilli()
)

internal fun PartyMemberEntity.toDomain() = PartyMember(
    partyId = partyId,
    userId = userId,
    username = username,
    role = role,
    status = status,
    invitedAt = invitedAt?.let(Instant::ofEpochMilli),
    joinedAt = joinedAt?.let(Instant::ofEpochMilli),
    statusChangedAt = statusChangedAt?.let(Instant::ofEpochMilli)
)
