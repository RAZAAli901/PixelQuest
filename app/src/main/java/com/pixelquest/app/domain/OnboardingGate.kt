package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.UserProfileEntity

/**
 * Whether onboarding has been done: its flag, or a profile that shows the player has played.
 *
 * v1.1.0's splash read the flag before it had loaded, so onboarding never showed and the flag was
 * never set. After an update the splash reads it properly, and every 1.1.0 player was sent through
 * onboarding again, which ended by writing a new level-1, 0-XP profile over theirs. Found on the
 * emulator by updating a v1.1.0 build in place (Day 34).
 */
object OnboardingGate {

    fun hasPlayed(profile: UserProfileEntity?): Boolean = profile != null && (profile.totalXp > 0 || profile.level > 1)

    fun isDone(flag: Boolean, profile: UserProfileEntity?): Boolean = flag || hasPlayed(profile)
}
