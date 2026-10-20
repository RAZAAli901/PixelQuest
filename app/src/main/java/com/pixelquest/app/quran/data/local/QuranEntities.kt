package com.pixelquest.app.quran.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
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
 * @param arabicPlain the Arabic as search matches it (ArabicSearch.key), from version 16: never
 *   shown. Empty until the loader fills it, which it does when it finds it empty.
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
    val translation: String,
    @ColumnInfo(defaultValue = "''") val arabicPlain: String = ""
)

/**
 * A translation on the phone: who made it, its terms, and Tanzil's notice for it, shown on the
 * About screen.
 *
 * @param id Tanzil's id for it, such as `en.sahih`.
 * @param lastUpdate the translation's own last update, as its source states it.
 * @param licenceNote its terms in a sentence (for Tanzil's translations: non-commercial use only).
 * @param notice its source's notice, whole.
 * @param dataVersion the version of the bundled files it was loaded from: once it's the bundle's,
 *   the Quran on the phone is complete and current.
 */
@Entity(tableName = "quran_translation")
data class TranslationInfoEntity(
    @PrimaryKey val id: String,
    val name: String,
    val translator: String,
    val language: String,
    val lastUpdate: String,
    val licenceNote: String,
    val notice: String,
    val dataVersion: String
)

/** Where a juz starts (from version 14), from Tanzil's metadata: the Quran's 30 parts. */
@Entity(tableName = "quran_juz")
data class QuranJuzEntity(
    @PrimaryKey val number: Int,
    val surah: Int,
    val ayah: Int
)

/**
 * A full-text index over [QuranAyahEntity.translation], for searching the translation by its words.
 * It follows its table by the row id (the ayah's place in the Quran), and Room keeps it in step.
 */
@Fts4(contentEntity = QuranAyahEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "quran_ayah_fts")
data class QuranAyahFtsEntity(val translation: String)
