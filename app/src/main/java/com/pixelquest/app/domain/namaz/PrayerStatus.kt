package com.pixelquest.app.domain.namaz

/** Where a prayer stands on its day (docs/namaz-design.md). */
enum class PrayerStatus {
    /** Not marked yet, and its window hasn't passed. */
    PENDING,

    /** Marked prayed by the player. */
    PRAYED,

    /** Not marked by the time the next prayer started. It can still be marked late (Phase 6). */
    MISSED
}
