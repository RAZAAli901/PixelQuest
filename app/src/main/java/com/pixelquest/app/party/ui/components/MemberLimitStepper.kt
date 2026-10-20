package com.pixelquest.app.party.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.theme.PixelTheme

// A party's member limit, chosen when it starts: shared by every kind of party (docs/party-core.md).

/** How many the party holds, 2 to 30, the leader included: − and + around the number. */
@Composable
fun MemberLimitStepper(count: Int, canFewer: Boolean, canMore: Boolean, onFewer: () -> Unit, onMore: () -> Unit) {
    val colors = PixelTheme.colors
    val description = pluralStringResource(R.plurals.darood_create_limit_value, count, count)
    // TalkBack says what the buttons do, not their signs.
    val fewer = stringResource(R.string.darood_create_fewer)
    val more = stringResource(R.string.darood_create_more)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(R.string.darood_create_limit), style = MaterialTheme.typography.labelLarge, color = colors.primaryText)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PixelButton(
                text = "−",
                onClick = onFewer,
                enabled = canFewer,
                modifier = Modifier.testTag(DaroodTestTags.CREATE_FEWER).semantics { contentDescription = fewer }
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                modifier = Modifier
                    .testTag(DaroodTestTags.CREATE_LIMIT)
                    .semantics { stateDescription = description }
            )
            PixelButton(
                text = "+",
                onClick = onMore,
                enabled = canMore,
                modifier = Modifier.testTag(DaroodTestTags.CREATE_MORE).semantics { contentDescription = more }
            )
        }
        Text(text = stringResource(R.string.darood_create_limit_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}
