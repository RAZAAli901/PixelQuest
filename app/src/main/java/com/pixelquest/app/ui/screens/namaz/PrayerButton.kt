package com.pixelquest.app.ui.screens.namaz

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerSlot
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import kotlinx.coroutines.launch

/** How a prayer's button looks: what the player can do with it now. */
enum class PrayerButtonState(@StringRes val label: Int, val mark: String?) {
    /** Its time has come and it isn't marked: tap to mark it prayed. */
    PENDING(R.string.namaz_status_pending, null),
    PRAYED(R.string.namaz_status_prayed, "✓"),
    /** The next prayer started with this one unmarked. */
    MISSED(R.string.namaz_status_missed, "✕"),
    /** Its time hasn't come yet. */
    LOCKED(R.string.namaz_status_locked, "🔒");

    companion object {
        fun of(slot: PrayerSlot): PrayerButtonState = when {
            slot.status == PrayerStatus.PRAYED -> PRAYED
            slot.status == PrayerStatus.MISSED -> MISSED
            !slot.started -> LOCKED
            else -> PENDING
        }
    }
}

/** Each prayer's picture: dawn, midday sun, afternoon, sunset, night. */
val Prayer.icon: String
    get() = when (this) {
        Prayer.FAJR -> "🌅"
        Prayer.DHUHR -> "☀️"
        Prayer.ASR -> "🌤️"
        Prayer.MAGHRIB -> "🌇"
        Prayer.ISHA -> "🌙"
    }

/** The accent a state is drawn in: its ring, its mark and its label. */
@Composable
internal fun prayerStateColor(state: PrayerButtonState): Color {
    val colors = PixelTheme.colors
    return when (state) {
        PrayerButtonState.PENDING -> colors.primary
        PrayerButtonState.PRAYED -> colors.tertiary
        PrayerButtonState.MISSED -> colors.error
        PrayerButtonState.LOCKED -> colors.onSurfaceVariant
    }
}

/**
 * One prayer's button: its picture in a ring that shows its state, its name and time, and what
 * the state means. A whole-width row, easy to read and to hit. A locked one is dimmed, and
 * tapping it says when it starts ("Starts at 17:29") instead of marking it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerButton(
    prayer: Prayer,
    time: String,
    state: PrayerButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    if (state != PrayerButtonState.LOCKED) {
        PrayerButtonBody(prayer, time, state, onClick, modifier, onLongClick)
        return
    }
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(modifier = Modifier.testTag(NamazTestTags.LOCKED_TOOLTIP)) {
                Text(stringResource(R.string.namaz_starts_at, time))
            }
        },
        state = tooltipState
    ) {
        PrayerButtonBody(prayer, time, state, onClick = { scope.launch { tooltipState.show() } }, modifier = modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrayerButtonBody(
    prayer: Prayer,
    time: String,
    state: PrayerButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val colors = PixelTheme.colors
    val accent = prayerStateColor(state)
    PixelCard(
        variant = PixelPanelVariant.BORDER,
        contentPadding = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .alpha(if (state == PrayerButtonState.LOCKED) 0.6f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrayerStateRing(icon = prayer.icon, state = state, accent = accent)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(prayer.displayName),
                    style = MaterialTheme.typography.titleMedium,
                    color = inkOnPanel(colors.primaryText)
                )
                Text(text = time, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(state.label),
                style = MaterialTheme.typography.labelMedium,
                color = if (state == PrayerButtonState.PENDING) inkOnPanel(colors.primaryText) else accent
            )
        }
    }
}

/** The prayer's picture in a ring: outlined while pending, filled once prayed, with the state's mark. */
@Composable
private fun PrayerStateRing(icon: String, state: PrayerButtonState, accent: Color) {
    Box(modifier = Modifier.size(48.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            if (state == PrayerButtonState.PRAYED) drawCircle(color = accent.copy(alpha = 0.25f))
            drawCircle(color = accent, radius = (size.minDimension - stroke) / 2, style = Stroke(width = stroke))
        }
        Text(text = icon, fontSize = 20.sp)
        state.mark?.let { mark ->
            Text(
                text = mark,
                fontSize = 12.sp,
                color = accent,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
