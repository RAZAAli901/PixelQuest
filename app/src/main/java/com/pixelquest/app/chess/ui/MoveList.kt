package com.pixelquest.app.chess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

object MoveListTags {
    const val LIST = "chess_move_list"
    fun move(ply: Int) = "chess_move_$ply"
}

/**
 * The moves so far in standard notation, as the design's chips: "3. Bc4", then Black's "Bc5".
 * The latest is in ink with its number in gold, and the list keeps it in view as the game goes on.
 */
@Composable
fun MoveList(moves: List<ChessMove>, modifier: Modifier = Modifier) {
    val list = rememberLazyListState()
    LaunchedEffect(moves.size) { if (moves.isNotEmpty()) list.animateScrollToItem(moves.lastIndex) }
    LazyRow(
        state = list,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 6.dp),
        modifier = modifier.fillMaxWidth().testTag(MoveListTags.LIST)
    ) {
        itemsIndexed(moves, key = { _, m -> m.ply }) { i, move ->
            val current = i == moves.lastIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag(MoveListTags.move(move.ply))
                    .comic(bg = if (current) PqColors.Ink else PqColors.Paper, radius = PqDimens.RadiusSm, border = PqDimens.BorderThin, shadow = 0.dp)
                    .padding(horizontal = 8.dp)
            ) {
                if (move.ply % 2 == 1) {
                    Text(text = "${(move.ply + 1) / 2}.", style = PqType.MetaStrong.copy(color = if (current) PqColors.Gold else PqColors.Muted))
                }
                Text(text = move.san, style = PqType.MetaStrong.copy(color = if (current) PqColors.Paper else PqColors.Ink))
            }
        }
    }
}
