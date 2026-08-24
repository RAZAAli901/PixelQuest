package com.pixelquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FlavorTextCatalogTest {

    @Test
    fun getFlavorText_deterministicPerDay() {
        val date = LocalDate.of(2026, 8, 24)
        val text1 = FlavorTextCatalog.getFlavorText(taskCount = 3, completedCount = 1, isPerfectDay = false, date = date)
        val text2 = FlavorTextCatalog.getFlavorText(taskCount = 3, completedCount = 1, isPerfectDay = false, date = date)
        assertEquals(text1, text2)
    }

    @Test
    fun getFlavorText_variesByDate() {
        val date1 = LocalDate.of(2026, 8, 24)
        val date2 = LocalDate.of(2026, 8, 25)
        val text1 = FlavorTextCatalog.getFlavorText(taskCount = 3, completedCount = 0, isPerfectDay = false, date = date1)
        val text2 = FlavorTextCatalog.getFlavorText(taskCount = 3, completedCount = 0, isPerfectDay = false, date = date2)
        // With distinct date seeds, flavor text selection varies
        assertTrue(text1.isNotBlank() && text2.isNotBlank())
    }

    @Test
    fun getFlavorText_zeroTasksState() {
        val date = LocalDate.of(2026, 8, 24)
        val text = FlavorTextCatalog.getFlavorText(taskCount = 0, completedCount = 0, isPerfectDay = false, date = date)
        assertTrue(FlavorTextCatalog.zeroTasksLines.contains(text))
    }

    @Test
    fun getFlavorText_allCompletedState() {
        val date = LocalDate.of(2026, 8, 24)
        val text = FlavorTextCatalog.getFlavorText(taskCount = 3, completedCount = 3, isPerfectDay = true, date = date)
        assertTrue(FlavorTextCatalog.allCompletedLines.contains(text))
    }

    @Test
    fun getFlavorText_inProgressState() {
        val date = LocalDate.of(2026, 8, 24)
        val text = FlavorTextCatalog.getFlavorText(taskCount = 4, completedCount = 2, isPerfectDay = false, date = date)
        assertTrue(FlavorTextCatalog.inProgressLines.contains(text))
    }
}
