package com.pixelquest.app.study.chat.data

import com.pixelquest.app.darood.data.SendReport
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.map
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.study.chat.data.local.ChatDao
import com.pixelquest.app.study.chat.data.local.ChatFileEntity
import com.pixelquest.app.study.chat.data.local.ChatMessageEntity
import com.pixelquest.app.study.chat.data.local.PendingChatMessageEntity
import com.pixelquest.app.study.chat.data.remote.StudyChatRemoteDataSource
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.domain.StudyChatConfig
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.chat.sync.ChatChange
import com.pixelquest.app.study.chat.sync.ChatLiveSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** How sending a message went. */
sealed interface ChatSend {
    data object Sent : ChatSend
    /** Saved on the phone; it goes when there's a connection. */
    data object Waiting : ChatSend
    /** Empty, or over 2,000 characters: nothing was saved. */
    data object Invalid : ChatSend
    data class Refused(val refusal: StudyRefusal) : ChatSend
}

/** Asks for the messages waiting on the phone to go once there's a connection. */
fun interface ChatSendScheduler {
    fun scheduleSend()
}

/**
 * A study party's chat for its screen (docs/study-chat.md, On the phone). Messages are kept in Room,
 * so the chat opens offline; the newest page first, older pages on asking. Text is saved first and
 * sent with its own key, so a bad connection loses nothing and a retry never stores it twice.
 */
interface StudyChatRepository {
    /** A party's newest [limit] messages, oldest first, with this phone's still waiting after them. */
    fun observeMessages(partyId: String, limit: Int): Flow<List<ChatMessage>>

    fun observeFiles(partyId: String): Flow<List<ChatFile>>

    /** The player's read mark in a party's chat. */
    fun observeLastRead(partyId: String): Flow<Instant?>

    /** Others' messages the player hasn't read, by party: for the Chat tab and the party list. */
    fun observeUnread(): Flow<Map<String, Int>>

    /** Sends what's waiting, then brings the party's newest messages and its files. */
    suspend fun refresh(partyId: String): SupabaseResult<Unit>

    /** Brings a page of messages older than the oldest the phone has; false once there are none. */
    suspend fun loadOlder(partyId: String): SupabaseResult<Boolean>

    /** Writes [text] in a party's chat, answering [replyTo]: saved, then sent. */
    suspend fun send(partyId: String, text: String, replyTo: String? = null): ChatSend

    /** Sends everything waiting, oldest first; what the server refuses is kept, FAILED, with its reason. */
    suspend fun sendPending(): SendReport

    /** Tries a refused message again, as it was. */
    suspend fun retry(clientId: String): ChatSend

    /** Drops a refused (or still waiting) message from the phone. */
    suspend fun discard(clientId: String)

    /** Deletes a message: the player's own, or any as the party's leader (the server checks). */
    suspend fun delete(messageId: String): SupabaseResult<Unit>

    /** The player has read the chat up to now: here, and on the server when it can. */
    suspend fun markRead(partyId: String)

    /**
     * Follows a party's chat live while collected: each change brings the chat up to date, once the
     * changes pause, and is passed on. Nothing without a live source.
     */
    fun follow(partyId: String): Flow<ChatChange> = emptyFlow()
}

