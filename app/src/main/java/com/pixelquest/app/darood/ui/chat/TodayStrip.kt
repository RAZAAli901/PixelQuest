package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.DayTotals
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.PixelTheme

/** Under the chat's top bar: what the player has added today, and the whole party. */
@Composable
fun TodayStrip(today: DayTotals, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { }
            .testTag(DaroodTestTags.CHAT_TODAY)
    ) {
        Text(
            text = stringResource(R.string.darood_today_mine, formatCount(today.mine)),
            style = MaterialTheme.typography.labelLarge,
            color = colors.primaryText
        )
        Text(text = "|", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        Text(
            text = stringResource(R.string.darood_today_party, formatCount(today.party)),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface
        )
    }
}
