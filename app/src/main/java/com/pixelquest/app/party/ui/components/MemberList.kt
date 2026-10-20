package com.pixelquest.app.party.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// A party's members, as every kind of party lists them (docs/party-core.md).

/**
 * A party's members, the leader first: each with their role and the day they joined. For the
 * leader, a way to hand the lead to each other member, or remove them, never themselves.
 */
@Composable
fun MemberList(
    members: List<PartyMember>,
    myId: String?,
    isLeader: Boolean,
    enabled: Boolean,
    onRemove: (PartyMember) -> Unit,
    onTransfer: (PartyMember) -> Unit
) {
    members.forEach { member ->
        MemberRow(
            member = member,
            isMe = member.userId == myId,
            onRemove = if (isLeader && member.role != PartyRole.LEADER) ({ onRemove(member) }) else null,
            onTransfer = if (isLeader && member.role != PartyRole.LEADER) ({ onTransfer(member) }) else null,
            enabled = enabled
        )
    }
}

/** How a member is named: their @username, or "someone" where it isn't theirs to see. */
@Composable
fun displayName(member: PartyMember): String =
    member.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)

/** One member: their username, their role, the day they joined, and for the leader, a way to hand over or remove. */
@Composable
fun MemberRow(member: PartyMember, isMe: Boolean, onRemove: (() -> Unit)?, onTransfer: (() -> Unit)?, enabled: Boolean) {
    val colors = PixelTheme.colors
    val name = displayName(member)
    PixelCard(
        variant = PixelPanelVariant.BORDER,
        contentPadding = 12.dp,
        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.member(member.userId))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isMe) stringResource(R.string.darood_info_you, name) else name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = inkOnPanel(colors.primaryText)
                    )
                    member.joinedAt?.let { joined ->
                        Text(
                            text = stringResource(R.string.darood_info_joined, joinedDate(joined)),
                            style = MaterialTheme.typography.bodySmall,
                            color = inkOnPanel(colors.onSurfaceVariant)
                        )
                    }
                }
                Text(
                    text = stringResource(if (member.role == PartyRole.LEADER) R.string.darood_badge_leader else R.string.darood_badge_member),
                    style = MaterialTheme.typography.labelMedium,
                    color = inkOnPanel(colors.primaryText)
                )
            }
            // The leader's tools for this member, on their own line so the name keeps its room.
            if (onTransfer != null || onRemove != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                    if (onTransfer != null) {
                        val label = stringResource(R.string.darood_info_make_leader_label, name)
                        PixelButton(
                            text = stringResource(R.string.darood_info_make_leader),
                            onClick = onTransfer,
                            variant = PixelButtonVariant.BLUE,
                            enabled = enabled,
                            modifier = Modifier
                                .semantics { contentDescription = label }
                                .testTag(DaroodTestTags.memberTransfer(member.userId))
                        )
                    }
                    if (onRemove != null) {
                        val label = stringResource(R.string.darood_info_remove_label, name)
                        PixelButton(
                            text = stringResource(R.string.darood_info_remove),
                            onClick = onRemove,
                            variant = PixelButtonVariant.BLUE,
                            enabled = enabled,
                            modifier = Modifier
                                .semantics { contentDescription = label }
                                .testTag(DaroodTestTags.memberRemove(member.userId))
                        )
                    }
                }
            }
        }
    }
}

internal fun joinedDate(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(instant)
