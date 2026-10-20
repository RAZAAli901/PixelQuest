package com.pixelquest.app.quran.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Bookmarks in memory, for view models' tests. */
class MemoryBookmarks : BookmarkRepository {
    val marks = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())

    override fun bookmarkedIn(surah: Int): Flow<Set<Int>> = marks.map { list -> list.filter { it.first == surah }.map { it.second }.toSet() }

    override fun all(): Flow<List<Pair<Int, Int>>> = marks.map { list -> list.sortedWith(compareBy({ it.first }, { it.second })) }

    override suspend fun toggle(surah: Int, ayah: Int): Boolean {
        val mark = surah to ayah
        val now = mark !in marks.value
        marks.value = if (now) marks.value + mark else marks.value - mark
        return now
    }
}
