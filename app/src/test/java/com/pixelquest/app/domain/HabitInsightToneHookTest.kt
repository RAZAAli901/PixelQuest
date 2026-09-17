package com.pixelquest.app.domain

import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.ai.HabitInsightTone
import com.pixelquest.app.domain.ai.HabitInsightToneHook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Step 31: Unit test confirming HabitInsightToneHook compiles, wires, and evaluates correctly
 * according to Simple Mode state, with no real Gemini API behavior expected yet.
 */
class HabitInsightToneHookTest {

    private lateinit var toneHook: HabitInsightToneHook

    @Before
    fun setUp() {
        toneHook = DefaultHabitInsightToneHook()
    }

    @Test
    fun testGamifiedMode_resolvesHeroicTone() {
        val tone = toneHook.resolveTone(isSimpleModeEnabled = false)
        assertEquals(HabitInsightTone.GAMIFIED_HEROIC, tone)

        val promptGuidance = toneHook.getSystemPromptGuidance(tone)
        assertTrue(promptGuidance.contains("questmaster", ignoreCase = true))
        assertTrue(promptGuidance.contains("hero", ignoreCase = true))
    }

    @Test
    fun testSimpleMode_resolvesMinimalistTone() {
        val tone = toneHook.resolveTone(isSimpleModeEnabled = true)
        assertEquals(HabitInsightTone.SIMPLE_MINIMALIST, tone)

        val promptGuidance = toneHook.getSystemPromptGuidance(tone)
        assertTrue(promptGuidance.contains("minimalist", ignoreCase = true))
        assertTrue(promptGuidance.contains("Do not reference points", ignoreCase = true))
    }

    @Test
    fun testCustomHookImplementation_conformsToInterfaceContract() {
        val customHook = object : HabitInsightToneHook {
            override fun resolveTone(isSimpleModeEnabled: Boolean): HabitInsightTone {
                return if (isSimpleModeEnabled) HabitInsightTone.SIMPLE_MINIMALIST else HabitInsightTone.GAMIFIED_HEROIC
            }

            override fun getSystemPromptGuidance(tone: HabitInsightTone): String {
                return "Custom guidance for $tone"
            }
        }

        assertEquals(HabitInsightTone.SIMPLE_MINIMALIST, customHook.resolveTone(true))
        assertEquals(HabitInsightTone.GAMIFIED_HEROIC, customHook.resolveTone(false))
        assertEquals("Custom guidance for SIMPLE_MINIMALIST", customHook.getSystemPromptGuidance(HabitInsightTone.SIMPLE_MINIMALIST))
    }
}
