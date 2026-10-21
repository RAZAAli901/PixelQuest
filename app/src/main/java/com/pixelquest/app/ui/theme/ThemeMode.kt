package com.pixelquest.app.ui.theme

/**
 * The skin the player chose in Settings > Look (docs/ui-v2.md), stored by its [id]: [Comic] or
 * [Pixel]. Versions before UI v2 also offered Clean Light and Follow System; a phone that kept one
 * of those wears Comic, as it has since UI v2.
 */
enum class ThemeMode(val id: String) {
    Pixel("pixel"),
    Comic("comic");

    /** The UI v2 skin this choice wears. */
    val skin: PqSkin get() = if (this == Pixel) PqSkin.Pixel else PqSkin.Comic

    companion object {
        /** The skin of a player who hasn't chosen one: Comic (from 1.2.0; Pixel before). */
        val DEFAULT: ThemeMode get() = Comic

        /** The ids of the looks before UI v2 that wear Comic now. */
        private val WEARS_COMIC = setOf("comic", "light", "system")

        /** The skin stored as [id]: Comic for "comic" and the old "light" and "system", Pixel otherwise. */
        fun fromId(id: String?): ThemeMode =
            if (id?.trim()?.lowercase() in WEARS_COMIC) Comic else Pixel
    }
}
