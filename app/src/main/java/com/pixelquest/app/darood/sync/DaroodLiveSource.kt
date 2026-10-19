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

/** Something that changed in a party while it was open. */
sealed interface PartyChange {
    /** Someone posted (the account included, from this phone or another). */
    data class EntryAdded(val entry: DaroodEntry) : PartyChange

    /** Someone joined, left, was removed or was invited. */
    data object MembersChanged : PartyChange
}

/**
 * Live changes to one party, as they happen (docs/darood-design.md). The flow is cold: collecting
 * it subscribes, and stopping it unsubscribes, so a party is followed only while its screen is up.
 */
fun interface DaroodLiveSource {
    fun changes(partyId: String): Flow<PartyChange>
}

/**
 * Supabase Realtime's postgres changes for one party's entries and members. The server sends only
 * what the account's row-level security lets it read (20261019000025_darood_realtime.sql).
 */
@Singleton
class SupabaseDaroodLiveSource @Inject constructor(
    private val supabase: SupabaseClient
) : DaroodLiveSource {

    override fun changes(partyId: String): Flow<PartyChange> = channelFlow {
        val channel = supabase.channel("darood-party-$partyId")
        val entries = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "darood_entries"
            filter("party_id", FilterOperator.EQ, partyId)
        }
        val members = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "party_members"
            filter("party_id", FilterOperator.EQ, partyId)
        }
        launch { entries.collect { send(PartyChange.EntryAdded(it.decodeRecord<DaroodEntryDto>().toDomain())) } }
        launch { members.collect { send(PartyChange.MembersChanged) } }
        try {
            channel.subscribe()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }
}
