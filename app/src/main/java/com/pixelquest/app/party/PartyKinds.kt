package com.pixelquest.app.party

import com.pixelquest.app.darood.ui.DaroodPartyKind
import com.pixelquest.app.party.domain.PartyKind
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.ui.StudyPartyKind

/** Every kind of party, by its type. A new kind is one more [PartyKind], and one more line here. */
object PartyKinds {
    val all: List<PartyKind> = listOf(DaroodPartyKind, StudyPartyKind)

    fun of(type: PartyType): PartyKind = all.first { it.type == type }
}
