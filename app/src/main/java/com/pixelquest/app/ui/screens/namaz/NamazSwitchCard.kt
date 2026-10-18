package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * The tracker's master switch, at the top of the screen. The whole row toggles it (a bigger
 * target than the switch alone), and TalkBack reads it as one switch with its label.
 */
@Composable
fun NamazSwitchCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    PixelCard(variant = PixelPanelVariant.BORDER, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                .testTag(NamazTestTags.SWITCH),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.namaz_switch_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = inkOnPanel(colors.primaryText)
                )
                Text(
                    text = stringResource(if (enabled) R.string.namaz_switch_on else R.string.namaz_switch_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // The row handles the toggle; the switch only shows it.
            Switch(
                checked = enabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = colors.onSurfaceVariant,
                    uncheckedTrackColor = colors.surfaceVariant,
                    uncheckedBorderColor = colors.pixelBorder
                )
            )
        }
    }
}

/** While the tracker is off: what turning it on gives, in a few words. */
@Composable
fun NamazExplainerCard(modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    PixelCard(variant = PixelPanelVariant.BLUE, modifier = modifier.fillMaxWidth().testTag(NamazTestTags.EXPLAINER)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "🕌  " + stringResource(R.string.namaz_explainer_title),
                style = MaterialTheme.typography.titleSmall,
                color = inkOnPanel(colors.primaryText)
            )
            Text(
                text = stringResource(R.string.namaz_explainer_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        }
    }
}
