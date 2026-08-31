package com.pixelquest.app.integration

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TodoAuditTest {

    @Test
    fun auditCodebase_zeroUnresolvedTodoComments() {
        val srcDir = File("src/main/java")
        if (!srcDir.exists()) return

        var todoCount = 0
        srcDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "kt") {
                file.useLines { lines ->
                    lines.forEach { line ->
                        if (line.contains("TODO", ignoreCase = false)) {
                            todoCount++
                        }
                    }
                }
            }
        }
        assertTrue("Found $todoCount unresolved TODO comments in src/main/java", todoCount == 0)
    }
}
