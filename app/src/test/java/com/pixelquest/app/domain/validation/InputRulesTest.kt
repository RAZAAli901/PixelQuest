package com.pixelquest.app.domain.validation

import com.pixelquest.app.domain.validation.InputRules.Field
import com.pixelquest.app.domain.validation.InputRules.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** S053: one validation layer for every form's text. */
class InputRulesTest {

    @Test
    fun typing_dropsControlCharacters_andDisguises() {
        assertEquals("Run 5k", InputRules.typed("Run\u0000 5k\u0007", Field.QUEST_NAME))
        assertEquals("Read Surah", InputRules.typed("Read\nSurah", Field.QUEST_NAME))
        assertEquals("line one\nline two", InputRules.typed("line one\nline two", Field.CHAT_MESSAGE))
        // A right-to-left override can make "gpj.exe" read as "exe.jpg".
        assertEquals("adminexe.jpg", InputRules.typed("admin‮exe.jpg", Field.DISPLAY_NAME))
        assertEquals("ab", InputRules.typed("a​b﻿", Field.HERO_NAME))
    }

    @Test
    fun theJoinersThatWordsAndEmojiNeed_stay() {
        val family = "👨‍👩‍👧"
        assertEquals(family, InputRules.typed(family, Field.CHAT_MESSAGE))
        val urdu = "می‌ں"
        assertEquals(urdu, InputRules.typed(urdu, Field.STUDY_NOTES))
    }

    @Test
    fun limits_stopTyping_orLeaveRoomToSayTooLong() {
        assertEquals(100, InputRules.typed("x".repeat(500), Field.QUEST_NAME).length)
        assertEquals(Field.PARTY_NAME.max + 10, InputRules.typed("x".repeat(500), Field.PARTY_NAME).length)
        assertEquals(Problem.TOO_LONG, InputRules.problem("x".repeat(Field.PARTY_NAME.max + 1), Field.PARTY_NAME))
        assertEquals(Problem.BLANK, InputRules.problem(" \n\t ", Field.STUDY_TITLE))
        assertNull(InputRules.problem("", Field.STUDY_NOTES, required = false))
        assertNull(InputRules.problem("Revise chapter 3", Field.STUDY_TITLE))
        assertEquals("Revise", InputRules.saved("  Revise \u0000", Field.STUDY_TITLE))
        assertEquals("1234", InputRules.digits("1a2-3 4", 6))
        assertEquals("12", InputRules.digits("12345", 2))
    }

    @Test
    fun theOldLimits_areKept() {
        assertEquals(20, Field.HERO_NAME.max)
        assertEquals(20, Field.USERNAME.max)
        assertEquals(40, Field.PARTY_NAME.max)
        assertEquals(80, Field.STUDY_TITLE.max)
        assertEquals(40, Field.STUDY_SUBJECT.max)
        assertEquals(500, Field.STUDY_NOTES.max)
        assertEquals(80, Field.REMINDER_TITLE.max)
        assertEquals(2000, Field.CHAT_MESSAGE.max)
        assertEquals(80, Field.SEARCH.max)
        assertTrue(Field.entries.all { it.max > 0 })
    }

    @Test
    fun everyForm_goesThroughIt() {
        // The forms' text handlers: each one reads its field through InputRules.
        val main = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }.resolve("com/pixelquest/app")
        val forms = listOf(
            "ui/screens/tasks/TaskFormViewModel.kt", "domain/HeroName.kt", "study/ui/ItemForm.kt", "study/ui/ReminderForm.kt",
            "darood/ui/create/CreatePartyViewModel.kt", "darood/ui/info/PartyInfoScreen.kt", "study/chat/ui/StudyChatViewModel.kt",
            "study/chat/ui/ChatInputBar.kt", "ui/screens/account/AccountViewModel.kt", "darood/ui/username/UsernameSetupViewModel.kt",
            "darood/ui/invite/AddPeopleViewModel.kt", "auth/AuthViewModel.kt", "quran/ui/search/QuranSearchViewModel.kt",
            "quran/ui/list/SurahListViewModel.kt", "ui/screens/namaz/NamazLocation.kt", "study/chat/ui/FilesTab.kt",
        )
        val missing = forms.filterNot { "InputRules" in main.resolve(it).readText() }
        assertEquals(emptyList<String>(), missing)
    }
}
