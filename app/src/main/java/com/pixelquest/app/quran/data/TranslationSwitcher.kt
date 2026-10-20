package com.pixelquest.app.quran.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A translation the app bundles: one file in assets/quran, `quran_<language>.json.gz` as
 * tools/quran-build writes it (docs/quran-data.md), unzipped by the build into the APK.
 *
 * @param file its name in the APK, such as `quran_en.json`.
 */
data class BundledTranslation(
    val id: String,
    val name: String,
    val translator: String,
    val language: String,
    val file: String
)

/** Which translation the player reads, kept on the phone. */
interface TranslationChoiceStore {
    /** The chosen translation's id, or null if none has been chosen. */
    fun chosen(): String?

    fun choose(id: String)
}

@Singleton
class PrefsTranslationChoiceStore @Inject constructor(
    @ApplicationContext context: Context
) : TranslationChoiceStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun chosen(): String? = prefs.getString(KEY, null)

    override fun choose(id: String) {
        prefs.edit().putString(KEY, id).apply()
    }

    private companion object {
        const val PREFS = "pixelquest_quran"
        const val KEY = "translation"
    }
}

/**
 * The translations the app bundles, found by their files, and which one the player reads. A new
 * translation is added as one more `quran_<language>.json.gz` from the tool, with nothing to change
 * here: each file says who it is at its start.
 */
@Singleton
class TranslationSwitcher @Inject constructor(
    private val assets: QuranAssetSource,
    private val store: TranslationChoiceStore
) {
    /** Every bundled translation, in the order of their files' names. */
    fun available(): List<BundledTranslation> =
        assets.list()
            .filter { it.startsWith(PREFIX) && it.endsWith(SUFFIX) && it != QuranImporter.QURAN_FILE }
            .sorted()
            .mapNotNull { file -> describe(file) }

    /** The translation the player reads: their choice while it's bundled, else Saheeh International, else the first. */
    fun selected(): BundledTranslation {
        val all = available()
        if (all.isEmpty()) throw QuranImportException("no translation is bundled")
        return all.firstOrNull { it.id == store.chosen() }
            ?: all.firstOrNull { it.id == DEFAULT_ID }
            ?: all.first()
    }

    /** Chooses [id], if it's bundled; the Quran's loader then loads it in place of the last. */
    fun select(id: String): Boolean {
        if (available().none { it.id == id }) return false
        store.choose(id)
        return true
    }

    /** Who a file's translation is, from its start (where the tool writes it). */
    private fun describe(file: String): BundledTranslation? {
        val head = assets.head(file, HEAD_CHARS)
        fun field(name: String) = Regex("\"$name\":\"([^\"]*)\"").find(head)?.groupValues?.get(1)
        return BundledTranslation(
            id = field("id") ?: return null,
            name = field("name") ?: return null,
            translator = field("translator") ?: return null,
            language = field("language") ?: return null,
            file = file
        )
    }

    companion object {
        /** The translation read until another is chosen: Saheeh International. */
        const val DEFAULT_ID = "en.sahih"

        private const val PREFIX = "quran_"
        private const val SUFFIX = ".json"
        private const val HEAD_CHARS = 400
    }
}
