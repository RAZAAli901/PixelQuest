package com.pixelquest.app.domain

import androidx.annotation.DrawableRes
import com.pixelquest.app.R

data class AvatarItem(
    val id: String,
    val name: String,
    @DrawableRes val drawableRes: Int
)

object AvatarCatalog {
    val DEFAULT_AVATAR_ID = "avatar_hero"

    val avatars: List<AvatarItem> = listOf(
        AvatarItem("avatar_hero", "Hero", R.drawable.avatar_hero),
        AvatarItem("avatar_mage", "Mage", R.drawable.avatar_mage),
        AvatarItem("avatar_rogue", "Rogue", R.drawable.avatar_rogue),
        AvatarItem("avatar_warrior", "Warrior", R.drawable.avatar_warrior),
        AvatarItem("avatar_paladin", "Paladin", R.drawable.avatar_paladin),
        AvatarItem("avatar_ranger", "Ranger", R.drawable.avatar_ranger)
    )

    fun getAvatarById(avatarId: String?): AvatarItem {
        return avatars.find { it.id == avatarId } ?: avatars.first()
    }
}
