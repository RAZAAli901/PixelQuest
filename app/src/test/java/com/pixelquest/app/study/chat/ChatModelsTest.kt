package com.pixelquest.app.study.chat

import com.pixelquest.app.study.chat.data.remote.ChatFileDto
import com.pixelquest.app.study.chat.data.remote.ChatMessageDto
import com.pixelquest.app.study.chat.data.remote.NewChatMessageDto
import com.pixelquest.app.study.chat.data.remote.toDomain
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatFileType
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.domain.StudyChatConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** The study chat's limits are the server's, and its rows read as the server sends them. */
class ChatModelsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun repoFile(path: String) = listOf(File("../$path"), File(path)).first { it.exists() }

    @Test
    fun theLimitsAndTypes_areTheServers() {
        val sql = repoFile("supabase/migrations/20261019000053_chat_limits.sql").readText()
        fun server(name: String) = Regex("""(\d+) AS $name\b""").find(sql)!!.groupValues[1].toLong()
        assertEquals(server("message_max_chars"), StudyChatConfig.MESSAGE_MAX_CHARS.toLong())
        assertEquals(server("file_max_bytes"), StudyChatConfig.FILE_MAX_BYTES)
        assertEquals(server("party_max_bytes"), StudyChatConfig.PARTY_MAX_BYTES)
        assertEquals(server("uploads_per_day"), StudyChatConfig.UPLOADS_PER_DAY.toLong())
        assertEquals(server("messages_per_minute"), StudyChatConfig.MESSAGES_PER_MINUTE.toLong())
        assertEquals(server("file_name_max_chars"), StudyChatConfig.FILE_NAME_MAX_CHARS.toLong())
        val types = Regex("""\('([a-z0-9./+-]+)', ARRAY\[([^\]]+)]\)""").findAll(sql)
            .associate { it.groupValues[1] to it.groupValues[2].split(",").map { e -> e.trim().trim('\'') } }
        assertEquals(types, StudyChatConfig.ALLOWED_TYPES)
        // And the Edge Functions' own copy.
        val shared = repoFile("supabase/functions/_shared/study_files.ts").readText()
        for ((mime, extensions) in StudyChatConfig.ALLOWED_TYPES) {
            assertTrue(mime, shared.contains("\"$mime\": [${extensions.joinToString(", ") { "\"$it\"" }}]"))
        }
    }

    @Test
    fun aMessageRow_readsAsTheServerSendsIt() {
        val row = """{"id":"m1","party_id":"p1","sender_id":"u1","kind":"text","body":"Anyone done Q3?","file_id":null,
            "reply_to":"m0","client_id":"c1","created_at":"2026-10-21T10:00:00+00:00","deleted_at":null}"""
        val message = json.decodeFromString<ChatMessageDto>(row).toDomain()
        assertEquals(ChatMessageKind.TEXT, message.kind)
        assertEquals("m0", message.replyTo)
        assertEquals(Instant.parse("2026-10-21T10:00:00Z"), message.createdAt)
        assertFalse(message.deleted)
        val gone = json.decodeFromString<ChatMessageDto>(row.replace("\"deleted_at\":null", "\"deleted_at\":\"2026-10-21T11:00:00Z\"")).toDomain()
        assertTrue(gone.deleted)
    }

    @Test
    fun whatsSent_isTextAsTheSender_withoutWhoOrWhen() {
        val sent = json.encodeToString(NewChatMessageDto(partyId = "p1", body = "Hi", clientId = "c1"))
        assertTrue(sent.contains("\"kind\":\"text\""))
        for (field in listOf("sender_id", "created_at", "file_id", "deleted_at")) assertFalse(field, sent.contains(field))
    }

    @Test
    fun aFileRow_andItsKind() {
        val row = """{"id":"f1","party_id":"p1","uploader_id":"u1","display_name":"Past paper.pdf","mime":"application/pdf",
            "size_bytes":1200,"sha256":"ab","status":"ready","reject_reason":null,"pinned":true,"created_at":"2026-10-21T10:00:00Z","ready_at":null}"""
        val file = json.decodeFromString<ChatFileDto>(row).toDomain()
        assertEquals(ChatFileStatus.READY, file.status)
        assertEquals(ChatFileType.PDF, file.type)
        assertTrue(file.pinned)
        assertEquals(ChatFileType.EXCEL, ChatFileType.of("text/csv"))
        assertEquals(ChatFileType.WORD, ChatFileType.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertEquals(ChatFileStatus.REJECTED, ChatFileStatus.fromWire("something new"))
    }
}
