package com.pixelquest.app.study.chat

import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatFileType
import com.pixelquest.app.study.chat.ui.FilesQuery
import com.pixelquest.app.study.chat.ui.filesFor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** The Files tab: searched by name, filtered by kind, ordered by date, pinned on top. */
class FilesTabTest {
    private fun file(id: String, name: String, mime: String, day: Int, pinned: Boolean = false) =
        ChatFile(id, "p1", "u", name, mime, 1, null, ChatFileStatus.READY, null, pinned, Instant.parse("2026-10-%02dT00:00:00Z".format(day)), null)

    private val files = listOf(
        file("a", "Waves notes.pdf", "application/pdf", 1),
        file("b", "Marks.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 3),
        file("c", "Past paper.pdf", "application/pdf", 2, pinned = true)
    )

    @Test
    fun searchFilterAndOrder() {
        assertEquals(listOf("c", "b", "a"), filesFor(files, FilesQuery()).map { it.id })
        assertEquals(listOf("c", "a", "b"), filesFor(files, FilesQuery(newestFirst = false)).map { it.id })
        assertEquals(listOf("c", "a"), filesFor(files, FilesQuery(type = ChatFileType.PDF)).map { it.id })
        assertEquals(listOf("a"), filesFor(files, FilesQuery(search = "WAVES")).map { it.id })
        assertEquals(emptyList<String>(), filesFor(files, FilesQuery(search = "zzz")).map { it.id })
    }
}
