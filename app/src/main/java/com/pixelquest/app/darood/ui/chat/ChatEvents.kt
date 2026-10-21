package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.components.comic
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import java.time.Instant

/** Something that happened to a party, told in its chat between the entries. */
sealed interface PartyEvent {
    val at: Instant

    /** Stable, for the chat's list. */
    val key: String

    data class Joined(val userId: String, val username: String?, override val at: Instant) : PartyEvent {
        override val key: String get() = "joined-$userId-${at.toEpochMilli()}"
    }

    data class Left(val userId: String, val username: String?, override val at: Instant) : PartyEvent {
        override val key: String get() = "left-$userId-${at.toEpochMilli()}"
    }

    data class Removed(val userId: String, val username: String?, override val at: Instant) : PartyEvent {
        override val key: String get() = "removed-$userId-${at.toEpochMilli()}"
    }

    data class MilestoneSet(val target: Long, override val at: Instant) : PartyEvent {
        override val key: String get() = "milestone-$target-${at.toEpochMilli()}"
    }

    data class MilestoneReached(val target: Long?, override val at: Instant) : PartyEvent {
        override val key: String get() = "reached-${at.toEpochMilli()}"
    }

    /** A week closed (docs/darood-weekly.md): its ranking, as a card from the party itself. */
    data class WeekClosed(
        val week: com.pixelquest.app.darood.domain.WeekResult,
        val names: Map<String, String?>,
        val me: String?
    ) : PartyEvent {
        override val at: Instant get() = week.closedAt
        override val key: String get() = "week-${week.weekStart}"
    }
}

/**
 * What the cache knows happened to [party], oldest first: who joined (even if they've since gone),
 * who left or was removed, the milestone as it now stands, and its reaching. A milestone raised
 * twice shows once, at its latest, since only that is kept.
 */
fun partyEventsOf(
    party: Party?,
    members: List<PartyMember>,
    weeks: List<com.pixelquest.app.darood.domain.WeekResult> = emptyList(),
    me: String? = null
): List<PartyEvent> {
    val events = mutableListOf<PartyEvent>()
    val names = members.associate { it.userId to it.username }
    weeks.forEach { events += PartyEvent.WeekClosed(it, names, me) }
    for (member in members) {
        member.joinedAt?.let { events += PartyEvent.Joined(member.userId, member.username, it) }
        val changed = member.statusChangedAt ?: continue
        when (member.status) {
            MemberStatus.LEFT -> events += PartyEvent.Left(member.userId, member.username, changed)
            MemberStatus.REMOVED -> events += PartyEvent.Removed(member.userId, member.username, changed)
            else -> Unit
        }
    }
    if (party != null) {
        val milestone = party.milestone
        val setAt = party.milestoneSetAt
        if (milestone != null && setAt != null) events += PartyEvent.MilestoneSet(milestone, setAt)
        party.completedAt?.let { events += PartyEvent.MilestoneReached(milestone, it) }
    }
    return events.sortedBy { it.at }
}

/**
 * An event in the chat (mock 4E): who joined or left, as a quiet centred line; a milestone set or
 * reached, on gold with a flag.
 */
@Composable
fun EventPill(event: PartyEvent, modifier: Modifier = Modifier) {
    if (event is PartyEvent.WeekClosed) {
        WeekCard(event, modifier)
        return
    }
    val text = eventText(event)
    if (event is PartyEvent.MilestoneSet || event is PartyEvent.MilestoneReached) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag(DaroodTestTags.event(event.key))
                .semantics(mergeDescendants = true) { }
                .comic(bg = PqColors.Gold, radius = PqDimens.Radius, shadow = 0.dp)
                .padding(12.dp)
        ) {
            Icon(Icons.Rounded.Flag, contentDescription = null, tint = PqColors.Ink)
            Text(text = text, style = PqType.BodyStrong)
        }
    } else {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = text,
                style = PqType.Meta.copy(color = PqColors.Ink2),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(DaroodTestTags.event(event.key))
            )
        }
    }
}

@Composable
private fun eventText(event: PartyEvent): String {
    @Composable
    fun name(username: String?) = username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)
    return when (event) {
        is PartyEvent.Joined -> stringResource(R.string.darood_event_joined, name(event.username))
        is PartyEvent.Left -> stringResource(R.string.darood_event_left, name(event.username))
        is PartyEvent.Removed -> stringResource(R.string.darood_event_removed, name(event.username))
        is PartyEvent.MilestoneSet -> stringResource(R.string.darood_event_milestone_set, formatCount(event.target))
        is PartyEvent.MilestoneReached -> event.target?.let { stringResource(R.string.darood_event_milestone_reached, formatCount(it)) }
            ?: stringResource(R.string.darood_event_finished)
        is PartyEvent.WeekClosed -> stringResource(R.string.darood_week_title)
    }
}

/** The week's card in the chat (J078): its dates, the player's place, and the top three. */
@Composable
private fun WeekCard(event: PartyEvent.WeekClosed, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val week = event.week
    val dates = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
    androidx.compose.foundation.layout.Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(DaroodTestTags.event(event.key))
            .semantics(mergeDescendants = true) { }
            .comic(bg = PqColors.Paper, radius = PqDimens.Radius, shadow = 0.dp)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = PqColors.Ink)
            Text(text = stringResource(R.string.darood_week_title), style = PqType.BodyStrong)
        }
        Text(
            text = stringResource(R.string.darood_week_dates, week.weekStart.format(dates), week.weekStart.plusDays(6).format(dates)),
            style = PqType.Meta
        )
        event.me?.let { me ->
            com.pixelquest.app.darood.sync.WeekRankingText.line(context, week, me)?.let { Text(text = it, style = PqType.Body) }
            com.pixelquest.app.darood.sync.WeekRankingText.tone(context, week, me)?.let { Text(text = it, style = PqType.Meta) }
        }
        week.places.take(3).forEach { place ->
            val name = if (place.userId == event.me) stringResource(R.string.darood_week_you) else event.names[place.userId] ?: stringResource(R.string.darood_someone)
            Text(
                text = stringResource(
                    R.string.darood_week_notif_row,
                    com.pixelquest.app.darood.sync.WeekRankingText.ordinal(context, place.position), name, formatCount(place.weekTotal)
                ),
                style = PqType.Body
            )
        }
    }
}
