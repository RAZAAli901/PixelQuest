package com.pixelquest.app.domain

import java.time.LocalDate
import kotlin.math.abs

object FlavorTextCatalog {

    val zeroTasksLines = listOf(
        "No quests scheduled today. Enjoy your rest, hero!",
        "A quiet day in the realm. Time to sharpen your blade!",
        "The quest board is clear today. Take a well-earned break!"
    )

    val notStartedLines = listOf(
        "Your quest board awaits, brave hero!",
        "A new day dawns in PixelQuest. Ready your gear!",
        "First step on the path to glory. Begin your quest!"
    )

    val inProgressLines = listOf(
        "Halfway there, hero! Keep up the momentum!",
        "Victory favors the persistent. Onward!",
        "Every completed quest fuels your inner fire!"
    )

    val perfectDayLines = listOf(
        "Quest board is clear! Perfect Day achieved!",
        "Legendary work! The realm celebrates your victory!",
        "Maximum glory unlocked today, hero!"
    )

    val allCompletedLines = listOf(
        "All quests completed! Perfect score today, hero!",
        "Quest log cleared! Rest easy until tomorrow's adventure!",
        "Flawless victory! Every scheduled quest accomplished!"
    )

    fun getFlavorText(
        taskCount: Int,
        completedCount: Int,
        isPerfectDay: Boolean,
        date: LocalDate = LocalDate.now()
    ): String {
        val seed = abs(date.hashCode())
        val allCompleted = taskCount > 0 && completedCount == taskCount
        return when {
            taskCount == 0 -> zeroTasksLines[seed % zeroTasksLines.size]
            allCompleted || isPerfectDay -> {
                if (allCompleted) allCompletedLines[seed % allCompletedLines.size]
                else perfectDayLines[seed % perfectDayLines.size]
            }
            completedCount > 0 -> inProgressLines[seed % inProgressLines.size]
            else -> notStartedLines[seed % notStartedLines.size]
        }
    }
}
