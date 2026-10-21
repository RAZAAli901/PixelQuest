package com.pixelquest.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.*

/** Form + misc building blocks used by Parties, Study party and Coach. All skin-aware. */

@Composable
/** A switch; with no [onCheckedChange] it only shows [checked], for a row that is the toggle itself. */
fun PqSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, label = "knob")
    Box(
        modifier.size(48.dp, 28.dp).clip(PqCircle)
            .background(if (checked) PqColors.Progress else PqColors.Sunken)
            .border(PqDimens.Border, PqColors.Ink, PqCircle)
            .then(if (onCheckedChange != null) Modifier.toggleable(checked, onValueChange = onCheckedChange, role = Role.Switch) else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = x).size(19.dp).clip(PqCircle).background(if (checked) PqColors.Paper else PqColors.Ink))
    }
}

@Composable
fun PqFieldLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = PqType.Label.copy(color = PqColors.Muted), modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = PqType.Meta)
    }
}

@Composable
fun PqTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    height: Dp = 48.dp,
    bg: Color = PqColors.Paper,
    maxLength: Int? = null,
) {
    val shape = pqShape(12.dp)
    Box(
        modifier.fillMaxWidth().height(height).clip(shape).background(bg).border(PqDimens.Border, PqColors.Ink, shape).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(placeholder, style = PqType.Body.copy(color = PqColors.Muted), maxLines = 1)
        BasicTextField(
            value, { v -> onValueChange(if (maxLength != null) v.take(maxLength) else v) },
            singleLine = true, textStyle = PqType.Body, cursorBrush = SolidColor(PqColors.Ink),
            keyboardOptions = keyboardOptions, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PqStepper(value: Int, onValueChange: (Int) -> Unit, range: IntRange, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.alpha(if (value > range.first) 1f else 0.45f)) {
            PqIconButton(Icons.Rounded.Remove, stringResource(R.string.pq_fewer), onClick = { if (value > range.first) onValueChange(value - 1) })
        }
        Text("$value", style = PqType.display(24), textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 28.dp))
        Box(Modifier.alpha(if (value < range.last) 1f else 0.45f)) {
            PqIconButton(Icons.Rounded.Add, stringResource(R.string.pq_more), onClick = { if (value < range.last) onValueChange(value + 1) })
        }
    }
}

/** Single-select chip. Selected = ink fill. */
@Composable
fun PqChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val fg = if (selected) PqColors.Paper else PqColors.Ink
    Row(
        modifier.height(34.dp).clip(PqCircle).background(if (selected) PqColors.Ink else PqColors.Paper)
            .border(PqDimens.BorderThin, PqColors.Ink, PqCircle)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)) }
        Text(label, style = PqType.MetaStrong.copy(color = fg))
    }
}

/** Round initial avatar for party members (square in Pixel). */
@Composable
fun InitialAvatar(name: String, size: Dp = 36.dp, bg: Color = PqColors.Sky) {
    Box(
        Modifier.size(size).clip(PqCircle).background(bg).border(PqDimens.BorderThin, PqColors.Ink, PqCircle),
        contentAlignment = Alignment.Center,
    ) { Text(name.trimStart('@').take(1).uppercase(), style = PqType.Button) }
}

/** Bottom sheet with the comic/pixel surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PqSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = PqColors.Paper, contentColor = PqColors.Ink,
        shape = pqShapeTop(22.dp),
        scrimColor = Color(0x73161412),
        dragHandle = { Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(44.dp, 5.dp).clip(PqCircle).background(PqColors.Ink.copy(alpha = 0.3f))) },
    ) { Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) { content() } }
}

// ---------- Coach Intel ----------

data class CoachUi(
    val live: Boolean,
    val situation: String,       // first paragraph of the brief
    val updatedAt: String,       // "8:48 AM"
    val nextBriefIn: String?,    // "~6h" or null if available now
)

/** Replaces the old Coach Intel card + disabled COOLDOWN button + separate "OPEN AI COACH" button. */
@Composable
fun CoachCard(coach: CoachUi, onOpen: () -> Unit, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    PqCard(modifier.fillMaxWidth(), onClick = onOpen) {
        Column {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(36.dp).clip(pqShape(10.dp)).background(PqColors.Ink), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = PqColors.Gold, modifier = Modifier.size(20.dp))
                    }
                    Text(stringResource(R.string.pq_coach_title), style = PqType.CardTitle, modifier = Modifier.weight(1f))
                    if (coach.live) PqTag(stringResource(R.string.pq_coach_live), color = PqColors.Live)
                }
                Text(stringResource(R.string.pq_coach_situation), style = PqType.Label.copy(color = PqColors.Muted))
                Text(coach.situation, style = PqType.Body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                PqQuietButton(stringResource(R.string.pq_coach_full_brief), onExpand)
            }
            Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    coach.nextBriefIn?.let { stringResource(R.string.pq_coach_next_brief, coach.updatedAt, it) } ?: stringResource(R.string.pq_coach_brief_ready, coach.updatedAt),
                    style = PqType.Meta, modifier = Modifier.weight(1f),
                )
                PqButton(stringResource(R.string.pq_coach_open), onOpen, style = PqButtonStyle.Secondary, trailingIcon = Icons.Rounded.ArrowForward, height = 38.dp)
            }
        }
    }
}

// ---------- Confirm ----------

/**
 * Asks before something that can't simply be taken back (skipping a quest, leaving a party): a
 * card with the question, Cancel, and [confirm] as the dialog's one red button.
 */
@Composable
fun PqConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        PqCard(Modifier.fillMaxWidth(), shadow = PqDimens.ShadowLg, radius = 18.dp) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = PqType.CardTitle)
                Text(message, style = PqType.Body.copy(color = PqColors.Ink2))
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PqButton(stringResource(R.string.pq_cancel), onDismiss, Modifier.weight(1f), style = PqButtonStyle.Secondary)
                    PqButton(confirm, onConfirm, Modifier.weight(1f))
                }
            }
        }
    }
}
