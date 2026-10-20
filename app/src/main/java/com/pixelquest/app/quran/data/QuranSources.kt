package com.pixelquest.app.quran.data

import kotlinx.serialization.json.Json

/**
 * Where the bundled Quran comes from, as its own files state it, for the About and Sources screen:
 * Tanzil's notice for the text, whole and unchanged, and its metadata's.
 *
 * @param version the bundle's data version (tools/quran-build).
 * @param notice Tanzil's copyright notice for the Quran text, exactly as in its file.
 * @param metadataNotice the surah and juz data's copyright and licence, as its file states them.
 */
data class QuranSources(val version: String, val notice: String, val metadataNotice: String) {

    companion object {
        /** When the bundled files were downloaded from Tanzil (licenses/README.md). */
        const val DOWNLOADED = "2026-10-20"

        /** How much of the Arabic file's start holds them (tools/quran-build writes them before the surahs). */
        private const val HEAD_CHARS = 4_000

        /** The bundle's own statement of its sources; null if its start doesn't hold them. */
        fun read(assets: QuranAssetSource): QuranSources? {
            val head = assets.head(QuranImporter.QURAN_FILE, HEAD_CHARS)
            return QuranSources(
                version = field(head, "version") ?: return null,
                notice = field(head, "notice") ?: return null,
                metadataNotice = field(head, "metadataNotice") ?: return null
            )
        }

        /** A string field of the JSON at [head]'s top level, with its escapes read. */
        private fun field(head: String, name: String): String? {
            val quoted = Regex("\"$name\":(\"(?:[^\"\\\\]|\\\\.)*\")").find(head)?.groupValues?.get(1) ?: return null
            return Json.decodeFromString<String>(quoted)
        }
    }
}
