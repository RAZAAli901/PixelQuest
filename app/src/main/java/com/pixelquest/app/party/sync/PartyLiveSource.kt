package com.pixelquest.app.party.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
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

/** What a live membership listener hears. */
sealed interface MembershipChange {
    /** Listening has started: anything that changed while it was starting wasn't heard, so catch up now. */
    data object Subscribed : MembershipChange

    /** A row changed: someone joined, left, was removed or invited; or an invitation arrived or went. */
    data object Changed : MembershipChange
}

/**
 * Live changes to who is in a party, for every kind of party (docs/party-core.md). The flows are
 * cold: collecting one subscribes, stopping it unsubscribes, so a party, or the account's
 * invitations, are followed only while a screen shows them.
 */
interface PartyLiveSource {
    /** [partyId]'s members, while collected. */
    fun members(partyId: String): Flow<MembershipChange>

    /** The account's own memberships ([userId]'s): an invitation to any kind of party arriving, or taken back. */
    fun invitations(userId: String): Flow<MembershipChange>
}

/**
 * Supabase Realtime's postgres changes on party_members (published by 20261019000025). The server
 * sends only the rows the account's row-level security lets it read: a party's members while the
 * account is active in it, and the account's own rows.
 */
@Singleton
class SupabasePartyLiveSource @Inject constructor(
    private val supabase: SupabaseClient
) : PartyLiveSource {

    override fun members(partyId: String): Flow<MembershipChange> = follow("party-members-$partyId", "party_id", partyId)

    override fun invitations(userId: String): Flow<MembershipChange> = follow("party-invitations-$userId", "user_id", userId)

    /** party_members' rows whose [column] is [value], on a channel of their own ([name]). */
    private fun follow(name: String, column: String, value: String): Flow<MembershipChange> = channelFlow {
        // One table on one channel: supabase-kt 3.0.2 can throw when two change flows on one channel
        // are torn down at once, as they are when a screen closes.
        val channel = supabase.channel(name)
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "party_members"
            filter(column, FilterOperator.EQ, value)
        }
        launch { changes.collect { send(MembershipChange.Changed) } }
        try {
            channel.subscribe(blockUntilSubscribed = true)
            send(MembershipChange.Subscribed)
            awaitCancellation()
        } finally {
            // Closing must never fail the screen: a channel already gone is fine.
            withContext(NonCancellable) { runCatching { supabase.realtime.removeChannel(channel) } }
        }
    }
}
