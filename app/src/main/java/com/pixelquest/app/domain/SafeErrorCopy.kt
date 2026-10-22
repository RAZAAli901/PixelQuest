package com.pixelquest.app.domain

/**
 * What the player is told when something fails (S056). A server's or a library's own error text
 * never reaches the screen: it can quote a table, a policy, a request or a token, and it doesn't
 * tell the player what to do. Each failure maps to one plain sentence. The AI Coach has its own
 * (AiErrorCopy); parties, chess and study map the server's short refusal tokens to their strings.
 */
object SafeErrorCopy {
    const val GOOGLE_EXCHANGE_FAILED = "Google authentication succeeded, but cloud token exchange failed. Try again in a few minutes."
    const val GOOGLE_FAILED = "Google Sign-In didn't work this time. Try again."
    const val SYNC_SERVER = "The cloud couldn't save your progress just now. Try again later."
    const val SYNC_FAILED = "Sync failed. Your progress is safe on this phone; try again later."
    const val TASKS_FAILED = "Failed to load tasks"
    const val BOARD_OFFLINE = "Network error: check your connection and try again."
    const val BOARD_SIGN_IN = "Your sign-in has ended. Sign in again in Account to see the leaderboard."
    const val BOARD_FAILED = "Failed to load the leaderboard. Try again later."
}
