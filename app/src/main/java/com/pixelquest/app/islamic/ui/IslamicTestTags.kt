package com.pixelquest.app.islamic.ui

/** Test tags on the Islamic hub (docs/islamic-hub.md). */
object IslamicTestTags {
    const val HUB = "islamic_hub"

    /** The "More coming soon" card, in debug builds. */
    const val COMING_SOON = "islamic_coming_soon"

    /** A card's badge: how many things wait in the item. */
    const val BADGE = "islamic_badge"

    /** An item's card, by the item's id. */
    fun card(id: String) = "islamic_card_$id"
}
