package com.pixelquest.app.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/** S051: the coach's words are shown as plain text. */
class AiTextTest {

    @Test
    fun markdownHtmlAndLinks_becomePlainWords() {
        assertEquals("Great week! Keep going.", AiText.plain("**Great week!** __Keep__ going."))
        assertEquals("Read this", AiText.plain("[Read this](https://evil.example/x)"))
        assertEquals("Tap here now", AiText.plain("<a href=\"https://evil.example\">Tap here</a> now"))
        assertEquals("Focus on rest.", AiText.plain("## Focus on rest."))
        assertEquals("SELF_CARE", AiText.plain("SELF_CARE"))
    }

    @Test
    fun controlCharacters_andLength_areCut() {
        assertEquals("one two", AiText.plain("one\u0000\u001b[31m two".replace("[31m", "")))
        assertEquals("a b c", AiText.plain("a\n\nb\t c"))
        assertEquals(AiText.MAX_CHARS, AiText.plain("x".repeat(5000)).length)
    }

    @Test
    fun theInsight_isCleanedAsItIsParsed() {
        val insight = HabitInsightResponse.parseFromJson(
            """{"summary": "**You did 5 of 7.**", "suggestion": "[Click](http://x.example)", "encouragement": "<b>Nice</b>", "highlightCategory": "FITNESS"}"""
        )
        assertEquals("You did 5 of 7.", insight.summary)
        assertEquals("Click", insight.suggestion)
        assertEquals("Nice", insight.encouragement)
        assertEquals("FITNESS", insight.highlightCategory)
    }
}
