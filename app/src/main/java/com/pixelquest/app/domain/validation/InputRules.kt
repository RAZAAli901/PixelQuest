package com.pixelquest.app.domain.validation

import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.domain.HeroName
import com.pixelquest.app.party.domain.PartyRules
import com.pixelquest.app.study.chat.domain.StudyChatConfig
import com.pixelquest.app.study.domain.ReminderRules
import com.pixelquest.app.study.domain.StudyRules

/**
 * The one place every form's text goes through (S053). Each field has a limit. While the player
 * types, [typed] drops control characters and invisible direction marks, turns line breaks in a
 * one-line field into spaces, and stops at the limit. Some fields stop a little past the limit
 * ([Field.slack]) so the form can say "too long" instead of cutting silently. [problem] says why
 * a value can't be saved.
 *
 * The rules that belong to one kind of name stay with it: [Username.problemWith], [HeroName.error],
 * the display name's in AccountViewModel. They read their limits from here, and the server checks
 * the same limits again (supabase/migrations, input_constraints).
 */
object InputRules {

    enum class Field(val max: Int, val multiline: Boolean = false, val slack: Int = 0) {
        QUEST_NAME(100),
        HERO_NAME(HeroName.MAX_LENGTH),
        DISPLAY_NAME(20, slack = 10),
        // "@" and spaces come off when it's normalised, and "too long" should still show.
        USERNAME(Username.MAX_LENGTH, slack = 10),
        PARTY_NAME(PartyRules.NAME_MAX, slack = 10),
        STUDY_TITLE(StudyRules.TITLE_MAX),
        STUDY_SUBJECT(StudyRules.SUBJECT_MAX),
        STUDY_NOTES(StudyRules.NOTES_MAX, multiline = true),
        REMINDER_TITLE(ReminderRules.TITLE_MAX),
        CHAT_MESSAGE(StudyChatConfig.MESSAGE_MAX_CHARS, multiline = true),
        FILE_NAME(StudyChatConfig.FILE_NAME_MAX_CHARS),
        EMAIL(254),
        SEARCH(80),
    }

    enum class Problem { BLANK, TOO_LONG }

    /**
     * Direction overrides and isolates, the zero-width space and the BOM: they can disguise what a
     * name says. The joiners stay (emoji and Urdu need them), and so do the plain direction marks.
     */
    private val INVISIBLE = Regex("[\u200B\u202A-\u202E\u2066-\u2069\uFEFF]")

    /** What the field keeps as the player types or pastes. */
    fun typed(raw: String, field: Field): String {
        val text = buildString(raw.length) {
            for (c in raw) {
                when {
                    c == '\n' && field.multiline -> append(c)
                    c == '\n' || c == '\t' || c == '\r' -> append(' ')
                    Character.isISOControl(c) -> Unit
                    else -> append(c)
                }
            }
        }.replace(INVISIBLE, "")
        return text.take(field.max + field.slack)
    }

    /** Why [raw] can't be saved in [field], or null if it can. */
    fun problem(raw: String, field: Field, required: Boolean = true): Problem? {
        val value = typed(raw, field).trim()
        return when {
            value.isEmpty() -> if (required) Problem.BLANK else null
            value.length > field.max -> Problem.TOO_LONG
            else -> null
        }
    }

    /**
     * Text another player wrote, as it arrives from the server (S054): cleaned the same way and cut
     * at the field's limit before anything shows it. It is always shown as plain text: no screen
     * renders HTML, markdown or links (PlainTextRenderingTest).
     */
    fun shown(raw: String, field: Field): String = typed(raw, field).take(field.max)

    fun shownOrNull(raw: String?, field: Field): String? = raw?.let { shown(it, field) }

    /** The value to save: cleaned and trimmed. */
    fun saved(raw: String, field: Field): String = typed(raw, field).trim().take(field.max)

    /** A number field: digits only, at most [maxDigits]. */
    fun digits(raw: String, maxDigits: Int): String = raw.filter { it in '0'..'9' }.take(maxDigits)
}
