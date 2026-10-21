package com.pixelquest.app.testing

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.study.chat.data.remote.StudyChatRemoteDataSource
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.domain.StudyRefusal
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Supabase's study chat as 20261019000050 to 058 make it, in memory: members of [members] read and
 * send (as themselves, once per key, 30 a minute); the sender or the [leaders]' leader deletes.
 * [online] false is no connection; [loseNextAnswer] stores the next message but loses its answer.
 */
class FakeChatServer(var me: String, var now: Instant = Instant.parse("2026-10-21T10:00:00Z")) : StudyChatRemoteDataSource {
    val messages = mutableListOf<ChatMessage>()
    val files = mutableListOf<ChatFile>()
    val members = mutableMapOf<String, MutableSet<String>>()
    val leaders = mutableMapOf<String, String>()
    val reads = mutableMapOf<Pair<String, String>, Instant>()
    var online = true
    var loseNextAnswer = false
    var calls = 0
        private set

    private fun offline() = SupabaseResult.NetworkError(IOException("offline"))
    private fun member(party: String) = me in members[party].orEmpty()

    @Synchronized
    private fun <T> answer(block: () -> SupabaseResult<T>): SupabaseResult<T> {
        calls++
        return if (!online) offline() else block()
    }

    override suspend fun fetchMessages(partyId: String, limit: Int, before: Instant?): SupabaseResult<List<ChatMessage>> = answer {
        if (!member(partyId)) return@answer SupabaseResult.Success(emptyList())
        SupabaseResult.Success(messages.filter { it.partyId == partyId && (before == null || it.createdAt < before) }.sortedByDescending { it.createdAt }.take(limit))
    }

    override suspend fun fetchMessagesSince(partyId: String, after: Instant): SupabaseResult<List<ChatMessage>> = answer {
        if (!member(partyId)) return@answer SupabaseResult.Success(emptyList())
        SupabaseResult.Success(messages.filter { it.partyId == partyId && it.createdAt >= after }.sortedBy { it.createdAt })
    }

    override suspend fun sendMessage(partyId: String, body: String, replyTo: String?, clientId: String): SupabaseResult<ChatMessage> = answer {
        if (!member(partyId)) return@answer SupabaseResult.ServerError(403, "new row violates row-level security policy")
        messages.firstOrNull { it.senderId == me && it.clientId == clientId }?.let { return@answer SupabaseResult.Success(it) }
        if (replyTo != null && messages.none { it.id == replyTo && it.partyId == partyId }) return@answer SupabaseResult.ServerError(400, "bad_reply")
        if (messages.count { it.senderId == me && it.createdAt > now.minus(Duration.ofMinutes(1)) } >= 30) {
            return@answer SupabaseResult.ServerError(400, StudyRefusal.TOO_MANY_MESSAGES.token)
        }
        val stored = ChatMessage(UUID.randomUUID().toString(), partyId, me, ChatMessageKind.TEXT, body, null, replyTo, clientId, now, null)
        messages += stored
        if (loseNextAnswer) {
            loseNextAnswer = false
            return@answer offline()
        }
        SupabaseResult.Success(stored)
    }

    override suspend fun deleteMessage(messageId: String): SupabaseResult<Unit> = answer {
        val i = messages.indexOfFirst { it.id == messageId }
        val message = messages.getOrNull(i)
        if (message == null || !member(message.partyId) || !(message.senderId == me || leaders[message.partyId] == me)) {
            return@answer StudyRefusal.result(StudyRefusal.NOT_ALLOWED)
        }
        messages[i] = message.copy(body = null, fileId = null, deletedAt = now)
        SupabaseResult.Success(Unit)
    }

    override suspend fun fetchFiles(partyId: String): SupabaseResult<List<ChatFile>> = answer {
        if (!member(partyId)) return@answer SupabaseResult.Success(emptyList())
        SupabaseResult.Success(files.filter { it.partyId == partyId && (it.status == ChatFileStatus.READY || it.uploaderId == me) })
    }

    override suspend fun markRead(partyId: String): SupabaseResult<Unit> = answer {
        if (!member(partyId)) return@answer SupabaseResult.ServerError(403, "not_a_member")
        reads[partyId to me] = maxOf(reads[partyId to me] ?: Instant.EPOCH, now)
        SupabaseResult.Success(Unit)
    }

    /** A message someone sent, straight on the server, [minutesAgo] before now. */
    fun seed(partyId: String, by: String, body: String, minutesAgo: Long = 0): ChatMessage {
        val message = ChatMessage(UUID.randomUUID().toString(), partyId, by, ChatMessageKind.TEXT, body, null, null, null, now.minus(Duration.ofMinutes(minutesAgo)), null)
        messages += message
        return message
    }
}
