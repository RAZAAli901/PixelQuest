package com.pixelquest.app.study.chat.data.remote

import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.map
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatMessage
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A study party's chat on Supabase (docs/study-chat.md): its messages and files, as row-level
 * security lets the account see them (20261019000054). Every call is wrapped in [safeSupabaseCall].
 */
interface StudyChatRemoteDataSource {
    /** Up to [limit] of a party's messages, newest first: before [before], or all the newest. */
    suspend fun fetchMessages(partyId: String, limit: Int, before: Instant? = null): SupabaseResult<List<ChatMessage>>

    /** Every message of a party since [after], oldest first, deletes among them. */
    suspend fun fetchMessagesSince(partyId: String, after: Instant): SupabaseResult<List<ChatMessage>>

    /**
     * Sends text as the account; the message as stored. [clientId] is its own key: sent again (an
     * answer lost on the way back), the one stored comes back, never a second.
     */
    suspend fun sendMessage(partyId: String, body: String, replyTo: String?, clientId: String): SupabaseResult<ChatMessage>

    /** Its sender, or the party's leader, deletes a message (delete_chat_message). */
    suspend fun deleteMessage(messageId: String): SupabaseResult<Unit>

    /** A party's files the account may see: ready ones, and its own pending or rejected. */
    suspend fun fetchFiles(partyId: String): SupabaseResult<List<ChatFile>>

    /** Moves the account's read mark on the server (mark_chat_read). */
    suspend fun markRead(partyId: String): SupabaseResult<Unit>

    /** Which files are linked to which of a party's items: item id to file ids (study_item_files). */
    suspend fun fetchItemFiles(partyId: String): SupabaseResult<Map<String, List<String>>> = SupabaseResult.Success(emptyMap())

    /** Its creator or the leader links a file to an item, or unlinks it (link_item_file). */
    suspend fun linkItemFile(itemId: String, fileId: String, linked: Boolean): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    /** Reports someone else's message (a file message reports its file): report_chat_message. */
    suspend fun reportMessage(messageId: String, reason: String): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    /** The account's own block list, and changing it. */
    suspend fun fetchBlocked(): SupabaseResult<Set<String>> = SupabaseResult.Success(emptySet())
    suspend fun setBlocked(userId: String, blocked: Boolean): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    /** The party's leader pins or unpins a ready file (pin_chat_file). */
    suspend fun pinFile(fileId: String, pinned: Boolean): SupabaseResult<Unit> = SupabaseResult.Success(Unit)
}

