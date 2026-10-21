package com.pixelquest.app.darood.data

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.local.DaroodDao
import com.pixelquest.app.darood.data.local.DaroodEntryEntity
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.darood.data.local.PendingEntryEntity
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.DaroodWeeks
import com.pixelquest.app.darood.domain.WeekResult
import com.pixelquest.app.party.domain.Party
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
import com.pixelquest.app.darood.data.remote.map
import java.time.LocalDate
import com.pixelquest.app.darood.domain.DayTotals
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.party.data.PartyRepositoryImpl
import com.pixelquest.app.party.sync.PartyLiveSource

/**
 * Darood Parties for the screens (docs/darood-design.md): every party's calls ([PartyRepository]),
 * and Darood's own ([DaroodRepository]), as one.
 */
interface DaroodPartyRepository : PartyRepository, DaroodRepository

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
    private val catchUpDelayMs: Long = CATCH_UP_DELAY_MS,
    partyLive: PartyLiveSource? = null
) : PartyRepositoryImpl(remote, dao, now, partyLive, account::id), DaroodPartyRepository {

    @Inject
    constructor(
        remote: DaroodPartyRemoteDataSource,
        dao: DaroodDao,
        account: DaroodAccount,
        syncScheduler: DaroodSyncScheduler,
        live: DaroodLiveSource,
        partyLive: PartyLiveSource
    ) : this(remote, dao, account, System::currentTimeMillis, ZoneId::systemDefault, syncScheduler, live, partyLive = partyLive)

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

    override fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(zone()).toLocalDate()

    override suspend fun fetchDayTotals(partyId: String, day: LocalDate): SupabaseResult<DayTotals> {
        val me = account.id()
        return remote.fetchDayTotals(partyId, day).map { rows ->
            DayTotals(mine = rows.filter { me != null && it.userId == me }.sumOf { it.total }, party = rows.sumOf { it.total })
        }
    }

    override fun observePendingTotalOn(partyId: String, day: LocalDate): Flow<Long> = dao.observePendingTotalOn(partyId, day)

    override suspend fun fetchMyDays(partyId: String): SupabaseResult<Map<LocalDate, Long>> = remote.fetchMyDays(partyId)

    override suspend fun fetchRaceBoard(): SupabaseResult<List<com.pixelquest.app.darood.domain.RaceLine>> = remote.fetchRaceBoard()

    override suspend fun closeLastWeek(partyId: String): SupabaseResult<WeekResult?> {
        val monday = DaroodWeeks.lastClosed(today())
        val closed = remote.closeWeek(partyId, monday)
        // Still Sunday in the party's own zone: its week isn't over there yet.
        if (closed is SupabaseResult.ServerError && closed.message.contains(WEEK_NOT_OVER)) return SupabaseResult.Success(null)
        if (closed !is SupabaseResult.Success) return closed.failure()
        return remote.fetchWeekResults(partyId, monday).map { weeks -> weeks.firstOrNull { it.weekStart == monday } }
    }

    override suspend fun fetchWeekResults(partyId: String, weeks: Int): SupabaseResult<List<WeekResult>> =
        remote.fetchWeekResults(partyId, DaroodWeeks.mondayOf(today()).minusWeeks(weeks.toLong()))

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

    override suspend fun retryEntry(clientId: String): SendReport {
        val entry = dao.getPending(clientId) ?: return SendReport(0, 0, 0)
        if (entry.state != EntryState.FAILED) return SendReport(0, 0, 0)
        dao.setPendingState(clientId, EntryState.PENDING, null)
        return sendPending().also { if (it.needsRetry) syncScheduler?.scheduleSend() }
    }

    override suspend fun loadOlderEntries(partyId: String): SupabaseResult<Boolean> {
        val oldest = dao.oldestEntryId(partyId) ?: return SupabaseResult.Success(false)
        return when (val result = remote.fetchEntries(partyId, beforeId = oldest)) {
            is SupabaseResult.Success -> {
                val kept = dao.upsertEntriesIfKept(partyId, result.data.map { it.toEntity() })
                SupabaseResult.Success(kept && result.data.size == DaroodPartyRemoteDataSource.PAGE_SIZE)
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
            dao.upsertEntriesIfKept(entry.partyId, listOf(entry.toEntity()))
        }
    }

    override suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String> =
        remote.createParty(name, maxMembers, milestone).alsoOnSuccess { refreshParties() }

    override suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long> =
        remote.setMilestone(partyId, target).alsoOnSuccess { refreshParties() }

    override suspend fun setRaceBoard(partyId: String, on: Boolean): SupabaseResult<Unit> =
        remote.setRaceBoard(partyId, on).alsoOnSuccess { refreshParties() }

    private suspend inline fun <T> SupabaseResult<T>.alsoOnSuccess(then: () -> Unit): SupabaseResult<T> {
        if (this is SupabaseResult.Success) then()
        return this
    }


    /** A Darood Party refreshing brings its newest entries too, once its members are in. */
    override suspend fun refreshOwn(party: PartyEntity): SupabaseResult<Unit> {
        if (party.partyType != PartyType.DAROOD) return SupabaseResult.Success(Unit)
        val entries = when (val result = remote.fetchEntries(party.id)) {
            is SupabaseResult.Success -> result.data
            else -> return result.failure()
        }
        // The party may have gone while these were on their way (its leader deleted it): then they're dropped.
        dao.upsertEntriesIfKept(party.id, entries.map { it.toEntity() })
        return SupabaseResult.Success(Unit)
    }
    companion object {
        /** How long a party's entries must pause before its numbers are refreshed. */
        const val NUMBERS_DELAY_MS = 1_500L

        /** How long after listening starts the second catch-up comes, once the server listens too. */
        const val CATCH_UP_DELAY_MS = 3_000L

        /** The server's refusal while a week is still running in the party's zone. */
        const val WEEK_NOT_OVER = "week_not_over"
    }
}

/** A failure, as a result of any type. */
@Suppress("UNCHECKED_CAST")
internal fun <T> SupabaseResult<*>.failure(): SupabaseResult<T> = this as SupabaseResult<T>

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
    state = state,
    refusal = refusal
)

