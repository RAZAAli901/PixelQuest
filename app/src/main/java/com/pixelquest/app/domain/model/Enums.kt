package com.pixelquest.app.domain.model

import androidx.annotation.DrawableRes
import com.pixelquest.app.R

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
    FITNESS("Fitness", R.drawable.ic_tasks),
    HEALTH("Health", R.drawable.ic_profile),
    LEARNING("Learning", R.drawable.ic_stats),
    CHORES("Chores", R.drawable.ic_home),
    OTHER("Other", R.drawable.ic_tasks)
}

enum class DifficultyLevel {
    EASY,
    MEDIUM,
    HARD,
    HARDEST
}
