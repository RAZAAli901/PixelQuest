package com.pixelquest.app.darood.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * An invitation to a party, waiting for an answer. Only its name shows: the party's numbers are
 * for its members. [enabled] is off while any invitation is being answered.
 */
@Composable
fun InviteRow(party: Party, enabled: Boolean, onAccept: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        contentPadding = 16.dp,
        modifier = modifier.fillMaxWidth().testTag(DaroodTestTags.inviteRow(party.id))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.darood_invite_to),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface
            )
            Text(
                text = party.name,
                style = MaterialTheme.typography.titleMedium,
                color = inkOnPanel(colors.primaryText),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PixelButton(
                    text = stringResource(R.string.darood_invite_accept),
                    onClick = onAccept,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).testTag(DaroodTestTags.inviteAccept(party.id))
                )
                PixelButton(
                    text = stringResource(R.string.darood_invite_decline),
                    onClick = onDecline,
                    enabled = enabled,
                    variant = PixelButtonVariant.BLUE,
                    modifier = Modifier.weight(1f).testTag(DaroodTestTags.inviteDecline(party.id))
                )
            }
        }
    }
}
