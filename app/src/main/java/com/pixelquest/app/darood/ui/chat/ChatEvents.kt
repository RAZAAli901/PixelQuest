package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.PixelTheme
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
}

/**
 * What the cache knows happened to [party], oldest first: who joined (even if they've since gone),
 * who left or was removed, the milestone as it now stands, and its reaching. A milestone raised
 * twice shows once, at its latest, since only that is kept.
 */
fun partyEventsOf(party: Party?, members: List<PartyMember>): List<PartyEvent> {
    val events = mutableListOf<PartyEvent>()
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

/** An event in the chat: a small centred pill, as a group chat tells who joined. */
@Composable
fun EventPill(event: PartyEvent, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = eventText(event),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .testTag(DaroodTestTags.event(event.key))
                .background(colors.surface, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
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
    }
}
