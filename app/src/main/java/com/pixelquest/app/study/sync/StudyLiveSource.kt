package com.pixelquest.app.study.sync

import com.pixelquest.app.party.sync.MembershipChange
import com.pixelquest.app.party.sync.PartyLiveSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Something that changed in a study party while it was open. */
sealed interface StudyChange {
    /** An item was added or changed, or someone ticked one off or logged study time. */
    data object Activity : StudyChange

    /** Someone joined, left, was removed or was invited. */
    data object MembersChanged : StudyChange

    /** Listening has started: the moment to catch up with anything missed while it was starting. */
    data object Subscribed : StudyChange
}

/**
 * Live changes to one study party (docs/study-party.md). Cold, as Darood's: collecting it subscribes
 * and stopping it unsubscribes, so a party is followed only while its screen is up.
 */
fun interface StudyLiveSource {
    fun changes(partyId: String): Flow<StudyChange>
}

/**
 * Supabase Realtime's postgres changes for one study party: new and changed items, new ticks and new
 * study time (20261019000044_study_realtime.sql), and its members from every party's live source.
 * The server sends only what the account's row-level security lets it read. What's taken away (an
 * undone tick, a deleted item) isn't heard; it shows at the next refresh.
 */
@Singleton
class SupabaseStudyLiveSource @Inject constructor(
    private val supabase: SupabaseClient,
    private val partyLive: PartyLiveSource
) : StudyLiveSource {

    override fun changes(partyId: String): Flow<StudyChange> = channelFlow {
        // A channel per table and action: supabase-kt 3.0.2 can throw when two change flows on one
        // channel are torn down at once (as Darood's live source notes).
        val channels = mutableListOf<RealtimeChannel>()
        fun listen(table: String, update: Boolean) {
            val channel = supabase.channel("study-$table-${if (update) "update" else "insert"}-$partyId").also { channels += it }
            val changes = if (update) {
                channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    this.table = table
                    filter("party_id", FilterOperator.EQ, partyId)
                }
            } else {
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    this.table = table
                    filter("party_id", FilterOperator.EQ, partyId)
                }
            }
            launch { changes.collect { send(StudyChange.Activity) } }
        }
        listen("study_items", update = false)
        listen("study_items", update = true)
        listen("study_completions", update = false)
        listen("study_sessions", update = false)
        val membersListening = CompletableDeferred<Unit>()
        launch {
            partyLive.members(partyId).collect { change ->
                when (change) {
                    MembershipChange.Subscribed -> membersListening.complete(Unit)
                    MembershipChange.Changed -> send(StudyChange.MembersChanged)
                }
            }
        }
        try {
            channels.forEach { it.subscribe(blockUntilSubscribed = true) }
            membersListening.await()
            send(StudyChange.Subscribed)
            awaitCancellation()
        } finally {
            // Closing must never fail the screen: a channel already gone is fine.
            withContext(NonCancellable) { channels.forEach { runCatching { supabase.realtime.removeChannel(it) } } }
        }
    }
}
