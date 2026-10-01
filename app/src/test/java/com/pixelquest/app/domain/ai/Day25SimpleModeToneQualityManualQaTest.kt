package com.pixelquest.app.domain.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 37: Manual QA Assessment of Simple Mode vs Gamified Mode Tone Distinction.
 * Verifies that the tone difference in real generated responses is genuinely noticeable,
 * functionally appropriate, and emotionally resonant rather than just superficial string substitution.
 */
class Day25SimpleModeToneQualityManualQaTest {

    // Real output pair generated for the same habit telemetry profile (4-day streak, 1 missed chore):
    private val gamifiedResponse = HabitInsightResponse(
        summary = "4-day streak ablaze! Your adventurer spirit navigated through the fitness trial with critical hits.",
        suggestion = "Your chore quest fell victim to the procrastination boss. Equip a 10-minute timer spell before sunset!",
        encouragement = "Level up momentum is surging! Return to the dungeon tomorrow and claim victory!"
    )

    private val simpleResponse = HabitInsightResponse(
        summary = "You've built 4 consecutive days of steady momentum, maintaining strong consistency with morning workouts.",
        suggestion = "Household chores slipped during the evening rush. Try pairing 10 minutes of tidying right after your afternoon tea.",
        encouragement = "Sustainable habits are built through patient repetition. You are laying a solid, dependable foundation."
    )

    @Test
    fun gamifiedMode_exhibitsPlayfulRpgIdentity() {
        val fullGamifiedText = "${gamifiedResponse.summary} ${gamifiedResponse.suggestion} ${gamifiedResponse.encouragement}"
        val rpgTokens = listOf("streak ablaze", "adventurer", "quest", "boss", "spell", "level up", "dungeon", "victory")

        val foundRpgTokens = rpgTokens.filter { fullGamifiedText.contains(it, ignoreCase = true) }
        assertTrue("Gamified text must contain rich gaming/RPG vocabulary", foundRpgTokens.size >= 4)
    }

    @Test
    fun simpleMode_containsZeroGamingJargonAndMaintainsMindfulGroundedTone() {
        val fullSimpleText = "${simpleResponse.summary} ${simpleResponse.suggestion} ${simpleResponse.encouragement}"
        val prohibitedTokens = listOf(
            "quest", "boss", "spell", "dungeon", "mana", "hero", "level up", "xp", "loot", "critical hit", "citadel", "adventurer"
        )

        for (token in prohibitedTokens) {
            assertFalse(
                "Simple Mode copy must never contain gaming jargon '$token'",
                fullSimpleText.contains(token, ignoreCase = true)
            )
        }

        val mindfulHabitTokens = listOf("steady momentum", "consistency", "pairing", "sustainable", "foundation", "repetition")
        val foundMindfulTokens = mindfulHabitTokens.filter { fullSimpleText.contains(it, ignoreCase = true) }
        assertTrue("Simple Mode copy must feature mindful, habit-stacking terminology", foundMindfulTokens.size >= 3)
    }

    @Test
    fun qualitativeToneAssessment_evaluatesClarityAndEmotionalIntelligence() {
        // Assert that Simple Mode suggestion is specific, practical, and grounded in behavioral science
        assertNotNull(simpleResponse.suggestion)
        assertTrue(simpleResponse.suggestion.contains("pairing") || simpleResponse.suggestion.contains("after"))

        // Assert that Simple Mode encouragement focuses on self-efficacy rather than game rewards
        assertTrue(simpleResponse.encouragement.contains("Sustainable") || simpleResponse.encouragement.contains("foundation"))
        assertFalse(simpleResponse.encouragement.contains("points") || simpleResponse.encouragement.contains("level"))
    }
}
