package com.pixelquest.app.darood.ui

import com.pixelquest.app.R
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.islamic.IslamicItemProvider
import com.pixelquest.app.ui.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.pixelquest.app.party.domain.PartyType

/**
 * Darood Party on the Islamic hub (docs/islamic-hub.md): shown while its feature flag is on, with
 * the drawn bead icons the side menu's entry had. Its badge counts invitations waiting for an answer.
 */
class DaroodIslamicItem @Inject constructor(
    private val settings: SettingsRepository,
    private val parties: DaroodPartyRepository
) : IslamicItemProvider {

    override val order: Int = 20

    override fun item(): Flow<IslamicItem> = combine(settings.daroodAvailable, invitations()) { available, invitations ->
        IslamicItem(
            id = ID,
            titleRes = R.string.darood_title,
            descriptionRes = R.string.islamic_darood_description,
            icon = IslamicIcon("📿", pixel = R.drawable.ic_darood_pixel, comic = R.drawable.ic_darood_comic),
            route = Screen.DaroodParty.route,
            enabled = available,
            badgeCount = invitations,
            badgeLabel = R.plurals.islamic_darood_badge
        )
    }

    /** Darood Parties the player is invited to and hasn't answered, from the phone's copy (empty when signed out). */
    private fun invitations(): Flow<Int> =
        parties.observeMyParties(PartyType.DAROOD).map { list -> list.count { it.myStatus == MemberStatus.INVITED } }.distinctUntilChanged()

    companion object {
        const val ID = "darood"
    }
}
