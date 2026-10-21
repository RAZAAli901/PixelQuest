package com.pixelquest.app.study.chat.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
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

/** Something that changed in a study party's chat while it was open. */
sealed interface ChatChange {
    /** A new message. */
    data object NewMessage : ChatChange

    /** A message was deleted. */
    data object MessageChanged : ChatChange

    /** A file was shared, became ready or was rejected. */
    data object FileChanged : ChatChange

    /** Listening has started: the moment to catch up with anything missed while it was starting. */
    data object Subscribed : ChatChange
}

/**
 * Live changes to one study party's chat. Cold: collecting it subscribes and stopping it
 * unsubscribes, so a chat is followed only while it's on screen.
 */
fun interface ChatLiveSource {
    fun changes(partyId: String): Flow<ChatChange>
}

/**
 * Supabase Realtime's postgres changes for one study party's chat (20261019000063_chat_realtime.sql):
 * new messages, changed ones (a delete), and files shared or checked. The server sends only what
 * the account's row-level security lets it read; the app fetches on each, under the same rules.
 */
@Singleton
class SupabaseChatLiveSource @Inject constructor(
    private val supabase: SupabaseClient
) : ChatLiveSource {

    override fun changes(partyId: String): Flow<ChatChange> = channelFlow {
        // A channel per table and action, as the study party's live source does (supabase-kt can
        // throw when two change flows on one channel are torn down at once).
        val channels = mutableListOf<RealtimeChannel>()
        fun listen(table: String, update: Boolean, change: ChatChange) {
            val channel = supabase.channel("chat-$table-${if (update) "update" else "insert"}-$partyId").also { channels += it }
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
            launch { changes.collect { send(change) } }
        }
        listen("chat_messages", update = false, ChatChange.NewMessage)
        listen("chat_messages", update = true, ChatChange.MessageChanged)
        listen("chat_files", update = false, ChatChange.FileChanged)
        listen("chat_files", update = true, ChatChange.FileChanged)
        try {
            channels.forEach { it.subscribe(blockUntilSubscribed = true) }
            send(ChatChange.Subscribed)
            awaitCancellation()
        } finally {
            // Closing must never fail the screen: a channel already gone is fine.
            withContext(NonCancellable) { channels.forEach { runCatching { supabase.realtime.removeChannel(it) } } }
        }
    }
}
