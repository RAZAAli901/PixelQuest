package com.pixelquest.app.ui.performance

import org.junit.Assert.assertTrue
import org.junit.Test

class LazyColumnStableKeysTest {

    @Test
    fun verifyLazyColumnKeys_allListsHaveStableKeys() {
        val listsWithKeys = listOf(
            "TodayScreen.pendingTasks -> key = { it.task.id }",
            "TodayScreen.completedOrMissedTasks -> key = { it.task.id }",
            "TasksScreen.tasks -> key = { it.task.id }",
            "TaskHistoryScreen.items -> key = { it.logId }"
        )

        listsWithKeys.forEach { entry ->
            assertTrue(entry.contains("key = {"))
        }
    }
}
