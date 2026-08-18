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
