package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.chat.data.ChatUploadScheduler
import com.pixelquest.app.study.chat.data.ChatUploads
import com.pixelquest.app.study.chat.data.PickedDocuments
import com.pixelquest.app.study.chat.data.PickedFile
import com.pixelquest.app.study.chat.data.StudyChatRepositoryImpl
import com.pixelquest.app.study.chat.data.UploadRun
import com.pixelquest.app.study.chat.data.local.UploadState
import com.pixelquest.app.study.chat.data.remote.ChatFilesRemote
import com.pixelquest.app.study.chat.data.remote.DownloadReply
import com.pixelquest.app.study.chat.data.remote.FileCall
import com.pixelquest.app.study.chat.data.remote.FinalizeReply
import com.pixelquest.app.study.chat.data.remote.RequestUploadReply
import com.pixelquest.app.study.chat.ui.ChatUploadRow
import com.pixelquest.app.study.chat.ui.StudyChatTags
import com.pixelquest.app.study.chat.ui.UploadBubble
import com.pixelquest.app.testing.FakeChatServer
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.InputStream

/** Sharing a file from the phone: asked for, sent, checked; or rejected, waiting, cancelled, tried again. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChatUploadsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val chatServer = FakeChatServer(me = "aria").apply { members["p1"] = mutableSetOf("aria") }
    private val chat = StudyChatRepositoryImpl(chatServer, db.chatDao(), { "aria" }, clock = { 0L })

    /** The file functions, scripted: what each step answers. */
    private inner class Files : ChatFilesRemote {
        var request: FileCall<RequestUploadReply> = FileCall.Ok(RequestUploadReply("f1", "/storage/v1/object/upload/sign/study-files/p1/x?token=t", "Past paper.pdf", 300))
        var finalize: FileCall<FinalizeReply> = FileCall.Ok(FinalizeReply("ready", messageId = "m1"))
        var offlineMidway = false
        val sentBytes = mutableListOf<Long>()
        override suspend fun requestUpload(partyId: String, name: String, mime: String, sizeBytes: Long) = request
        override suspend fun upload(uploadPath: String, mime: String, maxBytes: Long, open: () -> InputStream?, onProgress: (Long) -> Unit): FileCall<Long> {
            val bytes = open()?.readBytes() ?: return FileCall.Refused("unreadable")
            if (bytes.size > maxBytes) return FileCall.Refused("too_large")
            if (offlineMidway) {
                onProgress(bytes.size / 2L)
                return FileCall.Offline
            }
            onProgress(bytes.size.toLong())
            sentBytes += bytes.size.toLong()
            return FileCall.Ok(bytes.size.toLong())
        }
        override suspend fun finalize(fileId: String) = finalize
        override suspend fun downloadUrl(fileId: String): FileCall<DownloadReply> = FileCall.Refused("not_allowed")
    }

    private val files = Files()
    private val kept = mutableListOf<String>()
    private val released = mutableListOf<String>()
    private var content = ByteArray(300_000) { 1 }
    private val documents = object : PickedDocuments {
        override fun open(uri: String): InputStream? = if (uri == "content://gone") null else ByteArrayInputStream(content)
        override fun keep(uri: String) {
            kept += uri
        }
        override fun release(uri: String) {
            released += uri
        }
    }
    private val scheduled = mutableListOf<String>()
    private val cancelled = mutableListOf<String>()
    private val scheduler = object : ChatUploadScheduler {
        override fun schedule(clientId: String) {
            scheduled += clientId
        }
        override fun cancel(clientId: String) {
            cancelled += clientId
        }
    }
    private val uploads = ChatUploads(db.chatDao(), files, documents, chat, scheduler, clock = { 0L })
    private val pdf = PickedFile("content://docs/1", "Past paper.pdf", "application/pdf", 300_000)

    private fun party() = runBlocking {
        db.partyDao().upsertParties(listOf(
            PartyEntity(
                id = "p1", name = "p1", leaderId = "aria", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
                createdAt = 0, total = 0, entryCount = 0, memberCount = 1, lastEntryDate = null, myRole = PartyRole.LEADER,
                myStatus = MemberStatus.ACTIVE, syncedAt = 0, partyType = PartyType.STUDY
            )
        ))
    }

    @After
    fun tearDown() = db.closeAfterWrites()

    private fun only() = runBlocking { db.chatDao().uploadsUnderWay() + listOfNotNull(scheduled.lastOrNull()?.let { db.chatDao().getUpload(it) }) }.distinct()

    @Test
    fun aFile_isAsked_sent_checked_andLeavesTheChatForItsMessage() = runBlocking {
        party()
        assertTrue(uploads.share("p1", pdf))
        assertEquals(listOf(pdf.uri), kept)
        val id = scheduled.single()
        assertEquals(UploadRun.DONE, uploads.run(id))
        assertEquals(listOf(300_000L), files.sentBytes)
        assertEquals("gone, for its message", null, db.chatDao().getUpload(id))
        assertEquals(listOf(pdf.uri), released)
    }

    @Test
    fun whatTheServerRefuses_isRejected_withWhy() = runBlocking {
        party()
        files.finalize = FileCall.Ok(FinalizeReply("rejected", reason = "macros"))
        uploads.share("p1", pdf)
        val id = scheduled.single()
        assertEquals(UploadRun.DONE, uploads.run(id))
        val rejected = db.chatDao().getUpload(id)!!
        assertEquals(UploadState.REJECTED, rejected.state)
        assertEquals("macros", rejected.reason)
        uploads.dismiss(id)
        assertEquals(null, db.chatDao().getUpload(id))

        files.request = FileCall.Refused("too_many_uploads")
        uploads.share("p1", pdf)
        uploads.run(scheduled.last())
        assertEquals("too_many_uploads", db.chatDao().getUpload(scheduled.last())!!.reason)
    }

    @Test
    fun noConnection_partWay_waitsAndStartsAgain() = runBlocking {
        party()
        files.offlineMidway = true
        uploads.share("p1", pdf)
        val id = scheduled.single()
        assertEquals(UploadRun.RETRY_LATER, uploads.run(id))
        assertEquals(UploadState.QUEUED, db.chatDao().getUpload(id)!!.state)
        files.offlineMidway = false
        assertEquals(UploadRun.DONE, uploads.run(id))
        assertEquals(null, db.chatDao().getUpload(id))
    }

    @Test
    fun moreBytesThanAFileMayHave_isStoppedOnTheWay() = runBlocking {
        party()
        content = ByteArray(20 * 1024 * 1024 + 10)
        uploads.share("p1", pdf.copy(sizeBytes = 10))
        val id = scheduled.single()
        uploads.run(id)
        assertEquals("too_large", db.chatDao().getUpload(id)!!.reason)
    }

    @Test
    fun aShare_isCancelled_orTriedAgain() = runBlocking {
        party()
        uploads.share("p1", pdf.copy(uri = "content://gone"))
        val id = scheduled.single()
        uploads.run(id)
        assertEquals(UploadState.FAILED, db.chatDao().getUpload(id)!!.state)
        uploads.retry(id)
        assertEquals(UploadState.QUEUED, db.chatDao().getUpload(id)!!.state)
        assertEquals(2, scheduled.size)
        uploads.cancel(id)
        assertEquals(listOf(id), cancelled)
        assertEquals(null, db.chatDao().getUpload(id))
        assertTrue(!uploads.share("gone", pdf))
    }

    @Test
    fun itsBubble_showsHowFarItsGot_andCanBeCancelled() {
        var stopped: String? = null
        val row = ChatUploadRow("u1", "Past paper.pdf", "application/pdf", 1_000_000, 400_000, UploadState.UPLOADING, null)
        composeTestRule.setContent { PixelQuestTheme { UploadBubble(row, onCancel = { stopped = it }, onRetry = {}, onDismiss = {}) } }
        composeTestRule.onNodeWithText("Sending, 40%").assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.UPLOAD_PROGRESS).assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.UPLOAD_CANCEL).performClick()
        assertEquals("u1", stopped)
    }
}
