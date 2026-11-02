package com.pixelquest.app.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestDataBuildersTest {

    @Test
    fun testDataGenerator_producesDeterministicIdenticalSequences_forSameSeed() {
        val gen1 = TestDataGenerator(12345L)
        val gen2 = TestDataGenerator(12345L)

        val task1 = gen1.buildTask()
        val task2 = gen2.buildTask()

        assertEquals(task1.id, task2.id)
        assertEquals(task1.name, task2.name)
        assertEquals(task1.scheduledDay, task2.scheduledDay)
    }

    @Test
    fun testDataBuilders_buildsValidQuestAndTask() {
        val task = TestDataBuilders.buildTask(id = 99L, name = "Custom Task")
        assertEquals(99L, task.id)
        assertEquals("Custom Task", task.name)

        val quest = TestDataBuilders.buildQuest(id = 101L, title = "Epic Quest")
        assertEquals(101L, quest.id)
        assertEquals("Epic Quest", quest.title)
    }

    @Test
    fun testDataBuilders_buildsValidStreakAndUserProfile() {
        val streak = TestDataBuilders.buildStreak(currentStreak = 14)
        assertEquals(14, streak.currentStreak)

        val user = TestDataBuilders.buildUserProfile(username = "CustomHero")
        assertEquals("CustomHero", user.username)
        assertTrue(user.level >= 1)
    }

    @Test
    fun testDataBuilders_buildsValidPartyAndStudyItem() {
        val party = TestDataBuilders.buildParty(name = "Math Circle")
        assertEquals("Math Circle", party.name)
        assertNotNull(party.createdAt)

        val studyItem = TestDataBuilders.buildStudyItem(title = "Calculus Exam")
        assertEquals("Calculus Exam", studyItem.title)
        assertNotNull(studyItem.dueAt)
    }

    @Test
    fun testDataBuilders_buildsValidDaroodEntryAndChessGame() {
        val darood = TestDataBuilders.buildDaroodEntry(amount = 100)
        assertEquals(100, darood.amount)

        val chess = TestDataBuilders.buildChessGame(id = "chess-1")
        assertEquals("chess-1", chess.id)
        assertNotNull(chess.fen)
    }
}
