package com.pixelquest.app.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 13: Unit tests verifying parsing of sample Gemini responses into HabitInsightResponse,
 * including graceful handling of malformed or unexpected responses.
 */
class HabitInsightParserTest {

    @Test
    fun parseValidJsonResponse_extractsAllFields() {
        val sampleJson = """
            {
              "summary": "Great momentum in morning fitness routines with a 7-day streak.",
              "suggestion": "Schedule your study sessions earlier in the afternoon to avoid fatigue.",
              "encouragement": "Keep pushing forward, you are on track for Level 5!",
              "highlightCategory": "FITNESS",
              "specificTaskCallout": "Morning workout routine"
            }
        """.trimIndent()

        val insight = HabitInsightResponse.parseFromJson(sampleJson)

        assertEquals("Great momentum in morning fitness routines with a 7-day streak.", insight.summary)
        assertEquals("Schedule your study sessions earlier in the afternoon to avoid fatigue.", insight.suggestion)
        assertEquals("Keep pushing forward, you are on track for Level 5!", insight.encouragement)
        assertEquals("FITNESS", insight.highlightCategory)
        assertEquals("Morning workout routine", insight.specificTaskCallout)
    }

    @Test
    fun parseMarkdownWrappedJsonResponse_stripsCodeFences() {
        val wrappedJson = """
            ```json
            {
              "summary": "Solid consistency across all active habits this week.",
              "suggestion": "Add one rest day per week to maintain recovery.",
              "encouragement": "A true hero balances effort and discipline!",
              "highlightCategory": "HEALTH",
              "specificTaskCallout": null
            }
            ```
        """.trimIndent()

        val insight = HabitInsightResponse.parseFromJson(wrappedJson)

        assertEquals("Solid consistency across all active habits this week.", insight.summary)
        assertEquals("Add one rest day per week to maintain recovery.", insight.suggestion)
        assertEquals("A true hero balances effort and discipline!", insight.encouragement)
        assertEquals("HEALTH", insight.highlightCategory)
        assertNull(insight.specificTaskCallout)
    }

    @Test
    fun parseWithOptionalNullFields_succeedsGracefully() {
        val minimalJson = """
            {
              "summary": "Observing steady routine formation.",
              "suggestion": "Try setting recurring reminders.",
              "encouragement": "Every day is progress."
            }
        """.trimIndent()

        val insight = HabitInsightResponse.parseFromJson(minimalJson)

        assertEquals("Observing steady routine formation.", insight.summary)
        assertNull(insight.highlightCategory)
        assertNull(insight.specificTaskCallout)
    }

    @Test
    fun parseMalformedJson_failsGracefullyWithExpectedException() {
        val brokenJson = "{\"summary\": \"Unfinished string without closing quote"

        var caught: IllegalArgumentException? = null
        try {
            HabitInsightResponse.parseFromJson(brokenJson)
        } catch (e: IllegalArgumentException) {
            caught = e
        }

        assertNotNull("Malformed JSON should produce an IllegalArgumentException", caught)
        assertTrue("Exception message should indicate malformed payload", caught?.message?.contains("Malformed") == true)
    }

    @Test
    fun parseMissingRequiredFields_failsGracefullyWithExpectedException() {
        val missingFieldsJson = """
            {
              "summary": "Only summary provided"
            }
        """.trimIndent()

        var caught: IllegalArgumentException? = null
        try {
            HabitInsightResponse.parseFromJson(missingFieldsJson)
        } catch (e: IllegalArgumentException) {
            caught = e
        }

        assertNotNull("Missing required fields should produce an IllegalArgumentException", caught)
        assertTrue("Exception message should note missing required fields", caught?.message?.contains("Missing required insight fields") == true)
    }

    @Test
    fun parseResponseWithConversationalPreambleAndPostamble_successfullyExtractsJson() {
        val mixedText = """
            Certainly! Here is your quest habit debrief:
            ```json
            {
              "summary": "You have sustained a 5-day streak in fitness.",
              "suggestion": "Keep active routines consistent.",
              "encouragement": "Victory awaits you, hero!",
              "highlightCategory": "FITNESS",
              "specificTaskCallout": "Morning workout"
            }
            ```
            Hope this guidance aids your adventure!
        """.trimIndent()

        val insight = HabitInsightResponse.parseFromJson(mixedText)
        assertEquals("You have sustained a 5-day streak in fitness.", insight.summary)
        assertEquals("FITNESS", insight.highlightCategory)
        assertEquals("Morning workout", insight.specificTaskCallout)
    }

    @Test
    fun parseResponseWithLiteralNullStrings_normalizesToNull() {
        val literalNullJson = """
            {
              "summary": "Great routine momentum.",
              "suggestion": "Maintain pace.",
              "encouragement": "Well done.",
              "highlightCategory": "null",
              "specificTaskCallout": "NULL"
            }
        """.trimIndent()

        val insight = HabitInsightResponse.parseFromJson(literalNullJson)
        assertNull("Literal 'null' should be converted to null", insight.highlightCategory)
        assertNull("Literal 'NULL' should be converted to null", insight.specificTaskCallout)
    }
}
