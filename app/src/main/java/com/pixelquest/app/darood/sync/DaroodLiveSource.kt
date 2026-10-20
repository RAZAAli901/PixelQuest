package com.pixelquest.app.darood.sync

import com.pixelquest.app.darood.data.remote.DaroodEntryDto
import com.pixelquest.app.darood.data.remote.toDomain
import com.pixelquest.app.darood.domain.DaroodEntry
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.pixelquest.app.party.sync.MembershipChange
import com.pixelquest.app.party.sync.PartyLiveSource
import kotlinx.coroutines.CompletableDeferred

/** Something that changed in a party while it was open. */
sealed interface PartyChange {
    /** Someone posted (the account included, from this phone or another). */
    data class EntryAdded(val entry: DaroodEntry) : PartyChange

    /** Someone joined, left, was removed or was invited. */
    data object MembersChanged : PartyChange

    /**
     * Listening has started. Anything posted while it was starting wasn't heard, so this is the
     * moment to catch up.
     */
    data object Subscribed : PartyChange
}

/**
 * Live changes to one party, as they happen (docs/darood-design.md). The flow is cold: collecting
 * it subscribes, and stopping it unsubscribes, so a party is followed only while its screen is up.
 */
fun interface DaroodLiveSource {
    fun changes(partyId: String): Flow<PartyChange>
}

/**
 * Supabase Realtime's postgres changes for one party's entries, with its members from every party's
 * live source ([PartyLiveSource]). The server sends only what the account's row-level security lets
 * it read (20261019000025_darood_realtime.sql).
 */
@Singleton
class SupabaseDaroodLiveSource @Inject constructor(
    private val supabase: SupabaseClient,
    private val partyLive: PartyLiveSource
) : DaroodLiveSource {

    override fun changes(partyId: String): Flow<PartyChange> = channelFlow {
        // A channel per table: supabase-kt 3.0.2 can throw (IndexOutOfBoundsException) when two change
        // flows on one channel are torn down at once, as they are when the screen closes. The members'
        // channel is the shared helper's.
        val entriesChannel = supabase.channel("darood-entries-$partyId")
        val entries = entriesChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "darood_entries"
            filter("party_id", FilterOperator.EQ, partyId)
        }
        val membersListening = CompletableDeferred<Unit>()
        launch { entries.collect { send(PartyChange.EntryAdded(it.decodeRecord<DaroodEntryDto>().toDomain())) } }
        launch {
            partyLive.members(partyId).collect { change ->
                when (change) {
                    MembershipChange.Subscribed -> membersListening.complete(Unit)
                    MembershipChange.Changed -> send(PartyChange.MembersChanged)
                }
            }
        }
        try {
            entriesChannel.subscribe(blockUntilSubscribed = true)
            membersListening.await()
            send(PartyChange.Subscribed)
            awaitCancellation()
        } finally {
            // Closing must never fail the screen: a channel already gone is fine. The members' channel
            // closes itself as its flow stops.
            withContext(NonCancellable) { runCatching { supabase.realtime.removeChannel(entriesChannel) } }
        }
    }
}
