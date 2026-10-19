package com.pixelquest.app.darood.domain

import com.pixelquest.app.data.remote.SupabaseResult

/**
 * Why the server said no to a Darood Party request (supabase/schema.md, Darood Parties). Each
 * refusal carries its name in the error's message; the screens say what each means.
 */
enum class DaroodRefusal(val token: String) {
    PARTY_FULL("party_full"),
    NOT_LEADER("not_leader"),
    PARTY_CLOSED("party_closed"),
    NO_INVITATION("no_invitation"),
    NOT_A_MEMBER("not_a_member"),
    LEADER_MUST_HAND_OVER("leader_must_hand_over"),
    MILESTONE_TOO_LOW("milestone_too_low"),
    USERNAME_REQUIRED("username_required"),
    DAILY_LIMIT("daily_limit"),
    BAD_DAY("bad_day"),
    BAD_TIME_ZONE("bad_time_zone"),

    /** Another of the server's rules (a name's length, an amount's range): the app checks these first. */
    INVALID("");

    companion object {
        /** The refusal a result carries, or null when it isn't one (success, no connection, signed out). */
        fun of(result: SupabaseResult<*>): DaroodRefusal? {
            val error = result as? SupabaseResult.ServerError ?: return null
            if (error.code == null || error.code !in 400..499) return null
            return entries.firstOrNull { it.token.isNotEmpty() && error.message.trim() == it.token } ?: INVALID
        }
    }
}
