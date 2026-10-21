package com.pixelquest.app.study.chat.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pixelquest.app.study.chat.data.local.ChatDao
import com.pixelquest.app.study.chat.data.local.ChatUploadEntity
import com.pixelquest.app.study.chat.data.local.UploadState
import com.pixelquest.app.study.chat.data.remote.ChatFilesRemote
import com.pixelquest.app.study.chat.data.remote.FileCall
import com.pixelquest.app.study.chat.domain.StudyChatConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The documents the player picked, read the way the system allows: the picker's grant to read
 * each, kept while its upload is under way (so it survives the app closing), and given back after.
 */
interface PickedDocuments {
    fun open(uri: String): InputStream?
    fun keep(uri: String) {}
    fun release(uri: String) {}
}

@Singleton
class ContentResolverPickedDocuments @Inject constructor(
    @ApplicationContext private val context: Context
) : PickedDocuments {
    override fun open(uri: String): InputStream? {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != "content") return null
        return runCatching { context.contentResolver.openInputStream(parsed) }.getOrNull()
    }

    override fun keep(uri: String) {
        runCatching { context.contentResolver.takePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    override fun release(uri: String) {
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}

/** Runs a share in the background, once there's a connection: WorkManager in the app, a fake in tests. */
interface ChatUploadScheduler {
    fun schedule(clientId: String)
    fun cancel(clientId: String)
}

/** What a run of a share came to. */
enum class UploadRun { DONE, RETRY_LATER }

/**
 * Sharing a file in a study party's chat (docs/study-chat.md, How a file is shared), from the
 * phone's side: asked for (request-upload), its bytes sent to the one path the server chose, then
 * checked (finalize-upload). Each step is saved as it's reached (chat_upload), so the chat shows
 * how far it's got, and a run after the app was closed picks it up again. What the server refuses
 * is rejected with its reason; no connection waits for a later run.
 */
@Singleton
class ChatUploads(
    private val dao: ChatDao,
    private val remote: ChatFilesRemote,
    private val documents: PickedDocuments,
    private val chat: StudyChatRepository,
    private val scheduler: ChatUploadScheduler?,
    private val clock: () -> Long
) {
    @Inject
    constructor(dao: ChatDao, remote: ChatFilesRemote, documents: PickedDocuments, chat: StudyChatRepository, scheduler: ChatUploadScheduler) :
        this(dao, remote, documents, chat, scheduler, System::currentTimeMillis)

    fun observe(partyId: String): Flow<List<ChatUploadEntity>> = dao.observeUploads(partyId)

    /** Shares [file] in [partyId]'s chat: saved, then sent in the background. False for a party not on the phone. */
    suspend fun share(partyId: String, file: PickedFile): Boolean {
        val upload = ChatUploadEntity(
            clientId = UUID.randomUUID().toString(), partyId = partyId, uri = file.uri, displayName = file.name,
            mime = file.mime, sizeBytes = file.sizeBytes, createdAt = clock()
        )
        if (!dao.insertUploadIfKept(upload)) return false
        documents.keep(file.uri)
        scheduler?.schedule(upload.clientId)
        return true
    }

    /** Stops a share: nothing more is sent, and it leaves the chat. A file asked for and never finished expires on the server. */
    suspend fun cancel(clientId: String) {
        val upload = dao.getUpload(clientId) ?: return
        scheduler?.cancel(clientId)
        dao.deleteUpload(clientId)
        documents.release(upload.uri)
    }

    /** Tries a failed share again, from the start (a new request: the old URL's 5 minutes are gone). */
    suspend fun retry(clientId: String) {
        val upload = dao.getUpload(clientId) ?: return
        if (upload.state != UploadState.FAILED) return
        dao.upsertUpload(upload.copy(state = UploadState.QUEUED, fileId = null, sentBytes = 0, reason = null))
        documents.keep(upload.uri)
        scheduler?.schedule(clientId)
    }

    /** Takes a rejected share off the chat; the reason was said. */
    suspend fun dismiss(clientId: String) {
        val upload = dao.getUpload(clientId) ?: return
        if (upload.state == UploadState.REJECTED || upload.state == UploadState.FAILED) {
            dao.deleteUpload(clientId)
            documents.release(upload.uri)
        }
    }

    /** One run of a share, from wherever it got to. */
    suspend fun run(clientId: String): UploadRun {
        var upload = dao.getUpload(clientId) ?: return UploadRun.DONE
        if (upload.state !in setOf(UploadState.QUEUED, UploadState.UPLOADING, UploadState.CHECKING)) return UploadRun.DONE

        if (upload.state != UploadState.CHECKING) {
            // Asked for afresh each time it's sent: an upload URL is honoured for 5 minutes only.
            val asked = remote.requestUpload(upload.partyId, upload.displayName, upload.mime, upload.sizeBytes)
            val reply = when (asked) {
                is FileCall.Ok -> asked.data
                is FileCall.Refused -> return reject(upload, asked.error)
                FileCall.Offline -> return waitFor(upload.copy(state = UploadState.QUEUED, sentBytes = 0))
                FileCall.SignedOut -> return fail(upload, "signed_out")
            }
            upload = upload.copy(fileId = reply.fileId, state = UploadState.UPLOADING, sentBytes = 0, displayName = reply.displayName)
            dao.upsertUpload(upload)
            var shown = 0L
            val sent = remote.upload(reply.uploadPath, upload.mime, StudyChatConfig.FILE_MAX_BYTES, { documents.open(upload.uri) }) { bytes ->
                // Saved now and then, not on every block: enough for the bar to move smoothly.
                if (bytes - shown >= PROGRESS_STEP || bytes == upload.sizeBytes) {
                    shown = bytes
                    dao.setSentBytes(clientId, bytes)
                }
            }
            when (sent) {
                is FileCall.Ok -> Unit
                is FileCall.Refused -> return if (sent.error == "unreadable") fail(upload, sent.error) else reject(upload, sent.error)
                FileCall.Offline -> return waitFor(upload.copy(state = UploadState.QUEUED, sentBytes = 0, fileId = null))
                FileCall.SignedOut -> return fail(upload, "signed_out")
            }
            upload = upload.copy(state = UploadState.CHECKING, sentBytes = upload.sizeBytes.coerceAtLeast(0))
            dao.upsertUpload(upload)
        }

        val checked = remote.finalize(upload.fileId ?: return waitFor(upload.copy(state = UploadState.QUEUED)))
        return when (checked) {
            is FileCall.Ok -> if (checked.data.status == "ready") {
                // Its message is the server's: brought in, and the share leaves the chat for it.
                chat.refresh(upload.partyId)
                dao.deleteUpload(clientId)
                documents.release(upload.uri)
                UploadRun.DONE
            } else {
                reject(upload, checked.data.reason ?: "refused")
            }
            // The bytes never arrived: sent again from the start.
            is FileCall.Refused -> if (checked.error == "not_uploaded") waitFor(upload.copy(state = UploadState.QUEUED, fileId = null, sentBytes = 0)) else reject(upload, checked.error)
            FileCall.Offline -> waitFor(upload)
            FileCall.SignedOut -> fail(upload, "signed_out")
        }
    }

    private suspend fun reject(upload: ChatUploadEntity, reason: String): UploadRun {
        dao.upsertUpload(upload.copy(state = UploadState.REJECTED, reason = reason))
        documents.release(upload.uri)
        return UploadRun.DONE
    }

    private suspend fun fail(upload: ChatUploadEntity, reason: String): UploadRun {
        dao.upsertUpload(upload.copy(state = UploadState.FAILED, reason = reason))
        return UploadRun.DONE
    }

    private suspend fun waitFor(upload: ChatUploadEntity): UploadRun {
        dao.upsertUpload(upload)
        return UploadRun.RETRY_LATER
    }

    private companion object {
        const val PROGRESS_STEP = 256L * 1024
    }
}
