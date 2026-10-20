package com.pixelquest.app.study.domain

import com.pixelquest.app.data.remote.SupabaseResult

/**
 * Why the server said no to a study party request (docs/study-party.md). Each refusal carries its
 * name in the error's message, as Darood's do; the screens say what each means.
 */
enum class StudyRefusal(val token: String) {
    NOT_LEADER("not_leader"),
    NOT_A_MEMBER("not_a_member"),
    NO_SUCH_ITEM("no_such_item"),
    BAD_DAY("bad_day"),
    BAD_TIME_ZONE("bad_time_zone"),
    BAD_WEEK("bad_week"),
    WEEK_NOT_OVER("week_not_over"),

    /** Row-level security let nothing through: not the item's creator or leader, or past the 24 hours to undo. */
    NOT_ALLOWED("not_allowed"),

    /** Another of the server's rules (a title's length, a quiz without a due date): the app checks these first. */
    INVALID("");

    companion object {
        /** The refusal a result carries, or null when it isn't one (success, no connection, signed out). */
        fun of(result: SupabaseResult<*>): StudyRefusal? {
            val error = result as? SupabaseResult.ServerError ?: return null
            // 401 is the sign-in lapsing, not a rule: what's waiting waits for it to be refreshed.
            if (error.code == null || error.code !in 400..499 || error.code == 401) return null
            return entries.firstOrNull { it.token.isNotEmpty() && error.message.trim() == it.token } ?: INVALID
        }

        /** A refusal as a result, in the shape the server's own would have. */
        fun result(refusal: StudyRefusal): SupabaseResult.ServerError = SupabaseResult.ServerError(403, refusal.token)
    }
}
