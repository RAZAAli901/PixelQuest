package com.pixelquest.app.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape

/**
 * One hero on the board, as the locked board's preview draws them (mock 2J): the rank, their initial,
 * their name and what they're ranked by, and the number. The first three's ranks sit on gold, silver
 * and bronze; the player's own row is gold. Anyone else's name can be reported.
 */
@Composable
fun PixelLeaderboardRow(
    rank: Int,
    displayName: String,
    statLabel: String,
    statValue: String,
    modifier: Modifier = Modifier,
    isCurrentUser: Boolean = false,
    @Suppress("UNUSED_PARAMETER") avatarId: String? = null,
    onReportClicked: ((displayName: String) -> Unit)? = null
) {
    val medal = when (rank) {
        1 -> PqColors.Gold
        2 -> PqColors.Sunken
        3 -> PqColors.Amber
        else -> null
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .comic(bg = if (isCurrentUser) PqColors.Gold else PqColors.Paper, radius = 12.dp, border = PqDimens.BorderThin, shadow = 0.dp)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .widthIn(min = 30.dp)
                .then(if (medal != null) Modifier.comic(bg = medal, radius = 15.dp, border = PqDimens.BorderThin, shadow = 0.dp).padding(horizontal = 6.dp, vertical = 2.dp) else Modifier)
        ) {
            Text(text = rank.toString(), style = PqType.number(20))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(34.dp).clip(pqShape(8.dp)).background(PqColors.Ink)
        ) {
            Text(text = displayName.take(1).uppercase(), style = PqType.Button.copy(color = PqColors.Paper))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, style = PqType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = statLabel, style = PqType.Meta)
        }
        Text(text = statValue, style = PqType.BodyStrong)
        if (onReportClicked != null) {
            IconButton(onClick = { onReportClicked(displayName) }) {
                Icon(Icons.Rounded.Flag, contentDescription = "Report $displayName", tint = PqColors.Muted)
            }
        } else {
            Box(Modifier.size(8.dp))
        }
    }
}