@Singleton
class StudyChatRepositoryImpl(
    private val remote: StudyChatRemoteDataSource,
    private val dao: ChatDao,
    private val account: DaroodAccount,
    private val clock: () -> Long,
    private val sendLater: ChatSendScheduler? = null,
    private val live: ChatLiveSource? = null,
    private val quietMs: Long = QUIET_MS
) : StudyChatRepository {
    @Inject
    constructor(remote: StudyChatRemoteDataSource, dao: ChatDao, account: DaroodAccount, sendLater: ChatSendScheduler, live: ChatLiveSource) :
        this(remote, dao, account, System::currentTimeMillis, sendLater, live)

    /** One send at a time: two at once would send the same message twice. */
    private val sending = Mutex()

    override fun observeMessages(partyId: String, limit: Int): Flow<List<ChatMessage>> =
        combine(dao.observeMessages(partyId, limit), dao.observePending(partyId)) { stored, waiting ->
            val me = account.id()
            val sentKeys = stored.mapNotNull { it.clientId }.toSet()
            stored.map { it.toDomain() } + waiting.filter { it.clientId !in sentKeys }.map { it.toMessage(me) }
        }

    override fun observeFiles(partyId: String): Flow<List<ChatFile>> = dao.observeFiles(partyId).map { rows -> rows.map { it.toDomain() } }

    override fun observeLastRead(partyId: String): Flow<Instant?> = dao.observeLastRead(partyId).map { it?.let(Instant::ofEpochMilli) }

    override fun observeUnread(): Flow<Map<String, Int>> {
        val me = account.id() ?: return kotlinx.coroutines.flow.flowOf(emptyMap())
        return dao.observeUnread(me).map { counts -> counts.associate { it.partyId to it.unread } }
    }

    override suspend fun refresh(partyId: String): SupabaseResult<Unit> {
        sendPending()
        val newest = dao.newestMessage(partyId)
        // A page of the newest to start; after that, everything since the newest the phone has
        // (deletes among them: a deleted message comes back with its deleted_at).
        val fetched = if (newest == null || dao.messageCount(partyId) == 0) {
            remote.fetchMessages(partyId, PAGE)
        } else {
            remote.fetchMessagesSince(partyId, Instant.ofEpochMilli(newest.createdAt).minusSeconds(RECENT_SECONDS))
        }
        if (fetched !is SupabaseResult.Success) return fetched.map { }
        dao.upsertMessagesIfKept(partyId, fetched.data.map { it.toEntity() })
        val files = remote.fetchFiles(partyId)
        if (files !is SupabaseResult.Success) return files.map { }
        dao.upsertFilesIfKept(partyId, files.data.map { it.toEntity() })
        return SupabaseResult.Success(Unit)
    }

    override suspend fun loadOlder(partyId: String): SupabaseResult<Boolean> {
        val oldest = dao.oldestMessage(partyId) ?: return refresh(partyId).map { true }
        val fetched = remote.fetchMessages(partyId, PAGE, before = Instant.ofEpochMilli(oldest.createdAt))
        if (fetched !is SupabaseResult.Success) return fetched.map { false }
        dao.upsertMessagesIfKept(partyId, fetched.data.map { it.toEntity() })
        return SupabaseResult.Success(fetched.data.size == PAGE)
    }

    override suspend fun send(partyId: String, text: String, replyTo: String?): ChatSend {
        val body = text.trim()
        if (body.isEmpty() || body.length > StudyChatConfig.MESSAGE_MAX_CHARS) return ChatSend.Invalid
        val pending = PendingChatMessageEntity(clientId = UUID.randomUUID().toString(), partyId = partyId, body = body, replyTo = replyTo, createdAt = clock())
        if (!dao.insertPendingIfKept(pending)) return ChatSend.Refused(StudyRefusal.NOT_A_MEMBER)
        return sendAndReport(pending.clientId)
    }

    private suspend fun sendAndReport(clientId: String): ChatSend {
        sendPending()
        val left = dao.getPending(clientId) ?: return ChatSend.Sent
        return if (left.state == SyncState.FAILED) {
            ChatSend.Refused(StudyRefusal.entries.firstOrNull { it.token.isNotEmpty() && it.token == left.refusal } ?: StudyRefusal.INVALID)
        } else {
            sendLater?.scheduleSend()
            ChatSend.Waiting
        }
    }

    override suspend fun sendPending(): SendReport = sending.withLock {
        var sent = 0
        var refused = 0
        val waiting = dao.pendingToSend()
        for ((index, row) in waiting.withIndex()) {
            dao.countAttempt(row.clientId)
            val result = remote.sendMessage(row.partyId, row.body, row.replyTo, row.clientId)
            if (result is SupabaseResult.Success) {
                dao.confirmSent(row.clientId, result.data.toEntity())
                sent++
                continue
            }
            val refusal = StudyRefusal.of(result)
            // No connection, or signed out: this one and the rest wait, in order.
            if (refusal == null) return@withLock SendReport(sent, refused, waiting.size - index)
            dao.setPendingState(row.clientId, SyncState.FAILED, refusal.token.ifEmpty { (result as SupabaseResult.ServerError).message })
            refused++
        }
        SendReport(sent, refused, 0)
    }

    override suspend fun retry(clientId: String): ChatSend {
        val row = dao.getPending(clientId) ?: return ChatSend.Sent
        if (row.state == SyncState.FAILED) dao.setPendingState(clientId, SyncState.PENDING, null)
        return sendAndReport(clientId)
    }

    override suspend fun discard(clientId: String) = dao.deletePending(clientId)

    override suspend fun delete(messageId: String): SupabaseResult<Unit> =
        remote.deleteMessage(messageId).also { if (it is SupabaseResult.Success) dao.markDeleted(messageId, clock()) }

    override suspend fun markRead(partyId: String) {
        dao.markRead(partyId, clock())
        remote.markRead(partyId)
    }

    @OptIn(FlowPreview::class)
    override fun follow(partyId: String): Flow<ChatChange> = channelFlow {
        val source = live ?: return@channelFlow
        // A busy chat changes often: it's brought up to date once the changes pause, not after each.
        val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        launch { changed.debounce(quietMs).collect { refresh(partyId) } }
        source.changes(partyId).collect { change ->
            if (change == ChatChange.Subscribed) refresh(partyId) else changed.tryEmit(Unit)
            send(change)
        }
    }

    internal companion object {
        /** A page of messages: the newest to start, and each one older. */
        const val PAGE = 50

        /** A refresh asks again for the last minute before the newest message: a clock skew's room. */
        const val RECENT_SECONDS = 60L

        /** How long a chat's changes must pause before it's brought up to date. */
        const val QUIET_MS = 500L
    }
}

private fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id, partyId = partyId, senderId = senderId, kind = kind, body = body, fileId = fileId, replyTo = replyTo,
    clientId = clientId, createdAt = createdAt.toEpochMilli(), deletedAt = deletedAt?.toEpochMilli()
)

private fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id, partyId = partyId, senderId = senderId, kind = kind, body = body, fileId = fileId, replyTo = replyTo,
    clientId = clientId, createdAt = Instant.ofEpochMilli(createdAt), deletedAt = deletedAt?.let(Instant::ofEpochMilli)
)

private fun PendingChatMessageEntity.toMessage(me: String?) = ChatMessage(
    id = clientId, partyId = partyId, senderId = me, kind = ChatMessageKind.TEXT, body = body, fileId = null, replyTo = replyTo,
    clientId = clientId, createdAt = Instant.ofEpochMilli(createdAt), deletedAt = null, sync = state
)

private fun ChatFile.toEntity() = ChatFileEntity(
    id = id, partyId = partyId, uploaderId = uploaderId, displayName = displayName, mime = mime, sizeBytes = sizeBytes,
    sha256 = sha256, status = status, rejectReason = rejectReason, pinned = pinned, createdAt = createdAt.toEpochMilli(),
    readyAt = readyAt?.toEpochMilli()
)

private fun ChatFileEntity.toDomain() = ChatFile(
    id = id, partyId = partyId, uploaderId = uploaderId, displayName = displayName, mime = mime, sizeBytes = sizeBytes,
    sha256 = sha256, status = status, rejectReason = rejectReason, pinned = pinned, createdAt = Instant.ofEpochMilli(createdAt),
    readyAt = readyAt?.let(Instant::ofEpochMilli)
)
