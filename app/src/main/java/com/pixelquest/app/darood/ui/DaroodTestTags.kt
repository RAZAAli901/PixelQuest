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
    const val CREATE = "darood_create"
    const val CREATE_ACTION = "darood_create_action"
    const val CHAT_SCREEN = "darood_chat_screen"
    const val CREATE_SCREEN = "darood_create_screen"
    const val CREATE_NAME = "darood_create_name"
    const val CREATE_NAME_COUNTER = "darood_create_name_counter"
    const val CREATE_LIMIT = "darood_create_limit"
    const val CREATE_FEWER = "darood_create_fewer"
    const val CREATE_MORE = "darood_create_more"
    const val CREATE_MILESTONE = "darood_create_milestone"

    fun partyRow(partyId: String) = "darood_party_$partyId"
    fun inviteRow(partyId: String) = "darood_invite_$partyId"
    fun inviteAccept(partyId: String) = "darood_invite_accept_$partyId"
    fun inviteDecline(partyId: String) = "darood_invite_decline_$partyId"
}
