package com.pixelquest.app.darood.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.PartyKinds
import com.pixelquest.app.party.ui.icon
import com.pixelquest.app.party.ui.tint
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * An invitation to a party, waiting for an answer: what kind of party (its icon and name, so
 * invitations of every kind can wait side by side), and its name. Nothing else shows: the party's
 * numbers are for its members. [enabled] is off while any invitation is being answered.
 */
@Composable
fun InviteRow(party: Party, enabled: Boolean, onAccept: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier) {
    PqCard(modifier.fillMaxWidth().testTag(DaroodTestTags.inviteRow(party.id)), shadow = PqDimens.ShadowLg) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(party.type.icon, party.type.tint)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.party_invite_to, stringResource(PartyKinds.of(party.type).titleRes)),
                        style = PqType.Meta
                    )
                    Text(text = party.name, style = PqType.BodyStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PqButton(
                    stringResource(R.string.darood_invite_accept), onAccept,
                    // Not red: the screen's one red button is its own (New party, or Create a party).
                    Modifier.weight(1f).testTag(DaroodTestTags.inviteAccept(party.id)), style = PqButtonStyle.Secondary,
                    icon = Icons.Rounded.Check, enabled = enabled, height = 44.dp
                )
                PqButton(
                    stringResource(R.string.darood_invite_decline), onDecline,
                    Modifier.weight(1f).testTag(DaroodTestTags.inviteDecline(party.id)), style = PqButtonStyle.Secondary, enabled = enabled, height = 44.dp
                )
            }
        }
    }
}
