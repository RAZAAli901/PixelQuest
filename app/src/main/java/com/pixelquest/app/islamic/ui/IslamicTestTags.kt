package com.pixelquest.app.islamic.ui

/** Test tags on the Islamic hub (docs/islamic-hub.md). */
object IslamicTestTags {
    const val HUB = "islamic_hub"

    /** The Islamic section in Settings, and its reminders' switches. */
    const val SETTINGS = "islamic_settings"
    const val SETTINGS_DAROOD_WEEKLY = "islamic_settings_darood_weekly"
    const val SETTINGS_STUDY_DUE = "islamic_settings_study_due"
    const val SETTINGS_STUDY_WEEKLY = "islamic_settings_study_weekly"

    /** The "More coming soon" card, in debug builds. */
    const val COMING_SOON = "islamic_coming_soon"

    /** A card's icon: its emoji (Light, Simple Mode), or its drawing (Pixel, Comic). */
    const val EMOJI = "islamic_emoji"
    const val DRAWN_ICON = "islamic_drawn_icon"

    /** A card's badge: how many things wait in the item. */
    const val BADGE = "islamic_badge"

    /** An item's card, by the item's id. */
    fun card(id: String) = "islamic_card_$id"
}
