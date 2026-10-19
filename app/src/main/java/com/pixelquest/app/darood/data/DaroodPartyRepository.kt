package com.pixelquest.app.darood.data

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.local.DaroodDao
import com.pixelquest.app.darood.data.local.DaroodEntryEntity
import com.pixelquest.app.darood.data.local.PartyEntity
import com.pixelquest.app.darood.data.local.PartyMemberEntity
import com.pixelquest.app.darood.data.local.PendingEntryEntity
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.data.remote.InviteOutcome
import com.pixelquest.app.darood.data.remote.RemoteParty
import com.pixelquest.app.darood.data.remote.parseInstant
import com.pixelquest.app.darood.data.remote.roleOf
import com.pixelquest.app.darood.data.remote.statusOf
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyStats
import com.pixelquest.app.darood.sync.DaroodLiveSource
import com.pixelquest.app.darood.sync.DaroodSyncScheduler
import com.pixelquest.app.darood.sync.PartyChange
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
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

    /**
     * The newest [limit] entries the phone has for a party, newest first: those still waiting to be
     * sent (PENDING) or refused (FAILED) first, then the server's.
     */
    fun observeEntries(partyId: String, limit: Int): Flow<List<DaroodEntry>>

    /** How many entries wait to be sent, in every party. */
    fun observePendingCount(): Flow<Int>

    /**
     * Posts [amount] for today, in the phone's time zone. It's saved on the phone first, as PENDING,
     * then sent; without a connection it stays until it can be. Returns the entry as saved.
     */
    suspend fun addEntry(partyId: String, amount: Int): DaroodEntry

    /** Sends every waiting entry, oldest first, until one can't be sent for now. */
    suspend fun sendPending(): SendReport

    /**
     * Brings a party's live changes into the cache while it's collected, and passes each on: a new
     * entry appears at once, the party's numbers follow once the changes pause, and a change of
     * members refreshes the party. Collect it while the party's screen is up.
     */
    fun followParty(partyId: String): Flow<PartyChange>

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

    suspend fun renameParty(partyId: String, name: String): SupabaseResult<Unit>

    suspend fun transferLeadership(partyId: String, userId: String): SupabaseResult<Unit>

    suspend fun deleteParty(partyId: String): SupabaseResult<Unit>
}

/**
 * What a [DaroodPartyRepository.sendPending] did: [sent] reached the server, [refused] were turned
 * down for good (a rule, with its reason kept), and [waiting] are still to send because the
 * connection or the sign-in failed.
 */
data class SendReport(val sent: Int, val refused: Int, val waiting: Int) {
    val needsRetry: Boolean get() = waiting > 0
}

