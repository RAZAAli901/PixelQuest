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
    private val study: StudyRepository
) : ViewModel() {

    val partyId: String = savedStateHandle[StudyPartyViewModel.ARG_PARTY] ?: ""

    private val limit = MutableStateFlow(PAGE)
    private val draft = MutableStateFlow("")
    private val replyingTo = MutableStateFlow<String?>(null)
    private val loadingOlder = MutableStateFlow(false)
    private val notice = MutableStateFlow<ChatNotice?>(null)
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
        combine(messages, chat.observeFiles(partyId), ::Pair),
        people,
        combine(draft, replyingTo, loadingOlder, ::Triple),
        combine(readAtOpening, loaded, ::Pair),
        notice
    ) { (all, files), people, (text, replyId, older), (readAt, ready), said ->
        val me = study.myId()
        val rows = rowsOf(all, files, me, people.names, study.zone())
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
            notice = said
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
