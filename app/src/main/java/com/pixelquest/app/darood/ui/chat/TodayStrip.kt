package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.DayTotals
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * Under the chat's top bar (mock 4E): what the player has added today, and the whole party. With
 * large text the two go one under the other, so neither is squeezed.
 */
@Composable
fun TodayStrip(today: DayTotals, modifier: Modifier = Modifier) {
    val mine = stringResource(R.string.darood_today_mine, formatCount(today.mine))
    val party = stringResource(R.string.darood_today_party, formatCount(today.party))
    val strip = modifier
        .fillMaxWidth()
        .padding(horizontal = PqDimens.ScreenPad, vertical = 2.dp)
        .semantics(mergeDescendants = true) { }
        .testTag(DaroodTestTags.CHAT_TODAY)
    if (LocalDensity.current.fontScale >= LARGE_TEXT) {
        Column(modifier = strip) {
            Text(text = mine, style = PqType.MetaStrong)
            Text(text = party, style = PqType.MetaStrong)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = strip) {
            Text(text = mine, style = PqType.MetaStrong, modifier = Modifier.weight(1f))
            Text(text = party, style = PqType.MetaStrong)
        }
    }
}

/** From this font scale up (the phone's larger text sizes), the strip's two numbers stack. */
private const val LARGE_TEXT = 1.5f
