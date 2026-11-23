package com.pixelquest.app.play.review

/** Pure eligibility rules. Google Play may still decline to display its own review dialog. */
object ReviewPromptPolicy {
    const val MIN_ACCOUNT_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000
    const val MIN_COMPLETIONS = 10
    const val COOLDOWN_MILLIS = 180L * 24 * 60 * 60 * 1000

    fun isEligible(snapshot: Snapshot): Boolean =
        snapshot.onboardingComplete &&
            snapshot.completedQuestCount >= MIN_COMPLETIONS &&
            snapshot.nowMillis - snapshot.profileCreatedAtMillis >= MIN_ACCOUNT_AGE_MILLIS &&
            snapshot.lastAttemptVersionCode != snapshot.currentVersionCode &&
            (snapshot.lastAttemptAtMillis == 0L || snapshot.nowMillis - snapshot.lastAttemptAtMillis >= COOLDOWN_MILLIS)

    data class Snapshot(
        val onboardingComplete: Boolean,
        val profileCreatedAtMillis: Long,
        val completedQuestCount: Int,
        val nowMillis: Long,
        val currentVersionCode: Int,
        val lastAttemptAtMillis: Long,
        val lastAttemptVersionCode: Int,
    )
}
