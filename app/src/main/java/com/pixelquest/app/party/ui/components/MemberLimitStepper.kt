package com.pixelquest.app.party.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

// A party's member limit, chosen when it starts: shared by every kind of party (docs/party-core.md).

/** How many the party holds, 2 to 30, the leader included: − and + around the number (UI v2's stepper). */
@Composable
fun MemberLimitStepper(count: Int, canFewer: Boolean, canMore: Boolean, onFewer: () -> Unit, onMore: () -> Unit) {
    val description = pluralStringResource(R.plurals.darood_create_limit_value, count, count)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PqFieldLabel(stringResource(R.string.darood_create_limit))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // TalkBack says what the buttons do, not their signs.
            StepButton(Icons.Rounded.Remove, stringResource(R.string.darood_create_fewer), canFewer, onFewer, DaroodTestTags.CREATE_FEWER)
            Text(
                text = count.toString(),
                style = PqType.display(24),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 28.dp).testTag(DaroodTestTags.CREATE_LIMIT).semantics { stateDescription = description }
            )
            StepButton(Icons.Rounded.Add, stringResource(R.string.darood_create_more), canMore, onMore, DaroodTestTags.CREATE_MORE)
            Text(text = stringResource(R.string.darood_create_limit_hint), style = PqType.Meta, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .size(40.dp)
            .comicPressable(onClick, radius = 12.dp, shadow = 2.dp, enabled = enabled, role = Role.Button)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = PqColors.Ink, modifier = Modifier.size(22.dp)) }
}
