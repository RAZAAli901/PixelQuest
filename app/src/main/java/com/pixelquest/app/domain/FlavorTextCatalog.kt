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

    val simpleZeroTasksLines = listOf(
        "No tasks scheduled for today.",
        "Your checklist is empty for today.",
        "All clear for today. Add tasks whenever you're ready."
    )

    val simpleNotStartedLines = listOf(
        "Ready to begin today's tasks.",
        "Here is your task checklist for today.",
        "First step of the day: review your scheduled tasks."
    )

    val simpleInProgressLines = listOf(
        "Tasks in progress. Keep moving forward.",
        "Great progress on today's checklist.",
        "Continuing through your scheduled tasks."
    )

    val simpleAllCompletedLines = listOf(
        "All tasks completed for today.",
        "Checklist complete. Well done on finishing today's tasks.",
        "Every scheduled task for today has been completed."
    )

    fun getFlavorText(
        taskCount: Int,
        completedCount: Int,
        isPerfectDay: Boolean,
        date: LocalDate = LocalDate.now(),
        isSimpleMode: Boolean = false
    ): String {
        val seed = abs(date.hashCode())
        val allCompleted = taskCount > 0 && completedCount == taskCount
        if (isSimpleMode) {
            return when {
                taskCount == 0 -> simpleZeroTasksLines[seed % simpleZeroTasksLines.size]
                allCompleted || isPerfectDay -> simpleAllCompletedLines[seed % simpleAllCompletedLines.size]
                completedCount > 0 -> simpleInProgressLines[seed % simpleInProgressLines.size]
                else -> simpleNotStartedLines[seed % simpleNotStartedLines.size]
            }
        }
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
