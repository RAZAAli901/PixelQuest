package com.pixelquest.app.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The Islamic hub, the Quran, Darood Party, the party core, Study parties and the first-run tips are
 * ready to translate: none of their code puts text on screen that isn't a string resource, and their
 * strings say what each value is (an xliff:g with an example) and where it goes when there are
 * several (%1$s), so a translator can move them as their grammar needs.
 */
class StringsAuditTest {

    private fun repo(path: String) = listOf(File("../$path"), File(path)).first { it.exists() }

    private val packages = listOf("islamic", "quran", "darood", "study", "party", "ui/tips")

    private fun sources(): List<File> = packages.flatMap { pkg ->
        repo("app/src/main/java/com/pixelquest/app/$pkg").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    /** Text given to the screen as a literal: text, a description, a title, a click's label. */
    private val shown = Regex("""(?:\b(?:text|contentDescription|title|subtitle|placeholder|onClickLabel)\s*=\s*|\bText\(\s*)"((?:[^"\\]|\\.)*)"""")

    /** A literal with words in it, once its ${...} and $name templates are taken out. */
    private fun hasWords(literal: String) =
        Regex("""\p{L}{2,}""").containsMatchIn(literal.replace(Regex("""\$\{[^}]*}"""), "").replace(Regex("""\$\w+"""), ""))

    @Test
    fun theirCode_putsNoTextOnScreen_thatIsntAResource() {
        val files = sources()
        assertTrue(files.size > 100)
        val found = files.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                val code = line.substringBefore("//").trim()
                if (code.startsWith("*") || code.startsWith("/*")) return@mapIndexedNotNull null
                shown.findAll(code).map { it.groupValues[1] }.firstOrNull(::hasWords)?.let { "${file.name}:${i + 1}: \"$it\"" }
            }
        }
        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun theirStrings_markEveryValue_andNumberThemWhenThereAreSeveral() {
        val files = listOf("strings_islamic.xml", "strings_quran.xml", "strings_darood.xml", "strings_party.xml", "strings_study.xml")
            .map { repo("app/src/main/res/values/$it") }
        val placeholder = Regex("""%(\d+\$)?[sd]""")
        val problems = files.flatMap { file ->
            // Each string, or each plural's item, on its own.
            Regex("""<(string|item)\b[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL).findAll(file.readText()).flatMap { entry ->
                val body = entry.groupValues[2]
                val values = placeholder.findAll(body).toList()
                val unmarked = Regex("""%(\d+\$)?[sd]""").findAll(body.replace(Regex("""<xliff:g\b[^>]*>.*?</xliff:g>""", RegexOption.DOT_MATCHES_ALL), "")).toList()
                buildList {
                    if (unmarked.isNotEmpty()) add("${file.name}: a value with no xliff:g in \"${body.take(80)}\"")
                    if (values.size > 1 && values.any { it.groupValues[1].isEmpty() }) add("${file.name}: several values, not numbered, in \"${body.take(80)}\"")
                    Regex("""<xliff:g\b([^>]*)>""").findAll(body).filter { "example=" !in it.groupValues[1] }
                        .forEach { add("${file.name}: an xliff:g with no example in \"${body.take(80)}\"") }
                }
            }.toList()
        }
        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun theCheck_catchesTextLeftInCode_andLetsSymbolsAndJoinsBy() {
        fun caught(line: String) = shown.findAll(line).map { it.groupValues[1] }.any(::hasWords)
        assertTrue(caught("""Text(text = "Due soon", style = s)"""))
        assertTrue(caught("""Text("Mark done")"""))
        assertTrue(caught("""IconButton(onClick = {}) { SymbolIcon(symbol = "x", contentDescription = "Item actions") }"""))
        assertTrue(caught("""Text(text = "${'$'}{count} points")"""))
        assertTrue(!caught("""Text(text = "📌")"""))
        assertTrue(!caught("""Text(text = "${'$'}place · ${'$'}count")"""))
        assertTrue(!caught("""Text(text = stringResource(R.string.study_add_item))"""))
    }

    @Test
    fun theTips_areResourcesToo() {
        val strings = repo("app/src/main/res/values/strings.xml").readText()
        for (name in listOf("tip_got_it", "tip_hub_title", "tip_hub_body", "tip_quran_title", "tip_quran_body", "tip_study_title", "tip_study_body")) {
            assertTrue(name, "name=\"$name\"" in strings)
        }
    }
}
