package com.pixelquest.app.study.chat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.study.chat.data.ChatSend
import com.pixelquest.app.study.chat.data.StudyChatRepository
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.domain.StudyChatConfig
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.ui.StudyPartyViewModel
import com.pixelquest.app.ui.navigation.TabSharing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** A line about the last thing the chat tried. */
sealed interface ChatNotice {
    data object SavedOffline : ChatNotice
    data object Offline : ChatNotice
    data class Refused(val refusal: StudyRefusal) : ChatNotice

    /** A picked file that can't be shared, and why, before anything's sent. */
    data class FileProblem(val problem: com.pixelquest.app.study.chat.domain.UploadProblem) : ChatNotice

    /** A picked document the app couldn't read (it went, or the picker gave nothing to open). */
    data object FileUnreadable : ChatNotice

    /** A shared file the player may no longer open (they left, or it was deleted). */
    data object FileNotAllowed : ChatNotice

    /** A shared file that didn't arrive as the server checked it: not kept. */
    data object FileCorrupted : ChatNotice

    /** No app on the phone opens that kind of file. */
    data object NoViewer : ChatNotice

    /** A report was sent. */
    data object Reported : ChatNotice
}

/**
 * A study party's Chat tab (docs/study-chat.md), as navigation opens its party (`partyId`): its
 * messages from the phone's copy, brought up to date on opening and live while it's visible, with
 * names from the party's members. Every action goes through [StudyChatRepository], which keeps the
 * rules; the server keeps them again.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudyChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chat: StudyChatRepository,
    private val parties: PartyRepository,
    private val study: StudyRepository,
    private val picked: com.pixelquest.app.study.chat.data.PickedFileReader = com.pixelquest.app.study.chat.data.PickedFileReader { null },
    private val uploads: com.pixelquest.app.study.chat.data.ChatUploads? = null,
    private val fetcher: com.pixelquest.app.study.chat.data.ChatFileFetcher? = null,
    private val warned: com.pixelquest.app.study.chat.data.FileWarningSeen = com.pixelquest.app.study.chat.data.MemoryFileWarningSeen(),
    private val mutes: com.pixelquest.app.study.chat.sync.ChatMutes? = null
) : ViewModel() {

    /** Whether this party's chat notifies on this phone. */
    val chatMuted: StateFlow<Boolean> = (mutes?.mutedFlow ?: MutableStateFlow(emptySet()))
        .let { flow -> kotlinx.coroutines.flow.combine(flow, MutableStateFlow(Unit)) { set, _ -> partyId in set } }
        .stateIn(viewModelScope, TabSharing, false)

    fun setChatMuted(muted: Boolean) {
        mutes?.setMuted(partyId, muted)
    }

    /** A file waiting for the player to say yes to the first-open warning. */
    private val confirmingOpen = MutableStateFlow<com.pixelquest.app.study.chat.domain.ChatFile?>(null)

    /** A shared file saved and ready for the system viewer: the screen opens it, once. */
    val openRequests = kotlinx.coroutines.flow.MutableSharedFlow<com.pixelquest.app.study.chat.data.OpenedFile.Saved>(extraBufferCapacity = 1)

    /** The file being fetched to open, while it is. */
    private val opening = MutableStateFlow<String?>(null)


    val partyId: String = savedStateHandle[StudyPartyViewModel.ARG_PARTY] ?: ""

    private val limit = MutableStateFlow(PAGE)
    private val draft = MutableStateFlow("")
    private val replyingTo = MutableStateFlow<String?>(null)
    private val loadingOlder = MutableStateFlow(false)
    private val notice = MutableStateFlow<ChatNotice?>(null)
    private val attachment = MutableStateFlow<com.pixelquest.app.study.chat.data.PickedFile?>(null)
    /** False once the server has said there's nothing older than what the phone has. */
    @Volatile
    private var serverHasOlder = true

    /** Where the unread divider goes: the read mark as it was when the chat opened, kept while it's open. */
    private val readAtOpening = MutableStateFlow<Instant?>(null)
    private val loaded = MutableStateFlow(false)

    private val messages = limit.flatMapLatest { chat.observeMessages(partyId, it) }

    private val people = combine(parties.observeParty(partyId), parties.observeMembers(partyId)) { party, members ->
        People(party?.leaderId, members.mapNotNull { m -> m.username?.let { m.userId to it } }.toMap(), members.filter { it.status == MemberStatus.ACTIVE }.map { it.userId }.toSet())
    }

    private data class People(val leaderId: String?, val names: Map<String, String>, val active: Set<String>)

    val uiState: StateFlow<ChatUiState> = combine(
        combine(messages, chat.observeFiles(partyId), chat.observeBlocked(), ::Triple),
        people,
        combine(draft, replyingTo, loadingOlder, ::Triple),
        combine(readAtOpening, combine(loaded, opening, confirmingOpen, ::Triple), chat.observeUnread(), ::Triple),
        combine(combine(notice, chat.observeItemFiles(partyId), ::Pair), attachment, uploads?.observe(partyId) ?: kotlinx.coroutines.flow.flowOf(emptyList()), ::Triple)
    ) { (all, files, blockedIds), people, (text, replyId, older), (readAt, readyAndOpening, unread), (saidAndLinks, file, sharing) ->
        val (said, links) = saidAndLinks
        val (ready, fetching, confirming) = readyAndOpening
        val me = study.myId()
        val rows = rowsOf(all, files, me, people.names, study.zone()).map { if (it.message.senderId in blockedIds) it.copy(blocked = true) else it }
        ChatUiState(
            loaded = ready,
            rows = rows,
            files = files.filter { it.status == com.pixelquest.app.study.chat.domain.ChatFileStatus.READY },
            draft = text,
            replyingTo = replyId?.let { id -> rows.firstOrNull { it.message.id == id } },
            myId = me,
            isLeader = people.leaderId != null && people.leaderId == me,
            lastReadAt = readAt,
            today = study.today(),
            loadingOlder = older,
            notice = said,
            unread = unread[partyId] ?: 0,
            attachment = file,
            uploads = sharing.map { ChatUploadRow(it.clientId, it.displayName, it.mime, it.sizeBytes, it.sentBytes, it.state, it.reason) },
            opening = fetching,
            confirmingOpen = confirming,
            itemFiles = links
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, TabSharing, ChatUiState())

    /** The last thing tried, said once: a send saved offline, or refused. */
    val notices: StateFlow<ChatNotice?> = notice

    init {
        viewModelScope.launch {
            readAtOpening.value = chat.observeLastRead(partyId).first()
            // Fetched once the party is on the phone (a first visit brings it): its chat goes with it.
            parties.observeParty(partyId).filterNotNull().first()
            refresh()
        }
    }

    /** Brings the chat up to date: on opening, and when the player pulls for it. */
    fun refresh() {
        viewModelScope.launch {
            val result = chat.refresh(partyId)
            loaded.value = true
            if (result is SupabaseResult.NetworkError) notice.value = ChatNotice.Offline
        }
    }

    /** Follows the chat live while it's on screen (the caller collects only while it's visible). */
    suspend fun followWhileVisible() {
        chat.follow(partyId).collect { }
    }

    /** The player is looking at the chat: what's there is read. */
    fun markRead() {
        viewModelScope.launch { chat.markRead(partyId) }
    }

    fun changeDraft(text: String) {
        draft.value = text.take(StudyChatConfig.MESSAGE_MAX_CHARS)
    }

    /**
     * The player picked a document to share: checked on the phone first (type, size, name), so a
     * file sure to be refused is said at once; otherwise it waits over the field, cleanly named.
     */
    fun attach(uri: String) {
        viewModelScope.launch {
            val file = picked.describe(uri)
            if (file == null) {
                notice.value = ChatNotice.FileUnreadable
                return@launch
            }
            com.pixelquest.app.study.chat.domain.UploadChecks.check(file.name, file.mime, file.sizeBytes)
                .onSuccess { ok ->
                    attachment.value = file.copy(name = ok.name, mime = ok.mime)
                    if (notice.value is ChatNotice.FileProblem) notice.value = null
                }
                .onFailure { e ->
                    attachment.value = null
                    notice.value = (e as? com.pixelquest.app.study.chat.domain.UploadRefused)?.let { ChatNotice.FileProblem(it.problem) } ?: ChatNotice.FileUnreadable
                }
        }
    }

    fun removeAttachment() {
        attachment.value = null
    }

    fun replyTo(row: ChatMessageRow) {
        if (!row.message.deleted && row.message.sync == null) replyingTo.value = row.message.id
    }

    fun cancelReply() {
        replyingTo.value = null
    }

    fun dismissNotice() {
        notice.value = null
    }

    fun send() {
        val text = draft.value
        val file = attachment.value
        if (file != null) {
            attachment.value = null
            viewModelScope.launch { uploads?.share(partyId, file) }
        }
        if (text.isBlank()) return
        val reply = replyingTo.value
        draft.value = ""
        replyingTo.value = null
        viewModelScope.launch {
            when (val result = chat.send(partyId, text, reply)) {
                ChatSend.Sent -> Unit
                ChatSend.Waiting -> notice.value = ChatNotice.SavedOffline
                ChatSend.Invalid -> draft.value = text
                is ChatSend.Refused -> notice.value = ChatNotice.Refused(result.refusal)
            }
        }
    }

    /**
     * The player tapped a shared file: fetched into the app's own storage (membership checked
     * again by the server), then handed to the system viewer they choose. Never opened otherwise.
     */
    fun openFile(file: com.pixelquest.app.study.chat.domain.ChatFile) {
        // Files come from other people: the first one opened from a party asks first (the player's own don't).
        if (!warned.seen(partyId) && file.uploaderId != study.myId()) {
            confirmingOpen.value = file
            return
        }
        fetchAndOpen(file)
    }

    /** The player read the warning and still wants it open: not asked again in this party. */
    fun confirmOpen() {
        val file = confirmingOpen.value ?: return
        confirmingOpen.value = null
        warned.markSeen(partyId)
        fetchAndOpen(file)
    }

    fun cancelOpen() {
        confirmingOpen.value = null
    }

    private fun fetchAndOpen(file: com.pixelquest.app.study.chat.domain.ChatFile) {
        val fetch = fetcher ?: return
        if (opening.value != null) return
        opening.value = file.id
        viewModelScope.launch {
            when (val got = fetch.fetch(file)) {
                is com.pixelquest.app.study.chat.data.OpenedFile.Saved -> openRequests.emit(got)
                com.pixelquest.app.study.chat.data.OpenedFile.NotAllowed -> notice.value = ChatNotice.FileNotAllowed
                com.pixelquest.app.study.chat.data.OpenedFile.Offline -> notice.value = ChatNotice.Offline
                com.pixelquest.app.study.chat.data.OpenedFile.Corrupted -> notice.value = ChatNotice.FileCorrupted
            }
            opening.value = null
        }
    }

    /** Links a file to an item as its material, or unlinks it (its creator or the leader). */
    fun linkItemFile(itemId: String, fileId: String, linked: Boolean) {
        viewModelScope.launch {
            StudyRefusal.of(chat.linkItemFile(partyId, itemId, fileId, linked))?.let { notice.value = ChatNotice.Refused(it) }
        }
    }

    /** Reports someone else's message (or its file). Said once it's sent. */
    fun report(message: com.pixelquest.app.study.chat.domain.ChatMessage, reason: String) {
        viewModelScope.launch {
            val result = chat.report(message.id, reason)
            notice.value = if (result is SupabaseResult.Success) ChatNotice.Reported else StudyRefusal.of(result)?.let { ChatNotice.Refused(it) } ?: ChatNotice.Offline
        }
    }

    /** Blocks (or unblocks) a message's sender: their messages hide, for the player alone. */
    fun setBlocked(userId: String, blocked: Boolean) {
        viewModelScope.launch {
            if (chat.setBlocked(userId, blocked) !is SupabaseResult.Success) notice.value = ChatNotice.Offline
        }
    }

    /** The leader pins or unpins a file. */
    fun pinFile(file: com.pixelquest.app.study.chat.domain.ChatFile, pinned: Boolean) {
        viewModelScope.launch {
            StudyRefusal.of(chat.pinFile(file, pinned))?.let { notice.value = ChatNotice.Refused(it) }
        }
    }

    /** No app opened it: said, so the player knows why nothing happened. */
    fun noViewer() {
        notice.value = ChatNotice.NoViewer
    }

    fun cancelUpload(clientId: String) {
        viewModelScope.launch { uploads?.cancel(clientId) }
    }

    fun retryUpload(clientId: String) {
        viewModelScope.launch { uploads?.retry(clientId) }
    }

    fun dismissUpload(clientId: String) {
        viewModelScope.launch { uploads?.dismiss(clientId) }
    }

    fun retry(clientId: String) {
        viewModelScope.launch {
            val result = chat.retry(clientId)
            if (result is ChatSend.Refused) notice.value = ChatNotice.Refused(result.refusal)
        }
    }

    fun discard(clientId: String) {
        viewModelScope.launch { chat.discard(clientId) }
    }

    /** Deletes a message: the player's own, or any as the leader (the server checks). */
    fun delete(messageId: String) {
        viewModelScope.launch {
            val result = chat.delete(messageId)
            StudyRefusal.of(result)?.let { notice.value = ChatNotice.Refused(it) }
            if (result is SupabaseResult.NetworkError) notice.value = ChatNotice.Offline
        }
    }

    /** The reader reached the oldest message shown: a page older, from the phone, then the server. */
    fun loadOlder() {
        if (loadingOlder.value) return
        val shown = uiState.value.rows.size
        if (shown < limit.value) {
            if (!serverHasOlder) return
            loadingOlder.value = true
            viewModelScope.launch {
                val result = chat.loadOlder(partyId)
                if (result is SupabaseResult.Success) serverHasOlder = result.data
                limit.value += PAGE
                loadingOlder.value = false
            }
        } else {
            limit.value += PAGE
        }
    }

    internal companion object {
        const val PAGE = 50
    }
}

