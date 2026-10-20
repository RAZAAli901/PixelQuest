package com.pixelquest.app.darood.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import com.pixelquest.app.party.PartyKinds
import com.pixelquest.app.party.ui.phraseText

/** One party in the list: its name, then what its kind says about it (a Darood Party's members, total and milestone). */
@Composable
fun PartyRow(party: Party, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    PixelCard(
        variant = PixelPanelVariant.BORDER,
        contentPadding = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.darood_open_party), onClick = onClick)
            .testTag(DaroodTestTags.partyRow(party.id))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = party.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = inkOnPanel(colors.primaryText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (party.isFinished) {
                    Text(
                        text = stringResource(R.string.darood_finished),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primaryText
                    )
                }
            }
            // What the party's kind says about it: a Darood Party its members, total and milestone.
            val summary = PartyKinds.of(party.type).summary(party)
            Text(
                text = summary.main.map { phraseText(it) }.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            summary.progress?.let { PixelProgressBar(progress = it, height = 12.dp) }
            summary.detail?.let { detail ->
                Text(
                    text = phraseText(detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
