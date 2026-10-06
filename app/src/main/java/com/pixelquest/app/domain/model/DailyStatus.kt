package com.pixelquest.app.domain.model

enum class DailyStatus {
    PERFECT,
    PARTIAL,
    MISSED,
    NO_TASKS_SCHEDULED,
    /** Today, with quests due but none done yet: the day isn't over, so it isn't "missed". */
    IN_PROGRESS
}
