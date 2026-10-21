package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.ui.chat.bubbleTime
import com.pixelquest.app.ui.chat.dayDate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * A finished party's card, where the number field was: the milestone reached, everyone's total
 * pooled, and when it was reached, for good (a finish is final). It springs in once, unless motion is
 * reduced.
 *
 * @param posters how many people added to it.
 * @param startedAt when the party began, to say how long it took.
 */
@Composable
fun CelebrationCard(
    total: Long,
    milestone: Long?,
    completedAt: Instant,
    startedAt: Instant?,
    posters: Int,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    goalNumber: Int? = null,
    isLeader: Boolean = false,
    onExtend: () -> Unit = {}
) {
    val still = LocalReduceMotion.current
    val grow = remember { Animatable(if (still) 1f else 0.8f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }

    val day = completedAt.atZone(zone).toLocalDate()
    val daysTaken = startedAt?.let { ChronoUnit.DAYS.between(it.atZone(zone).toLocalDate(), day) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .scale(grow.value)
            .testTag(DaroodTestTags.CHAT_CELEBRATION)
            .comic(bg = PqColors.Gold, radius = PqDimens.RadiusLg, shadow = PqDimens.Shadow)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = goalNumber?.let { stringResource(R.string.darood_goal_reached_title, it) } ?: stringResource(R.string.darood_done_title),
            style = PqType.CardTitle,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = if (milestone != null) {
                pluralStringResource(R.plurals.darood_done_pooled, posters, posters, formatCount(total), formatCount(milestone))
            } else {
                pluralStringResource(R.plurals.darood_done_pooled_no_aim, posters, posters, formatCount(total))
            },
            style = PqType.Body,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.darood_done_when, dayDate(day), bubbleTime(completedAt, zone)) + " " + when (daysTaken) {
                null -> ""
                0L -> stringResource(R.string.darood_done_same_day)
                else -> pluralStringResource(R.plurals.darood_done_days, daysTaken.toInt(), daysTaken.toInt())
            },
            style = PqType.Meta.copy(color = PqColors.Ink2),
            textAlign = TextAlign.Center
        )
        // Goals that go on: the leader sets the next one; everyone else hears when it's set.
        if (isLeader) {
            com.pixelquest.app.ui.components.PqButton(
                text = stringResource(R.string.darood_goal_extend),
                onClick = onExtend,
                modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.CHAT_EXTEND_GOAL)
            )
        } else {
            Text(
                text = stringResource(R.string.darood_goal_waiting),
                style = PqType.Meta,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(DaroodTestTags.CHAT_GOAL_WAITING)
            )
        }
    }
}
