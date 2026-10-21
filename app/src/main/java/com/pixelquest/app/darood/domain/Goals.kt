package com.pixelquest.app.darood.domain

import java.time.Instant

/**
 * One of a Darood Party's goals (docs/darood-weekly.md, Goals that go on): goal [sequence], its
 * [target], who set it and when, and when the party's total first reached it (null: in progress).
 */
data class PartyGoal(val sequence: Int, val target: Long, val setBy: String?, val setAt: Instant, val reachedAt: Instant?) {
    val reached: Boolean get() = reachedAt != null
}

/** The goals' rules, as the server keeps them (20261019000072). */
object GoalRules {
    /** The highest goal the server takes. */
    const val MAX = 1_000_000_000_000L

    /** The goal now: the latest one. */
    fun current(goals: List<PartyGoal>): PartyGoal? = goals.maxByOrNull { it.sequence }
}