/**
 * The chat's messages as rows: each with its file, the message it answers, and its sender's name;
 * one sender's messages in a row on the same day make a run, named once. The server's own lines
 * (system) and deleted messages break a run.
 */
internal fun rowsOf(messages: List<ChatMessage>, files: List<ChatFile>, me: String?, names: Map<String, String>, zone: ZoneId): List<ChatMessageRow> {
    val filesById = files.associateBy { it.id }
    val byId = messages.associateBy { it.id }
    fun runner(m: ChatMessage?) = m?.takeIf { it.kind != ChatMessageKind.SYSTEM }?.senderId
    fun day(m: ChatMessage) = m.createdAt.atZone(zone).toLocalDate()
    return messages.mapIndexed { i, message ->
        val before = messages.getOrNull(i - 1)
        val after = messages.getOrNull(i + 1)
        val sender = runner(message)
        val reply = message.replyTo?.let(byId::get)
        ChatMessageRow(
            message = message,
            mine = me != null && message.senderId == me,
            senderName = message.senderId?.let(names::get),
            file = message.fileId?.let(filesById::get),
            replyTo = reply,
            replyToName = reply?.senderId?.let(names::get),
            firstOfRun = sender == null || runner(before) != sender || day(before!!) != day(message),
            lastOfRun = sender == null || runner(after) != sender || day(after!!) != day(message)
        )
    }
}