@Singleton
class SupabaseStudyChatRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest,
    private val account: DaroodAccount
) : StudyChatRemoteDataSource {

    private suspend fun <T> signedIn(block: suspend (String) -> T): SupabaseResult<T> {
        val me = account.id() ?: return SupabaseResult.AuthError(IllegalStateException("Not signed in"), "Sign in to use study parties.")
        return safeSupabaseCall { block(me) }
    }

    override suspend fun fetchMessages(partyId: String, limit: Int, before: Instant?): SupabaseResult<List<ChatMessage>> = signedIn {
        postgrest[MESSAGES].select(Columns.list(MESSAGE_COLUMNS)) {
            filter {
                eq("party_id", partyId)
                if (before != null) lt("created_at", before.toString())
            }
            order("created_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<ChatMessageDto>().map { it.toDomain() }
    }

    override suspend fun fetchMessagesSince(partyId: String, after: Instant): SupabaseResult<List<ChatMessage>> = signedIn {
        postgrest[MESSAGES].select(Columns.list(MESSAGE_COLUMNS)) {
            filter {
                eq("party_id", partyId)
                gte("created_at", after.toString())
            }
            order("created_at", Order.ASCENDING)
            limit(SINCE_LIMIT)
        }.decodeList<ChatMessageDto>().map { it.toDomain() }
    }

    override suspend fun sendMessage(partyId: String, body: String, replyTo: String?, clientId: String): SupabaseResult<ChatMessage> {
        val sent = signedIn {
            postgrest[MESSAGES].insert(NewChatMessageDto(partyId = partyId, body = body, replyTo = replyTo, clientId = clientId)) {
                select(Columns.list(MESSAGE_COLUMNS))
            }.decodeSingle<ChatMessageDto>().toDomain()
        }
        // 409: stored already under this key, its answer lost on the way back: that one.
        if (!(sent is SupabaseResult.ServerError && sent.code == 409)) return sent
        return signedIn { me ->
            postgrest[MESSAGES].select(Columns.list(MESSAGE_COLUMNS)) {
                filter {
                    eq("sender_id", me)
                    eq("client_id", clientId)
                }
            }.decodeSingle<ChatMessageDto>().toDomain()
        }
    }

    override suspend fun deleteMessage(messageId: String): SupabaseResult<Unit> = signedIn {
        postgrest.rpc("delete_chat_message", buildJsonObject { put("message_id", messageId) })
    }.map { }

    override suspend fun fetchFiles(partyId: String): SupabaseResult<List<ChatFile>> = signedIn {
        postgrest[FILES].select(Columns.list(FILE_COLUMNS)) {
            filter { eq("party_id", partyId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<ChatFileDto>().map { it.toDomain() }
    }

    override suspend fun markRead(partyId: String): SupabaseResult<Unit> = signedIn {
        postgrest.rpc("mark_chat_read", buildJsonObject { put("party_id", partyId) })
    }.map { }

    override suspend fun fetchItemFiles(partyId: String): SupabaseResult<Map<String, List<String>>> = signedIn {
        postgrest["study_item_files"].select(Columns.list("item_id", "file_id")) {
            filter { eq("party_id", partyId) }
        }.decodeList<ItemFileDto>().groupBy({ it.itemId }, { it.fileId })
    }

    override suspend fun linkItemFile(itemId: String, fileId: String, linked: Boolean): SupabaseResult<Unit> = signedIn {
        postgrest.rpc("link_item_file", buildJsonObject {
            put("item_id", itemId)
            put("file_id", fileId)
            put("linked", linked)
        })
    }.map { }

    override suspend fun reportMessage(messageId: String, reason: String): SupabaseResult<Unit> = signedIn {
        postgrest.rpc("report_chat_message", buildJsonObject {
            put("message_id", messageId)
            put("reason", reason)
        })
    }.map { }

    override suspend fun fetchBlocked(): SupabaseResult<Set<String>> = signedIn {
        postgrest["member_blocks"].select(Columns.list("blocked_id")).decodeList<BlockDto>().map { it.blockedId }.toSet()
    }

    override suspend fun setBlocked(userId: String, blocked: Boolean): SupabaseResult<Unit> = signedIn {
        postgrest.rpc(if (blocked) "block_member" else "unblock_member", buildJsonObject { put("user_id", userId) })
    }.map { }

    override suspend fun pinFile(fileId: String, pinned: Boolean): SupabaseResult<Unit> = signedIn {
        postgrest.rpc("pin_chat_file", buildJsonObject {
            put("file_id", fileId)
            put("pinned", pinned)
        })
    }.map { }

    internal companion object {
        const val MESSAGES = "chat_messages"
        const val FILES = "chat_files"
        const val SINCE_LIMIT = 500L
        val MESSAGE_COLUMNS = listOf("id", "party_id", "sender_id", "kind", "body", "file_id", "reply_to", "client_id", "created_at", "deleted_at")
        // Never storage_path: it isn't granted.
        val FILE_COLUMNS = listOf(
            "id", "party_id", "uploader_id", "display_name", "mime", "size_bytes", "sha256", "status", "reject_reason", "pinned", "created_at", "ready_at"
        )
    }
}

@kotlinx.serialization.Serializable
data class ItemFileDto(
    @kotlinx.serialization.SerialName("item_id") val itemId: String,
    @kotlinx.serialization.SerialName("file_id") val fileId: String
)

@kotlinx.serialization.Serializable
data class BlockDto(@kotlinx.serialization.SerialName("blocked_id") val blockedId: String)
