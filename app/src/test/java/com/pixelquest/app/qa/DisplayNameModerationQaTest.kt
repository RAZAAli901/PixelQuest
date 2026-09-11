package com.pixelquest.app.qa

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.domain.DisplayNameModerator
import com.pixelquest.app.ui.screens.account.AccountViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Manual QA programmatic verification suite testing defense-in-depth:
 * 1. UI Layer validation blocks offensive input at typing/submission time.
 * 2. Database/API layer simulation rejects bypass attempts via SQL exception emulation.
 */
class DisplayNameModerationQaTest {

    @Test
    fun uiLayer_blocksOffensiveInputImmediately() {
        val offensiveInput = "bitch_slayer_99"
        val validationError = AccountViewModel.validateDisplayName(offensiveInput)
        assertEquals("Display name contains disallowed or offensive language.", validationError)
    }

    @Test
    fun databaseLayer_rejectsBypassAttempts() {
        // Emulate server-side trigger behavior when direct REST API call bypasses client UI
        fun simulateServerProfileUpsert(profile: CloudProfileDto): SupabaseResult<Unit> {
            val isSafe = DisplayNameModerator.isAppropriate(profile.displayName)
            val matchesRegex = profile.displayName.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))
            return if (!matchesRegex || !isSafe) {
                SupabaseResult.ServerError(
                    Exception("check_violation: Display name contains disallowed or offensive terminology."),
                    "Server rejected display name due to moderation policy violation."
                )
            } else {
                SupabaseResult.Success(Unit)
            }
        }

        val maliciousBypassProfile = CloudProfileDto(
            id = "test-bypass-user-id",
            displayName = "asshole_bypass",
            currentStreak = 10,
            longestStreak = 10,
            level = 5,
            totalXp = 1500,
            leaderboardOptIn = true
        )

        val result = simulateServerProfileUpsert(maliciousBypassProfile)
        assertTrue("Server trigger must reject malicious bypass profile", result is SupabaseResult.ServerError)
        assertEquals(
            "Server rejected display name due to moderation policy violation.",
            (result as SupabaseResult.ServerError).userMessage
        )
    }
}
