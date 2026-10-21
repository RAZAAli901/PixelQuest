package com.pixelquest.app.study.chat

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.chat.data.ChatUploadScheduler
import com.pixelquest.app.study.chat.data.ChatUploads
import com.pixelquest.app.study.chat.data.OpenedFile
import com.pixelquest.app.study.chat.data.PickedDocuments
import com.pixelquest.app.study.chat.data.PickedFile
import com.pixelquest.app.study.chat.data.StudyChatRepositoryImpl
import com.pixelquest.app.study.chat.data.remote.ChatFilesRemote
import com.pixelquest.app.study.chat.data.remote.DownloadReply
import com.pixelquest.app.study.chat.data.remote.FileCall
import com.pixelquest.app.study.chat.data.remote.FinalizeReply
import com.pixelquest.app.study.chat.data.remote.RequestUploadReply
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatActions
import com.pixelquest.app.study.chat.ui.StudyChatPane
import com.pixelquest.app.study.chat.ui.StudyChatTags
import com.pixelquest.app.study.chat.ui.StudyChatViewModel
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.ui.StudyPartyViewModel
import com.pixelquest.app.testing.FakeChatServer
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** The study chat through its screen: text, a PDF shared with its progress, ready and opened, and a rejected file. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyChatFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val zone = ZoneId.of("UTC")
    private val server = FakeDaroodServer(me = "aria").apply { usernames += mapOf("aria" to "aria_k", "bram" to "bram"); now = Instant.parse("2026-10-21T10:00:00Z") }
    private val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
    private val chatServer = FakeChatServer(me = "aria", now = server.now).apply { members[party] = mutableSetOf("aria", "bram") }
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { zone })
    private val study = StudyRepositoryImpl(FakeStudyServer(server), db.studyDao(), db.partyDao(), { server.me }, clock = { server.now.toEpochMilli() }, timeZone = { zone })
    private val chat = StudyChatRepositoryImpl(chatServer, db.chatDao(), { "aria" }, clock = { server.now.toEpochMilli() })

    /** The file functions: the server stores the file and its message once it's checked, or rejects it. */
    private var rejectAs: String? = null
    private val files = object : ChatFilesRemote {
        override suspend fun requestUpload(partyId: String, name: String, mime: String, sizeBytes: Long) =
            FileCall.Ok(RequestUploadReply("f-$name", "/storage/v1/object/upload/sign/study-files/$partyId/x?token=t", name, 300))
        override suspend fun upload(uploadPath: String, mime: String, maxBytes: Long, open: () -> InputStream?, onProgress: (Long) -> Unit): FileCall<Long> {
            val size = open()!!.readBytes().size.toLong()
            onProgress(size / 2)
            onProgress(size)
            return FileCall.Ok(size)
        }
        override suspend fun finalize(fileId: String): FileCall<FinalizeReply> {
            rejectAs?.let { return FileCall.Ok(FinalizeReply("rejected", reason = it)) }
            val name = fileId.removePrefix("f-")
            chatServer.files += ChatFile(fileId, party, "aria", name, "application/pdf", 8, null, ChatFileStatus.READY, null, false, server.now, server.now)
            chatServer.messages += ChatMessage("m-$fileId", party, "aria", ChatMessageKind.FILE, null, fileId, null, null, server.now, null)
            return FileCall.Ok(FinalizeReply("ready", messageId = "m-$fileId"))
        }
        override suspend fun downloadUrl(fileId: String): FileCall<DownloadReply> = FileCall.Refused("not_allowed")
    }
    private val documents = object : PickedDocuments {
        override fun open(uri: String): InputStream = ByteArrayInputStream("%PDF-1.7".toByteArray())
    }
    private val scheduled = mutableListOf<String>()
    private val uploads = ChatUploads(db.chatDao(), files, documents, chat, object : ChatUploadScheduler {
        override fun schedule(clientId: String) { scheduled += clientId }
        override fun cancel(clientId: String) {}
    }, clock = { server.now.toEpochMilli() })
    private val opened = mutableListOf<String>()
    private val viewModels = TestViewModels()

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(300) {
            composeTestRule.waitForIdle()
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun text_thenAPdf_thenARejectedFile() {
        runBlocking { parties.refreshParties(); parties.refreshParty(party) }
        val vm = viewModels.keep(StudyChatViewModel::class.java) {
            StudyChatViewModel(
                SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), chat, parties, study,
                picked = { PickedFile(it, "Past paper.pdf", "application/pdf", 8) }, uploads = uploads,
                fetcher = { file -> opened += file.id; OpenedFile.Saved(java.io.File("x"), file.mime) },
                warned = com.pixelquest.app.study.chat.data.MemoryFileWarningSeen().apply { markSeen(party) }
            )
        }
        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            PixelQuestTheme {
                StudyChatPane(state, zone, ChatActions(
                    onDraftChange = vm::changeDraft, onSend = vm::send, onAttach = vm::attach,
                    onOpenFile = vm::openFile, onDismissUpload = vm::dismissUpload
                ))
            }
        }
        waitFor("loaded") { vm.uiState.value.loaded }

        // Text.
        composeTestRule.onNodeWithTag(StudyChatTags.INPUT).performTextInput("Anyone done Q3?")
        composeTestRule.onNodeWithTag(StudyChatTags.SEND).performClick()
        waitFor("sent") { chatServer.messages.any { it.body == "Anyone done Q3?" } }
        composeTestRule.onNodeWithText("Anyone done Q3?").assertExists()

        // A PDF: picked, sent (its bubble shows while it goes), then ready as a file message, and opened.
        vm.attach("content://docs/1")
        waitFor("waiting to send") { vm.uiState.value.attachment != null }
        composeTestRule.onNodeWithTag(StudyChatTags.SEND).performClick()
        waitFor("its share") { vm.uiState.value.uploads.isNotEmpty() }
        composeTestRule.onNodeWithTag(StudyChatTags.upload(scheduled.single())).assertExists()
        runBlocking { uploads.run(scheduled.single()) }
        waitFor("ready, as its message") { vm.uiState.value.uploads.isEmpty() && vm.uiState.value.rows.any { it.file?.displayName == "Past paper.pdf" } }
        composeTestRule.onNodeWithTag(StudyChatTags.file("f-Past paper.pdf")).performClick()
        waitFor("opened") { opened == listOf("f-Past paper.pdf") }

        // A file the server rejects: its bubble says why.
        rejectAs = "macros"
        vm.attach("content://docs/2")
        waitFor("waiting") { vm.uiState.value.attachment != null }
        vm.send()
        waitFor("its share") { scheduled.size == 2 }
        runBlocking { uploads.run(scheduled.last()) }
        waitFor("rejected") { vm.uiState.value.uploads.singleOrNull()?.reason == "macros" }
        composeTestRule.onNodeWithText("Not shared: it has macros").assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.UPLOAD_DISMISS).performClick()
        waitFor("dismissed") { vm.uiState.value.uploads.isEmpty() }
        assertEquals(1, chatServer.files.size)
    }
}

