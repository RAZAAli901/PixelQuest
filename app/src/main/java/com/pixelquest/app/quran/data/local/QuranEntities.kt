package com.pixelquest.app.quran.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// The Quran on the phone (docs/quran-data.md): loaded once from the app's bundled files
// (assets/quran), never from the internet, and never changed after that except by a newer bundle.
// Every text column holds its source's text exactly.

/**
 * One surah's details, from Tanzil's metadata.
 *
 * @param englishName its name in English letters (Al-Faatiha); [englishMeaning] what it means (The Opening).
 * @param revelation `Meccan` or `Medinan`, as the source writes it.
 * @param juz the juz its first ayah is in.
 * @param bismillah the Bismillah shown above it, exactly as the source gives it; null for Al-Fatihah,
 *   whose first ayah it is, and At-Tawbah, which has none.
 */
@Entity(tableName = "quran_surah")
data class QuranSurahEntity(
    @PrimaryKey val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishMeaning: String,
    val revelation: String,
    val revelationOrder: Int,
    val ayahCount: Int,
    val juz: Int,
    val bismillah: String?
)

/**
 * One ayah: its Arabic, and its translation in the translation the player reads.
 *
 * @param id its place in the whole Quran, 1 to 6,236: the row's own id, which the translation's
 *   search index follows, so it never changes.
 * @param surah, [ayah] where it is: unique together.
 * @param translationId the translation [translation] is from, such as `en.sahih`.
 */
@Entity(
    tableName = "quran_ayah",
    indices = [Index(value = ["surah", "ayah"], unique = true)]
)
data class QuranAyahEntity(
    @PrimaryKey val id: Int,
    val surah: Int,
    val ayah: Int,
    val arabic: String,
    val translationId: String,
    val translation: String
)
