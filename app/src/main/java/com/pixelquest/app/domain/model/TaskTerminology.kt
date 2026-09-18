package com.pixelquest.app.domain.model

/**
 * Step 3: Centralized terminology copy provider switching between Gamified ("Quest")
 * and Simple Mode ("Task") terminology across all task cards, headers, empty states, and dialogs.
 */
data class TerminologyStrings(
    val itemSingular: String,
    val itemPlural: String,
    val todayHeader: String,
    val upNextHeader: String,
    val completedHeader: String,
    val emptyStateTitle: String,
    val emptyStateSubtitle: String,
    val createButtonText: String,
    val skipDialogTitle: String,
    val skipDialogMessage: (String) -> String,
    val historyButtonText: String
)

object TaskTerminology {

    val GAMIFIED = TerminologyStrings(
        itemSingular = "Quest",
        itemPlural = "Quests",
        todayHeader = "⚔️ TODAY'S DASHBOARD",
        upNextHeader = "⚔️ UP NEXT",
        completedHeader = "📜 COMPLETED & PAST QUESTS",
        emptyStateTitle = "NO QUESTS SCHEDULED FOR TODAY",
        emptyStateSubtitle = "The realm is quiet. Add a new quest to begin your adventure!",
        createButtonText = "+ CREATE QUEST",
        skipDialogTitle = "SKIP QUEST",
        skipDialogMessage = { taskName -> "Are you sure you want to mark '$taskName' as missed/skipped?" },
        historyButtonText = "📜 QUEST HISTORY"
    )

    val SIMPLE = TerminologyStrings(
        itemSingular = "Task",
        itemPlural = "Tasks",
        todayHeader = "📋 TODAY'S TASKS",
        upNextHeader = "UP NEXT",
        completedHeader = "COMPLETED & PAST TASKS",
        emptyStateTitle = "NO TASKS SCHEDULED FOR TODAY",
        emptyStateSubtitle = "No tasks scheduled. Add a new task to get started.",
        createButtonText = "+ CREATE TASK",
        skipDialogTitle = "SKIP TASK",
        skipDialogMessage = { taskName -> "Are you sure you want to mark '$taskName' as skipped?" },
        historyButtonText = "📜 TASK HISTORY"
    )

    fun forMode(isSimpleMode: Boolean): TerminologyStrings {
        return if (isSimpleMode) SIMPLE else GAMIFIED
    }
}
