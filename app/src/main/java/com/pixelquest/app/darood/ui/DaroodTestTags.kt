package com.pixelquest.app.darood.ui

/** Test tags for Darood Party's screens. */
object DaroodTestTags {
    const val USERNAME_SCREEN = "darood_username_screen"
    const val USERNAME_FIELD = "darood_username_field"
    const val USERNAME_STATUS = "darood_username_status"
    const val USERNAME_SAVE = "darood_username_save"
    const val PROFILE_USERNAME = "darood_profile_username"
    const val PARTY_SCREEN = "darood_party_screen"
    const val SIGNED_OUT = "darood_signed_out"
    const val SIGN_IN = "darood_sign_in"
    const val EMPTY = "darood_empty"
    const val PARTY_LIST = "darood_party_list"
    const val CHAT_SCREEN = "darood_chat_screen"

    fun partyRow(partyId: String) = "darood_party_$partyId"
}
