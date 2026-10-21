package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqSwitch
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

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
    PqCard(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                .testTag(NamazTestTags.SWITCH)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = stringResource(R.string.namaz_switch_label), style = PqType.BodyStrong)
                Text(text = stringResource(if (enabled) R.string.namaz_switch_on else R.string.namaz_switch_off), style = PqType.Meta)
            }
            Spacer(modifier = Modifier.width(12.dp))
            // The row handles the toggle; the switch only shows it.
            PqSwitch(checked = enabled, onCheckedChange = null)
        }
    }
}

/** While the tracker is off: what turning it on gives, in a few words. */
@Composable
fun NamazExplainerCard(modifier: Modifier = Modifier) {
    PqCard(modifier.fillMaxWidth().testTag(NamazTestTags.EXPLAINER), bg = PqColors.Sky) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryTile(Icons.Rounded.Mosque, PqColors.Paper)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.namaz_explainer_title), style = PqType.BodyStrong)
                Text(text = stringResource(R.string.namaz_explainer_body), style = PqType.Body.copy(color = PqColors.Ink2))
            }
        }
    }
}
