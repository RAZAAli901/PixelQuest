package com.pixelquest.app.chess.ui

import com.pixelquest.app.ui.motion.clockAlarm
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.ChessClock
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ClockSync
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import kotlinx.coroutines.delay

object ChessClockTags {
    fun clock(colour: ChessColour) = "chess_clock_${colour.name.lowercase()}"
    fun low(colour: ChessColour) = "chess_clock_low_${colour.name.lowercase()}"
}

/** Under this, a clock warns (J145). */
const val LOW_TIME_MS = 10_000L

/**
 * The phone's time, ticking while a game runs, for the clocks: every 100 ms (they show tenths
 * under 10 seconds), and not at all once it's over.
 */
@Composable
fun rememberTicking(running: Boolean, phoneClock: () -> Long = System::currentTimeMillis): Long {
    var now by remember { mutableLongStateOf(phoneClock()) }
    LaunchedEffect(running) {
        while (running) {
            now = phoneClock()
            delay(100)
        }
        now = phoneClock()
    }
    return now
}

/**
 * One player beside the board, as the design draws them (J145): their avatar, name and Int points,
 * what they've captured, and their clock. The side to move has its clock in ink with paper text;
 * under 10 seconds it turns red.
 */
@Composable
fun PlayerRow(
    game: ChessGame,
    colour: ChessColour,
    name: String,
    points: Int?,
    avatarIndex: Int,
    now: Long,
    sync: ClockSync,
    captured: @Composable () -> Unit = {}
) {
    val left = ChessClock.remaining(game, colour, now, sync)
    val active = game.status == GameStatus.ACTIVE && game.turn == colour || game.status == GameStatus.WAITING && colour == ChessColour.WHITE
    val low = left < LOW_TIME_MS && game.status.live
    val shown = ChessClock.format(left)
    val said = stringResource(R.string.chess_clock_said, name, shown)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
    ) {
        InitialAvatar(name.removePrefix("@"), 36.dp, ChessLook.avatar(avatarIndex))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = name, style = PqType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                points?.let { com.pixelquest.app.ui.motion.RollingNumber(it, PqType.Meta, format = { n -> com.pixelquest.app.darood.ui.formatCount(n) }) }
            }
            Box(Modifier.height(16.dp)) { captured() }
        }
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .widthIn(min = 86.dp)
                .height(40.dp)
                .testTag(if (low) ChessClockTags.low(colour) else ChessClockTags.clock(colour))
                // Under 10 seconds the side to move's clock flashes and shakes each second (C05).
                .clockAlarm(left, low && active)
                .semantics { contentDescription = said }
                .comic(
                    bg = when {
                        low -> PqColors.Red
                        active -> PqColors.Ink
                        else -> PqColors.Paper
                    },
                    radius = PqDimens.RadiusSm,
                    shadow = if (colour == ChessColour.WHITE) 2.dp else 0.dp
                )
                .padding(horizontal = 10.dp)
        ) {
            Text(text = shown, style = ChessLook.clock.copy(color = if (active && !low) PqColors.Paper else PqColors.Ink))
        }
    }
}
