package com.pixelquest.app.quran.data

import com.pixelquest.app.quran.data.local.QuranBookmarkDao
import com.pixelquest.app.quran.data.local.QuranBookmarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The player's Quran bookmarks, kept on the phone. */
interface BookmarkRepository {
    /** The bookmarked ayahs of [surah], by number. */
    fun bookmarkedIn(surah: Int): Flow<Set<Int>>

    /** Every bookmark, in the Quran's order: (surah, ayah). */
    fun all(): Flow<List<Pair<Int, Int>>>

    /** Bookmarks [surah]:[ayah], or removes its bookmark; returns whether it's bookmarked now. */
    suspend fun toggle(surah: Int, ayah: Int): Boolean
}

@Singleton
class RoomBookmarkRepository(
    private val dao: QuranBookmarkDao,
    private val now: () -> Long
) : BookmarkRepository {

    @Inject
    constructor(dao: QuranBookmarkDao) : this(dao, System::currentTimeMillis)

    override fun bookmarkedIn(surah: Int): Flow<Set<Int>> = dao.observeIn(surah).map { it.toSet() }

    override fun all(): Flow<List<Pair<Int, Int>>> = dao.observeAll().map { list -> list.map { it.surah to it.ayah } }

    override suspend fun toggle(surah: Int, ayah: Int): Boolean =
        if (ayah in dao.observeIn(surah).first()) {
            dao.delete(surah, ayah)
            false
        } else {
            dao.insert(QuranBookmarkEntity(surah, ayah, now()))
            true
        }
}
