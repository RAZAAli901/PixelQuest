package com.pixelquest.app.domain.model

import androidx.annotation.DrawableRes
import com.pixelquest.app.R

/**
 * How a task's reminder behaves.
 * STANDARD: normal reminder with sound (if the user's reminder sound is on).
 * SILENT: shows in the shade without sound or vibration.
 * PROMPT: opens the full-screen "did you do it?" prompt as well as the notification.
 */
enum class ReminderStyle {
    STANDARD,
    SILENT,
    PROMPT
}

enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    ONE_TIME
}

enum class TaskCategory(
    val displayName: String,
    @DrawableRes val iconResId: Int
) {
    FITNESS("Fitness", R.drawable.ic_cat_fitness),
    HEALTH("Health", R.drawable.ic_cat_health),
    LEARNING("Learning", R.drawable.ic_cat_learning),
    CHORES("Chores", R.drawable.ic_cat_chores),
    OTHER("Other", R.drawable.ic_cat_other)
}

enum class DifficultyLevel {
    EASY,
    MEDIUM,
    HARD,
    HARDEST
}
