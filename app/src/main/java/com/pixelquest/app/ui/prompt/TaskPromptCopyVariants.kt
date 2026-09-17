package com.pixelquest.app.ui.prompt

/**
 * Step 14: Authoritative copy specification for the full-screen "did you do it?" prompt
 * across Gamified Mode (Default) and Simple Mode.
 *
 * Defines the copy variants for Day 19 UI adaptation:
 * - Gamified: Arcade RPG iconography ("⚔️"), urgent capitalization ("DID YOU DO IT?"), enthusiastic actions ("YES!", "NOT YET"), points badges.
 * - Simple: Clean, calm typography, neutral question ("Did you complete this task today?"), understated actions ("Completed", "Not yet"), no points badge.
 */
data class TaskPromptCopy(
    val iconEmoji: String?,
    val headerTitle: String,
    val questionPrompt: String,
    val confirmButtonText: String,
    val dismissButtonText: String,
    val showPointsBadge: Boolean
)

object TaskPromptCopyVariants {

    val GAMIFIED = TaskPromptCopy(
        iconEmoji = "⚔️",
        headerTitle = "DID YOU DO IT?",
        questionPrompt = "Quest Time",
        confirmButtonText = "YES!",
        dismissButtonText = "NOT YET",
        showPointsBadge = true
    )

    val SIMPLE = TaskPromptCopy(
        iconEmoji = null,
        headerTitle = "TASK REMINDER",
        questionPrompt = "Did you complete this task today?",
        confirmButtonText = "Completed",
        dismissButtonText = "Not yet",
        showPointsBadge = false
    )

    fun resolve(isSimpleMode: Boolean): TaskPromptCopy {
        return if (isSimpleMode) SIMPLE else GAMIFIED
    }
}
