package com.pixelquest.app.play.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptPolicyTest {
    private val now = ReviewPromptPolicy.MIN_ACCOUNT_AGE_MILLIS + 1_000L

    private fun eligibleSnapshot() = ReviewPromptPolicy.Snapshot(
        onboardingComplete = true,
        profileCreatedAtMillis = 0L,
        completedQuestCount = ReviewPromptPolicy.MIN_COMPLETIONS,
        nowMillis = now,
        currentVersionCode = 115,
        lastAttemptAtMillis = 0L,
        lastAttemptVersionCode = 0,
    )

    @Test
    fun eligible_afterSevenDaysAndTenCompletions() {
        assertTrue(ReviewPromptPolicy.isEligible(eligibleSnapshot()))
    }

    @Test
    fun notEligible_beforeOnboardingOrValueMoment() {
        assertFalse(ReviewPromptPolicy.isEligible(eligibleSnapshot().copy(onboardingComplete = false)))
        assertFalse(ReviewPromptPolicy.isEligible(eligibleSnapshot().copy(completedQuestCount = 9)))
        assertFalse(ReviewPromptPolicy.isEligible(eligibleSnapshot().copy(profileCreatedAtMillis = now)))
    }

    @Test
    fun notEligible_sameVersionOrCooldown() {
        assertFalse(ReviewPromptPolicy.isEligible(eligibleSnapshot().copy(lastAttemptVersionCode = 115)))
        assertFalse(
            ReviewPromptPolicy.isEligible(
                eligibleSnapshot().copy(lastAttemptAtMillis = now - ReviewPromptPolicy.COOLDOWN_MILLIS + 1L),
            ),
        )
    }
}
