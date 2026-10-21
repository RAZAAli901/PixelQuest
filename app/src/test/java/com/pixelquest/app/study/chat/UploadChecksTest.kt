package com.pixelquest.app.study.chat

import com.pixelquest.app.study.chat.domain.UploadCandidate
import com.pixelquest.app.study.chat.domain.UploadChecks
import com.pixelquest.app.study.chat.domain.UploadProblem
import com.pixelquest.app.study.chat.domain.UploadRefused
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the phone says no to before sending anything: the server's rules, said at once. */
class UploadChecksTest {

    private fun problem(name: String, mime: String, size: Long = 1000) =
        (UploadChecks.check(name, mime, size).exceptionOrNull() as? UploadRefused)?.problem

    @Test
    fun theAllowedTypes_pass_namedAsTheServerWouldKeepThem() {
        assertEquals(UploadCandidate("Past paper.pdf", "application/pdf"), UploadChecks.check("Past paper.pdf", "application/pdf", 1000).getOrThrow())
        assertEquals("by the name, when the picker doesn't know", "text/csv", UploadChecks.check("marks.csv", "", 10).getOrThrow().mime)
        assertEquals("image/jpeg", UploadChecks.check("photo.JPEG", "application/octet-stream", 10).getOrThrow().mime)
        assertEquals("Week 3.docx", UploadChecks.check("../../Week 3.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 10).getOrThrow().name)
    }

    @Test
    fun everythingElse_isSaidWhy() {
        assertEquals(UploadProblem.BAD_TYPE, problem("setup.exe", "application/x-msdownload"))
        assertEquals("a name and a type that disagree", UploadProblem.BAD_TYPE, problem("notes.pdf", "image/png"))
        assertEquals(UploadProblem.BAD_TYPE, problem("page.html", "text/html"))
        assertEquals(UploadProblem.MACROS, problem("budget.xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12"))
        assertEquals(UploadProblem.MACROS, problem("essay.docm", ""))
        assertEquals(UploadProblem.ARCHIVE, problem("answers.zip", "application/zip"))
        assertEquals(UploadProblem.ARCHIVE, problem("app.apk", "application/vnd.android.package-archive"))
        assertEquals(UploadProblem.EMPTY, problem("empty.txt", "text/plain", size = 0))
        assertEquals(UploadProblem.TOO_LARGE, problem("big.pdf", "application/pdf", size = 20L * 1024 * 1024 + 1))
        assertEquals(UploadProblem.BAD_NAME, problem("///", "application/pdf"))
    }

    @Test
    fun aLongOrHostileName_isCutDownAndCleaned_asTheServerDoes() {
        val name = UploadChecks.cleanName("in\u0000voice‮" + "a".repeat(300) + ".pdf")!!
        assertEquals(120, name.length)
        assertTrue(name.endsWith(".pdf"))
        assertTrue(!name.contains('‮'))
        assertEquals("abcdefg.txt", UploadChecks.cleanName("a<b>c:d\"e|f?g*.txt"))
        assertEquals(null, UploadChecks.cleanName(".."))
    }

    @Test
    fun anUnknownSize_isLeftToTheServer() {
        assertEquals("application/pdf", UploadChecks.check("notes.pdf", "application/pdf", -1).getOrThrow().mime)
    }
}