@Singleton
class DaroodPartyRepositoryImpl(
    private val remote: DaroodPartyRemoteDataSource,
    private val dao: DaroodDao,
    private val account: DaroodAccount,
    private val now: () -> Long,
    private val zone: () -> ZoneId,
    private val syncScheduler: DaroodSyncScheduler? = null,
    private val live: DaroodLiveSource? = null,
    private val numbersDelayMs: Long = NUMBERS_DELAY_MS,
    private val catchUpDelayMs: Long = CATCH_UP_DELAY_MS
) : DaroodPartyRepository {

    @Inject
    constructor(
        remote: DaroodPartyRemoteDataSource,
        dao: DaroodDao,
        account: DaroodAccount,
        syncScheduler: DaroodSyncScheduler,
        live: DaroodLiveSource
    ) : this(remote, dao, account, System::currentTimeMillis, ZoneId::systemDefault, syncScheduler, live)

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
        combine(dao.observePending(partyId), dao.observeEntries(partyId, limit)) { waiting, stored ->
            val me = account.id()
            waiting.asReversed().map { it.toDomain(me) } + stored.map { it.toDomain() }
        }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun addEntry(partyId: String, amount: Int): DaroodEntry {
        require(amount in DaroodRules.AMOUNT_MIN..DaroodRules.AMOUNT_MAX) { "An entry is 1 to 100,000" }
        val at = now()
        val here = zone()
        val pending = PendingEntryEntity(
            clientId = UUID.randomUUID().toString(),
            partyId = partyId,
            amount = amount,
            localDate = Instant.ofEpochMilli(at).atZone(here).toLocalDate(),
            tz = here.id,
            createdAt = at
        )
        dao.insertPending(pending)
        // Couldn't go now: the sync worker sends it once there's a connection.
        if (sendPending().needsRetry) syncScheduler?.scheduleSend()
        return dao.getPending(pending.clientId)?.toDomain(account.id())
            ?: dao.entryByClientId(pending.clientId)?.toDomain()
            ?: pending.toDomain(account.id())
    }

    override suspend fun sendPending(): SendReport {
        var sent = 0
        var refused = 0
        val waiting = dao.pendingToSend()
        for ((index, entry) in waiting.withIndex()) {
            dao.countAttempt(entry.clientId)
            // Sent with its own key: if a send arrived but its answer didn't, this one gets it back.
            when (val result = remote.addEntry(entry.partyId, entry.amount, entry.localDate, entry.tz, entry.clientId)) {
                is SupabaseResult.Success -> {
                    dao.confirmSent(entry.clientId, result.data.toEntity().copy(clientId = entry.clientId))
                    sent++
                }
                else -> {
                    val refusal = DaroodRefusal.of(result)
                    if (refusal == null) {
                        // No connection, or signed out: this one and the rest wait, in order.
                        if (sent > 0) refreshParties()
                        return SendReport(sent, refused, waiting.size - index)
                    }
                    dao.setPendingState(entry.clientId, EntryState.FAILED, refusal.token.ifEmpty { (result as SupabaseResult.ServerError).message })
                    refused++
                }
            }
        }
        if (sent > 0) refreshParties()
        return SendReport(sent, refused, 0)
    }

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

    @OptIn(FlowPreview::class)
    override fun followParty(partyId: String): Flow<PartyChange> = channelFlow {
        val source = live ?: return@channelFlow
        // A busy party posts often: its numbers refresh once the entries pause, not after each.
        val numbersChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        launch { numbersChanged.debounce(numbersDelayMs).collect { refreshParties() } }
        source.changes(partyId).collect { change ->
            when (change) {
                is PartyChange.EntryAdded -> {
                    applyLiveEntry(change.entry)
                    numbersChanged.tryEmit(Unit)
                }
                PartyChange.MembersChanged -> refreshParty(partyId)
                // Listening has started: catch up with anything posted while it was starting. Realtime
                // says a channel has joined a moment before its database listener is ready, so once
                // more a little later, for anything posted in between.
                PartyChange.Subscribed -> {
                    refreshParty(partyId)
                    launch {
                        delay(catchUpDelayMs)
                        refreshParty(partyId)
                    }
                }
            }
            send(change)
        }
    }

    /** An entry heard live: this phone's own, sent from the queue, replaces its waiting copy. */
    private suspend fun applyLiveEntry(entry: DaroodEntry) {
        if (dao.getParty(entry.partyId) == null || entry.id == null) return
        val clientId = entry.clientId
        if (clientId != null && dao.getPending(clientId) != null) {
            dao.confirmSent(clientId, entry.toEntity())
        } else {
            dao.upsertEntries(listOf(entry.toEntity()))
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

    companion object {
        /** How long a party's entries must pause before its numbers are refreshed. */
        const val NUMBERS_DELAY_MS = 1_500L

        /** How long after listening starts the second catch-up comes, once the server listens too. */
        const val CATCH_UP_DELAY_MS = 3_000L
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

internal fun PendingEntryEntity.toDomain(me: String?) = DaroodEntry(
    id = null,
    clientId = clientId,
    partyId = partyId,
    userId = me,
    amount = amount,
    localDate = localDate,
    tz = tz,
    createdAt = Instant.ofEpochMilli(createdAt),
    state = state
)

