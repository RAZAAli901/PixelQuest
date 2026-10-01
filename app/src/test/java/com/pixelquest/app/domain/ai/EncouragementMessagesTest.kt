package com.pixelquest.app.domain.ai

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.prefs.EncouragementPack
import com.pixelquest.app.data.local.prefs.EncouragementPackStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EncouragementMessagesTest {

    private val today = LocalDate.of(2026, 10, 2)
    private val aiLines = listOf("You showed up five days running.", "Seventy percent this week is solid.")

    @Test
    fun staticBank_isDeterministicPerDayAndTask() {
        val a = StaticEncouragementBank.messageFor(today, 7, isSimpleMode = false)
        assertEquals(a, StaticEncouragementBank.messageFor(today, 7, isSimpleMode = false))
        assertTrue(StaticEncouragementBank.gamified.contains(a))
        assertTrue(StaticEncouragementBank.calm.contains(StaticEncouragementBank.messageFor(today, 7, isSimpleMode = true)))
    }

    @Test
    fun staticBank_calmLinesPassSimpleModeSanitizer() {
        StaticEncouragementBank.calm.forEach {
            assertEquals(it, EncouragementSanitizer.cleanLine(it, isSimpleMode = true))
        }
    }

    @Test
    fun sanitizer_dropsUnsafeOrOffToneLines() {
        assertNull(EncouragementSanitizer.cleanLine("Visit https://example.com for tips", false))
        assertNull(EncouragementSanitizer.cleanLine("x".repeat(EncouragementSanitizer.MAX_CHARS + 1), false))
        assertNull(EncouragementSanitizer.cleanLine("Go", false))
        assertNull(EncouragementSanitizer.cleanLine("Keep your streak alive, hero!", isSimpleMode = true))
        assertEquals("Keep your streak alive, hero!", EncouragementSanitizer.cleanLine("Keep your streak alive, hero!", false))
        assertEquals("Small steps count.", EncouragementSanitizer.cleanLine("  \"**Small steps count.**\" ", true))
    }

    @Test
    fun prompt_parsesFencedJson_andRejectsGarbage() {
        val raw = "```json\n{\"messages\": [\"One\", \"Two\"]}\n```"
        assertEquals(listOf("One", "Two"), EncouragementPackPrompt.parse(raw))
        assertTrue(EncouragementPackPrompt.parse("Sorry, I can't help").isEmpty())
        assertTrue(EncouragementPackPrompt.parse("{\"other\": 1}").isEmpty())
    }

    @Test
    fun prompt_containsOnlyNumbers() {
        val prompt = EncouragementPackPrompt.prompt(EncouragementPackInput(4, 71, 3, HabitInsightTone.GAMIFIED_HEROIC))
        assertTrue(prompt.contains("4"))
        assertTrue(prompt.contains("71%"))
        assertFalse(prompt.contains("Read 15 Pages"))
    }

    @Test
    fun provider_usesFreshPackInMatchingTone() {
        val provider = AiEncouragementProvider({ EncouragementPack(today, HabitInsightTone.GAMIFIED_HEROIC, aiLines) })
        assertTrue(aiLines.contains(provider.messageFor(today, 1, isSimpleMode = false)))
    }

    @Test
    fun provider_fallsBackWhenStaleWrongToneOrEmpty() {
        val stale = AiEncouragementProvider({ EncouragementPack(today.minusDays(2), HabitInsightTone.GAMIFIED_HEROIC, aiLines) })
        assertTrue(StaticEncouragementBank.gamified.contains(stale.messageFor(today, 1, false)))

        val wrongTone = AiEncouragementProvider({ EncouragementPack(today, HabitInsightTone.GAMIFIED_HEROIC, aiLines) })
        assertTrue(StaticEncouragementBank.calm.contains(wrongTone.messageFor(today, 1, isSimpleMode = true)))

        val none = AiEncouragementProvider({ null })
        assertTrue(StaticEncouragementBank.gamified.contains(none.messageFor(today, 1, false)))
    }

    @Test
    fun provider_yesterdaysPackStillCounts() {
        val provider = AiEncouragementProvider({ EncouragementPack(today.minusDays(1), HabitInsightTone.GAMIFIED_HEROIC, aiLines) })
        assertTrue(aiLines.contains(provider.messageFor(today, 1, false)))
    }

    @Test
    fun packStore_roundTrips() {
        val store = EncouragementPackStore(ApplicationProvider.getApplicationContext<Application>())
        store.clear()
        assertNull(store.load())
        val pack = EncouragementPack(today, HabitInsightTone.SIMPLE_MINIMALIST, aiLines)
        store.save(pack)
        assertEquals(pack, store.load())
    }
}
