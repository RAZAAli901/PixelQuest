package com.pixelquest.app.domain.ai

/**
 * Represents the tone style for Gemini AI Habit Insights (Days 24-25).
 */
enum class HabitInsightTone {
    /**
     * Gamified retro RPG adventure tone with quest metaphors, XP references, and celebratory fanfare.
     */
    GAMIFIED_HEROIC,

    /**
     * Clean, neutral, focused habit coaching tone without gaming jargon, streaks, or points.
     */
    SIMPLE_MINIMALIST
}

/**
 * Step 30: Placeholder interface hook for tone-adjusted insight prompt construction
 * and copy generation based on Simple Mode state. Forward-compatible with Gemini AI integration in Days 24-25.
 */
interface HabitInsightToneHook {
    /**
     * Returns the appropriate [HabitInsightTone] based on whether Simple Mode is enabled.
     */
    fun resolveTone(isSimpleModeEnabled: Boolean): HabitInsightTone

    /**
     * Returns system instructions/context for prompt generation based on tone.
     */
    fun getSystemPromptGuidance(tone: HabitInsightTone): String
}

/**
 * Default placeholder implementation of [HabitInsightToneHook].
 */
class DefaultHabitInsightToneHook : HabitInsightToneHook {
    override fun resolveTone(isSimpleModeEnabled: Boolean): HabitInsightTone {
        return if (isSimpleModeEnabled) {
            HabitInsightTone.SIMPLE_MINIMALIST
        } else {
            HabitInsightTone.GAMIFIED_HEROIC
        }
    }

    override fun getSystemPromptGuidance(tone: HabitInsightTone): String {
        return when (tone) {
            HabitInsightTone.GAMIFIED_HEROIC ->
                "Adopt an epic 8-bit RPG questmaster persona. Frame habit consistency as hero resilience, streaks as battle momentum, and tasks as heroic quests."
            HabitInsightTone.SIMPLE_MINIMALIST ->
                "Adopt a calm, minimalist habit coach persona. Provide clear, objective observations and actionable suggestions. Do not reference points, XP, streaks, levels, or game terminology."
        }
    }
}
