package com.pixelquest.app.domain.model

/**
 * Domain specification and feature definitions for elements suppressed when Simple Mode is active.
 * Defines the authoritative suppression list: streak display, points/XP display, level badge/celebrations,
 * difficulty selection, and the level-up sound/CRT-adjacent flourishes specifically tied to gamification moments.
 */
enum class SimpleModeSuppressedFeature(val featureName: String, val description: String) {
    STREAK_DISPLAY(
        featureName = "Streak Display",
        description = "Streaks counter, streak flame indicators, and streak-break alerts"
    ),
    POINTS_XP_DISPLAY(
        featureName = "Points & XP Display",
        description = "XP gains, points badges, XP counters, and level progression bars"
    ),
    LEVEL_BADGE_AND_CELEBRATION(
        featureName = "Level Badge & Celebrations",
        description = "Player level badge, rank banner, and full-screen level-up celebration overlays"
    ),
    DIFFICULTY_SELECTION(
        featureName = "Difficulty Selection",
        description = "Custom difficulty threshold choices and days-to-level configuration"
    ),
    GAMIFICATION_AUDIO_VISUAL_FLOURISHES(
        featureName = "Audio & Visual Gamification Flourishes",
        description = "Level-up fanfares, points-awarded audio, and retro CRT scanline filter overlay"
    )
}

object SimpleModeSuppression {
    /**
     * Complete set of all features suppressed under Simple Mode.
     */
    val allSuppressedFeatures: Set<SimpleModeSuppressedFeature> = SimpleModeSuppressedFeature.values().toSet()

    /**
     * Evaluates whether a given feature should be suppressed given the current simple mode state.
     */
    fun isSuppressed(feature: SimpleModeSuppressedFeature, isSimpleModeEnabled: Boolean): Boolean {
        return isSimpleModeEnabled && allSuppressedFeatures.contains(feature)
    }
}
