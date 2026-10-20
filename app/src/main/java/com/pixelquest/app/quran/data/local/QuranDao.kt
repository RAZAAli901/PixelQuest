package com.pixelquest.app.quran.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The Quran on the phone (docs/quran-data.md): read by the reader, written only by its loader. */
@Dao
interface QuranDao {

    // Surahs

    /** All 114, in order (empty until the Quran is loaded). */
    @Query("SELECT * FROM quran_surah ORDER BY number")
    fun observeSurahs(): Flow<List<QuranSurahEntity>>

    @Query("SELECT * FROM quran_surah WHERE number = :number")
    suspend fun getSurah(number: Int): QuranSurahEntity?

    @Query("SELECT COUNT(*) FROM quran_surah")
    suspend fun surahCount(): Int

    // Ayahs

    /** A page of [surah]'s ayahs in order: [limit] of them, from the [offset]th (0 for its first). */
    @Query("SELECT * FROM quran_ayah WHERE surah = :surah ORDER BY ayah LIMIT :limit OFFSET :offset")
    suspend fun ayahsOf(surah: Int, offset: Int, limit: Int): List<QuranAyahEntity>

    /** [surah]'s ayahs [from] to [to], both included, in order. */
    @Query("SELECT * FROM quran_ayah WHERE surah = :surah AND ayah BETWEEN :from AND :to ORDER BY ayah")
    suspend fun ayahRange(surah: Int, from: Int, to: Int): List<QuranAyahEntity>

    @Query("SELECT * FROM quran_ayah WHERE surah = :surah AND ayah = :ayah")
    suspend fun getAyah(surah: Int, ayah: Int): QuranAyahEntity?

    @Query("SELECT COUNT(*) FROM quran_ayah")
    suspend fun ayahCount(): Int

    /** How many ayahs hold [translationId]'s translation: all of them once it's the one loaded. */
    @Query("SELECT COUNT(*) FROM quran_ayah WHERE translationId = :translationId")
    suspend fun ayahCountIn(translationId: String): Int

    /**
     * Ayahs whose translation matches [match], an FTS query (QuranRepository builds it from what
     * the player typed), in the Quran's order: at most [limit].
     */
    @Query(
        """
        SELECT quran_ayah.* FROM quran_ayah
        JOIN quran_ayah_fts ON quran_ayah.id = quran_ayah_fts.rowid
        WHERE quran_ayah_fts MATCH :match
        ORDER BY quran_ayah.id
        LIMIT :limit
        """
    )
    suspend fun searchTranslation(match: String, limit: Int): List<QuranAyahEntity>

    // Translations

    @Query("SELECT * FROM quran_translation WHERE id = :id")
    suspend fun getTranslation(id: String): TranslationInfoEntity?

    @Query("SELECT * FROM quran_translation ORDER BY language, name")
    fun observeTranslations(): Flow<List<TranslationInfoEntity>>

    // Loading: only QuranImporter writes, in one transaction.

    @Query("DELETE FROM quran_ayah")
    suspend fun deleteAyahs()

    @Query("DELETE FROM quran_surah")
    suspend fun deleteSurahs()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSurahs(surahs: List<QuranSurahEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAyahs(ayahs: List<QuranAyahEntity>)

    @Upsert
    suspend fun upsertTranslation(translation: TranslationInfoEntity)
}
