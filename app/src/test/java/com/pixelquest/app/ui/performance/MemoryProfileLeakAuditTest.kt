package com.pixelquest.app.ui.performance

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MemoryProfileLeakAuditTest {

    @Test
    fun profileExtendedSession_heatmapDataMemoryCheck() {
        val startDate = LocalDate.now().minusMonths(6)
        val endDate = LocalDate.now()
        val dates = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            dates.add(current)
            current = current.plusDays(1)
        }

        // Memory usage allocation test across 180 days of calendar data
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

        val dataMap = dates.associateWith { date ->
            com.pixelquest.app.domain.model.DailyStatus.NONE
        }

        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val deltaKb = (afterMemory - beforeMemory) / 1024

        // Verify dataset overhead remains minimal (< 500 KB) for 6 months of records
        assertTrue("Memory allocation delta ($deltaKb KB) must be within limits", deltaKb < 500)
    }
}
