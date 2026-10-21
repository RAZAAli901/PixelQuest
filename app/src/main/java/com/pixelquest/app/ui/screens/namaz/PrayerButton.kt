package com.pixelquest.app.ui.screens.namaz

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerSlot
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.components.prayerColor
import com.pixelquest.app.ui.components.prayerIcon
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.lsp
import com.pixelquest.app.ui.theme.pqShape
import com.pixelquest.app.ui.theme.rememberReduceMotion
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

/**
 * One prayer's row (UI v2, mock 2E): its icon in its colour, its name and time, and on the right
 * what can be done: Mark, Make up (missed, prayed late), or the inked Prayed. The next prayer stands
 * out, with how long until it. A locked one is dimmed, and tapping it says when it starts ("Starts
 * at 17:29") instead of marking it. [readOnly] (an earlier day) shows the same row with nothing to tap.
 *
 * TalkBack reads it as one item, "Fajr, 5:12 AM, not prayed", and says what a double tap does.
 *
 * @param isNext the prayer coming up next, drawn with a red shadow and [countdown].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerButton(
    prayer: Prayer,
    time: String,
    state: PrayerButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    readOnly: Boolean = false,
    isNext: Boolean = false,
    countdown: String? = null
) {
    if (state != PrayerButtonState.LOCKED || readOnly) {
        PrayerButtonBody(prayer, time, state, onClick, modifier, onLongClick, readOnly, isNext, countdown)
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
        PrayerButtonBody(prayer, time, state, onClick = { scope.launch { tooltipState.show() } }, modifier = modifier, isNext = isNext, countdown = countdown)
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
    onLongClick: (() -> Unit)? = null,
    readOnly: Boolean = false,
    isNext: Boolean = false,
    countdown: String? = null
) {
    val description = stringResource(
        R.string.namaz_a11y_prayer,
        stringResource(prayer.displayName),
        time,
        when (state) {
            PrayerButtonState.PENDING -> stringResource(R.string.namaz_a11y_not_prayed)
            PrayerButtonState.PRAYED -> stringResource(R.string.namaz_a11y_prayed)
            PrayerButtonState.MISSED -> stringResource(R.string.namaz_a11y_missed)
            PrayerButtonState.LOCKED -> stringResource(R.string.namaz_a11y_locked, time)
        }
    )
    // TalkBack says "double tap to ..." with these.
    val clickLabel = stringResource(
        when (state) {
            PrayerButtonState.PENDING -> R.string.namaz_a11y_mark
            PrayerButtonState.MISSED -> R.string.namaz_a11y_mark_late
            PrayerButtonState.PRAYED -> R.string.namaz_a11y_undo
            PrayerButtonState.LOCKED -> R.string.namaz_a11y_when
        }
    )
    val longClickLabel = onLongClick?.let { stringResource(R.string.namaz_a11y_undo) }
    val shape = pqShape(PqDimens.Radius)
    val frame = if (isNext && !readOnly) {
        Modifier.comic(radius = PqDimens.Radius, shadowColor = PqColors.Red, tappable = true)
    } else {
        Modifier.clip(shape).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, shape)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(frame)
            .then(
                if (readOnly) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        role = Role.Button,
                        onClickLabel = clickLabel,
                        onClick = onClick,
                        onLongClickLabel = longClickLabel,
                        onLongClick = onLongClick
                    )
                }
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = if (isNext) 10.dp else 8.dp)
            // A prayer still to come is dimmed, except the next one, which stands out.
            .alpha(if (state == PrayerButtonState.LOCKED && !isNext) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val key = prayer.name.lowercase()
        Box(
            Modifier.prayedPop(state).size(40.dp).clip(PqCircle).background(prayerColor(key)).border(PqDimens.BorderThin, PqColors.Ink, PqCircle).clearAndSetSemantics {},
            contentAlignment = Alignment.Center
        ) { Icon(prayerIcon(key), null, tint = PqColors.Ink, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text(stringResource(prayer.displayName), style = PqType.BodyStrong)
            Text(
                if (isNext && countdown != null) "$time · $countdown" else time,
                style = if (isNext) PqType.MetaStrong.copy(color = PqColors.RedText) else PqType.Meta
            )
        }
        // What a tap does, drawn: the row itself is the button. Its words are the row's state, for
        // tests; TalkBack reads the row's description.
        Box {
            when (state) {
                PrayerButtonState.PRAYED -> Row(
                    Modifier.height(30.dp).clip(PqCircle).background(PqColors.Ink).padding(start = 6.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Check, null, tint = PqColors.Paper, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(stringResource(R.string.namaz_status_prayed), style = PqType.Button.copy(fontSize = 12.lsp, color = PqColors.Paper, letterSpacing = 0.sp))
                }
                PrayerButtonState.MISSED -> if (readOnly) StatusText(R.string.namaz_status_missed) else ActionPill(R.string.namaz_make_up)
                PrayerButtonState.PENDING -> if (readOnly) StatusText(R.string.namaz_status_not_marked) else ActionPill(R.string.namaz_mark)
                PrayerButtonState.LOCKED -> StatusText(R.string.namaz_status_locked)
            }
        }
    }
}

/** A pill like a secondary button: what tapping the row does. */
@Composable
private fun ActionPill(label: Int) {
    Box(
        Modifier.height(34.dp).comic(radius = 12.dp, shadow = 2.dp).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(stringResource(label).uppercase(), style = PqType.Button) }
}

@Composable
private fun StatusText(label: Int) {
    Text(stringResource(label), style = PqType.MetaStrong.copy(color = PqColors.Muted))
}

/**
 * A short pop when a prayer turns prayed, so the tap is felt as done. Not when the screen opens on
 * one already prayed, nor with reduced motion (the app's setting or Android's).
 */
@Composable
private fun Modifier.prayedPop(state: PrayerButtonState): Modifier {
    val reduceMotion = rememberReduceMotion(LocalReduceMotion.current)
    val scale = remember { Animatable(1f) }
    val wasPrayed = remember { booleanArrayOf(state == PrayerButtonState.PRAYED) }
    LaunchedEffect(state) {
        val nowPrayed = state == PrayerButtonState.PRAYED
        if (nowPrayed && !wasPrayed[0] && !reduceMotion) {
            scale.animateTo(1.25f, tween(durationMillis = 90))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        wasPrayed[0] = nowPrayed
    }
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}
