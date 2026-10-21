package com.pixelquest.app.party.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqChip
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.pqShape
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// A party's members, as every kind of party lists them (docs/party-core.md).

/**
 * A party's members in one card (mock 4G), the leader first: each with the day they joined, the
 * leader marked. For the leader, a way to hand the lead to each other member, or remove them, never
 * themselves.
 */
@Composable
fun MemberList(
    members: List<PartyMember>,
    myId: String?,
    isLeader: Boolean,
    enabled: Boolean,
    onRemove: (PartyMember) -> Unit,
    onTransfer: (PartyMember) -> Unit,
    /** Something beside a member's name (a study party's chess badge). */
    badge: @Composable (PartyMember) -> Unit = {}
) {
    if (members.isEmpty()) return
    PqCard(radius = PqDimens.Radius, shadow = 0.dp) {
        Column {
            members.forEachIndexed { i, member ->
                MemberRow(
                    member = member,
                    isMe = member.userId == myId,
                    onRemove = if (isLeader && member.role != PartyRole.LEADER) ({ onRemove(member) }) else null,
                    onTransfer = if (isLeader && member.role != PartyRole.LEADER) ({ onTransfer(member) }) else null,
                    enabled = enabled,
                    badge = { badge(member) }
                )
                if (i != members.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
            }
        }
    }
}

/** How a member is named: their @username, or "someone" where it isn't theirs to see. */
@Composable
fun displayName(member: PartyMember): String =
    member.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)

/** One member: their picture, username and the day they joined, the leader marked; for the leader, a way to hand over or remove. */
@Composable
fun MemberRow(member: PartyMember, isMe: Boolean, onRemove: (() -> Unit)?, onTransfer: (() -> Unit)?, enabled: Boolean, badge: @Composable () -> Unit = {}) {
    val name = displayName(member)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DaroodTestTags.member(member.userId))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            InitialAvatar(name)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (isMe) stringResource(R.string.darood_info_you, name) else name, style = PqType.BodyStrong)
                member.joinedAt?.let { joined ->
                    Text(text = stringResource(R.string.darood_info_joined, joinedDate(joined)), style = PqType.Meta)
                }
            }
            badge()
            if (member.role == PartyRole.LEADER) PqChip(stringResource(R.string.darood_badge_leader).uppercase(), PqColors.Gold)
        }
        // The leader's tools for this member, on their own line so the name keeps its room.
        if (onTransfer != null || onRemove != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                if (onTransfer != null) {
                    MemberAction(
                        text = stringResource(R.string.darood_info_make_leader),
                        said = stringResource(R.string.darood_info_make_leader_label, name),
                        enabled = enabled,
                        onClick = onTransfer,
                        modifier = Modifier.testTag(DaroodTestTags.memberTransfer(member.userId))
                    )
                }
                if (onRemove != null) {
                    MemberAction(
                        text = stringResource(R.string.darood_info_remove),
                        said = stringResource(R.string.darood_info_remove_label, name),
                        enabled = enabled,
                        onClick = onRemove,
                        modifier = Modifier.testTag(DaroodTestTags.memberRemove(member.userId))
                    )
                }
            }
        }
    }
}

/** One of the leader's tools on a member's line: a quiet action, said with the member's name. */
@Composable
private fun MemberAction(text: String, said: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PqType.MetaStrong.copy(color = PqColors.RedText, fontSize = 13.bsp),
        modifier = modifier
            .semantics { contentDescription = said }
            .clip(pqShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

internal fun joinedDate(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(instant)
