package com.pixelquest.app.play.update

/** Chooses update UX from rollout metadata. Priority 4–5 is reserved for urgent fixes. */
object InAppUpdatePolicy {
    const val FLEXIBLE_PRIORITY = 3
    const val IMMEDIATE_PRIORITY = 4
    const val FLEXIBLE_STALENESS_DAYS = 7

    enum class Type { NONE, FLEXIBLE, IMMEDIATE }

    fun chooseType(priority: Int, stalenessDays: Int?, flexibleAllowed: Boolean, immediateAllowed: Boolean): Type = when {
        priority >= IMMEDIATE_PRIORITY && immediateAllowed -> Type.IMMEDIATE
        (priority >= FLEXIBLE_PRIORITY || (stalenessDays ?: -1) >= FLEXIBLE_STALENESS_DAYS) && flexibleAllowed -> Type.FLEXIBLE
        else -> Type.NONE
    }
}
