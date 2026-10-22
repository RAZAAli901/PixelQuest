package com.pixelquest.app.security

import com.pixelquest.app.domain.validation.InputRules
import com.pixelquest.app.study.chat.data.remote.ChatMessageDto
import com.pixelquest.app.study.chat.data.remote.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * S054: what other players write is shown as plain text. No screen turns text into HTML, markdown
 * or tappable links, and text from the server loses control characters and direction overrides
 * before it is shown.
 */
class PlainTextRenderingTest {

    private val main = listOf(File("src/main"), File("app/src/main")).first { it.exists() }

    @Test
    fun noScreen_rendersHtmlMarkdownOrLinks() {
        val renderers = Regex(
            """fromHtml|HtmlCompat|Html\.from|Linkify|LinkAnnotation|UrlAnnotation|ClickableText|withLink\(|autoLink|setMovementMethod|LinkMovementMethod|Markwon|commonmark|richtext"""
        )
        val found = main.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml") }
            .filter { renderers.containsMatchIn(it.readText()) }.map { it.name }.toList()
        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun textFromTheServer_isCleanedBeforeItIsShown() {
        val message = ChatMessageDto(
            id = "m1", partyId = "p1", senderId = "u1", kind = "text",
            body = "pay here‮\u0000 <b>now</b>\nthanks", createdAt = "2026-10-22T09:00:00Z"
        ).toDomain()
        // Tags stay as the characters they are: Text shows them, nothing interprets them.
        assertEquals("pay here <b>now</b>\nthanks", message.body)
        assertEquals("x".repeat(InputRules.Field.CHAT_MESSAGE.max), InputRules.shown("x".repeat(9000), InputRules.Field.CHAT_MESSAGE))
        assertEquals("Study club", InputRules.shown("Study\nclub⁦", InputRules.Field.PARTY_NAME))
    }

    @Test
    fun theMappersFromTheServer_cleanTheirText() {
        val app = main.resolve("java/com/pixelquest/app")
        val mappers = listOf(
            "study/chat/data/remote/ChatDtos.kt", "darood/data/remote/PartyDtos.kt", "party/data/PartyRepository.kt",
            "study/data/remote/StudyDtos.kt", "study/data/remote/StudyReminderDtos.kt",
        )
        assertEquals(emptyList<String>(), mappers.filterNot { "InputRules.shown" in app.resolve(it).readText() })
    }
}
