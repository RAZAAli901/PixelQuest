package com.pixelquest.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.ui.motion.FloatingReward
import com.pixelquest.app.ui.motion.PqMotion
import com.pixelquest.app.ui.motion.QuestTick
import com.pixelquest.app.ui.motion.questRowDone
import com.pixelquest.app.ui.motion.reducedMotion
import com.pixelquest.app.ui.motion.strikeProgress
import com.pixelquest.app.ui.motion.strikeThrough
import com.pixelquest.app.ui.theme.*
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

// ---------- Surfaces ----------

/** Ink border + hard offset shadow. Leave ~[shadow] of room right/bottom so the shadow isn't clipped. */
fun Modifier.comic(
    bg: Color = PqColors.Paper,
    radius: Dp = PqDimens.Radius,
    border: Dp = PqDimens.Border,
    shadow: Dp = PqDimens.Shadow,
    shadowColor: Color = PqColors.Ink,
    tappable: Boolean = false,
): Modifier {
    val spec = PqSkinState.spec
    // Pixel skin: the step shadow is reserved for tappable things; it drops straight down.
    val sh = if (spec.shadowOnlyTappable && !tappable) 0.dp else shadow
    val r = radius * spec.radiusScale
    val shape = pqShape(radius)
    return this
        .drawBehind {
            if (sh > 0.dp) {
                val o = sh.toPx()
                drawRoundRect(shadowColor, topLeft = Offset(o * spec.shadowX, o), size = size, cornerRadius = CornerRadius(r.toPx()))
            }
        }
        .clip(shape)
        .background(bg)
        .border(border, PqColors.Ink, shape)
}

/** Comic surface that sinks into its shadow while pressed. */
@Composable
fun Modifier.comicPressable(
    onClick: () -> Unit,
    bg: Color = PqColors.Paper,
    radius: Dp = PqDimens.Radius,
    shadow: Dp = PqDimens.Shadow,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    // M04: down with the press, and a spring back on release (steps in Pixel).
    val reduced = reducedMotion()
    val shift by animateDpAsState(
        if (pressed) shadow - 1.dp else 0.dp,
        when {
            reduced -> snap()
            pressed -> tween(PqMotion.Fast, easing = PqMotion.out())
            else -> tween(PqMotion.Base, easing = PqMotion.snap())
        },
        label = "press",
    )
    return this
        .offset(shift * PqSkinState.spec.shadowX, shift)
        .comic(bg = bg, radius = radius, shadow = (shadow - shift).coerceAtLeast(1.dp), tappable = true)
        .clickable(
            interactionSource = src,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
}

@Composable
fun PqCard(
    modifier: Modifier = Modifier,
    bg: Color = PqColors.Paper,
    shadow: Dp = PqDimens.Shadow,
    radius: Dp = PqDimens.RadiusLg,
    onClick: (() -> Unit)? = null,
    /** What TalkBack says a tap does ("open"), for a card that opens something. */
    onClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val m = if (onClick !=
        null
    ) {
        modifier.comicPressable(onClick, bg, radius, shadow, onClickLabel = onClickLabel, role = Role.Button)
    } else {
        modifier.comic(bg, radius, shadow = shadow)
    }
    Box(m) { content() }
}

// ---------- Buttons ----------

enum class PqButtonStyle { Primary, Secondary }

@Composable
fun PqButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PqButtonStyle = PqButtonStyle.Primary,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = PqDimens.TouchMin,
) {
    val bg = if (style == PqButtonStyle.Primary) PqColors.Red else PqColors.Paper
    val fg = if (style == PqButtonStyle.Primary) PqColors.OnRed else PqColors.Ink
    Row(
        modifier
            .heightIn(min = height)
            .alpha(if (enabled) 1f else 0.5f)
            .comicPressable(onClick, bg = bg, radius = PqDimens.Gap, enabled = enabled)
            .padding(horizontal = PqDimens.ScreenPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (trailingIcon != null) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(PqDimens.IconMd))
            Spacer(Modifier.width(6.dp))
        }
        Text(text.uppercase(), style = PqType.Button.copy(color = fg))
        if (trailingIcon != null) Icon(trailingIcon, null, tint = fg, modifier = Modifier.size(PqDimens.IconMd))
    }
}

@Composable
fun PqQuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = PqDimens.TouchMin)
            .clip(pqShape(PqDimens.SpaceSm))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = PqDimens.SpaceSm, vertical = 6.dp),
    ) {
        Text(
            text,
            style = PqType.MetaStrong.copy(color = PqColors.RedText, fontSize = 13.bsp),
        )
    }
}

/**
 * Something that can't be undone (leave, delete, reset): drawn in the primary text colour as an
 * outline, never filled, so it never looks like the screen's one red action.
 */
@Composable
fun PqDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Rounded.Delete,
) {
    val shape = pqShape(PqDimens.Gap)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .heightIn(min = PqDimens.TouchMin)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .border(PqDimens.Border, PqColors.RedText, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = PqDimens.Radius),
    ) {
        Icon(icon, contentDescription = null, tint = PqColors.RedText, modifier = Modifier.size(20.dp))
        Text(text = text.uppercase(), style = PqType.Button.copy(color = PqColors.RedText))
    }
}

/** Back, as every v2 top bar has it: an inked square with an arrow. */
@Composable
fun PqBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) =
    PqIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.navigate_back), onClick, modifier)

@Composable
fun PqIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .sizeIn(minWidth = PqDimens.TouchMin, minHeight = PqDimens.TouchMin)
            .size(44.dp)
            .comicPressable(onClick, radius = PqDimens.Gap, shadow = 2.dp, onClickLabel = contentDescription, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = PqColors.Ink, modifier = Modifier.size(PqDimens.IconMd)) }
}

// ---------- Chips & tags ----------

@Composable
fun PqChip(text: String, bg: Color, modifier: Modifier = Modifier, icon: ImageVector? = null, iconTint: Color = PqColors.Ink) {
    Row(
        modifier.height(30.dp).clip(PqCircle).background(bg).border(PqDimens.BorderThin, PqColors.Ink, PqCircle)
            .padding(start = if (icon != null) 6.dp else 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, style = PqType.Button)
    }
}

@Composable
fun PqTag(text: String, color: Color = PqColors.RedText) {
    Text(
        text.uppercase(),
        style = PqType.Label.copy(fontSize = 10.lsp, letterSpacing = 0.6.sp, color = color),
        modifier = Modifier.border(1.5.dp, color, pqShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

// ---------- Segmented ----------

@Composable
fun PqSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(pqShape(12.dp)).background(PqColors.Paper)
            .border(PqDimens.Border, PqColors.Ink, pqShape(12.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(pqShape(9.dp))
                    .background(if (on) PqColors.Ink else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(label, style = PqType.Button.copy(letterSpacing = 0.sp, color = if (on) PqColors.Paper else PqColors.Ink)) }
        }
    }
}

// ---------- Progress ----------

/** Continuous bar with an optional ink target marker (e.g. the 70% difficulty threshold). */
@Composable
fun PqProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    target: Float? = null,
    fill: Color = PqColors.Progress,
    height: Dp = 14.dp,
) {
    // M01/M07: the fill sweeps to its value from the last one shown.
    val reduced = reducedMotion()
    val shown by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        if (reduced) snap() else tween(600, easing = PqMotion.snap()),
        label = "bar",
    )
    Box(modifier.fillMaxWidth().height(height + 12.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier.fillMaxWidth().height(height).clip(pqShape(height / 2)).background(PqColors.Sunken)
                .border(PqDimens.BorderThin, PqColors.Ink, pqShape(height / 2)),
        ) {
            Box(Modifier.fillMaxWidth(shown.coerceIn(0f, 1f)).height(height).background(fill))
            Box(Modifier.fillMaxSize().border(PqDimens.BorderThin, PqColors.Ink, pqShape(height / 2)))
        }
        if (target != null) {
            Canvas(Modifier.fillMaxWidth().height(height + 12.dp)) {
                val x = size.width * target.coerceIn(0f, 1f)
                drawLine(PqColors.Ink, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3.dp.toPx())
            }
        }
    }
}

/** Pixel-style bar: one block per unit (prayers, perfect days). */
@Composable
fun PqSegmentBar(total: Int, filled: Int, modifier: Modifier = Modifier, fill: Color = PqColors.Gold, height: Dp = 12.dp) {
    val reduced = reducedMotion()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            // A block that fills pops (M11's pip pop).
            val on = i < filled
            val pop = remember { Animatable(1f) }
            var was by remember { mutableStateOf(on) }
            LaunchedEffect(on) {
                if (on && !was && !reduced) {
                    pop.animateTo(
                        1f,
                        keyframes {
                            durationMillis = PqMotion.Base
                            1.25f at 140
                        },
                    )
                }
                was = on
            }
            Box(
                Modifier.weight(1f).height(height).graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }.clip(pqShape(3.dp))
                    .background(if (on) fill else PqColors.Sunken)
                    .border(PqDimens.BorderThin, PqColors.Ink, pqShape(3.dp)),
            )
        }
    }
}

@Composable
fun ProgressRing(progress: Float, label: String, diameter: Dp = 78.dp) {
    // M07: the ring sweeps to its value; in Pixel the meter fills in ten steps.
    val reduced = reducedMotion()
    val target = PqMotion.quantizeProgress(progress.coerceIn(0f, 1f))
    val swept by animateFloatAsState(
        target,
        if (reduced) snap() else tween(700, easing = PqMotion.progressEasing()),
        label = "ring",
    )
    if (isPixelSkin) {
        // Pixel skin: a square "power meter" that fills from the bottom
        Box(
            Modifier.size(diameter).background(PqColors.Sunken).border(PqDimens.Border, PqColors.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(swept).background(PqColors.Progress))
            Text(label, style = PqType.number(22))
        }
        return
    }
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val b = PqDimens.Border.toPx()
            drawCircle(PqColors.Sunken)
            drawArc(PqColors.Progress, -90f, 360f * swept, useCenter = true)
            drawCircle(PqColors.Ink, radius = size.minDimension / 2 - b / 2, style = Stroke(b))
            val inner = size.minDimension * 0.35f
            drawCircle(PqColors.Paper, radius = inner)
            drawCircle(PqColors.Ink, radius = inner, style = Stroke(b))
        }
        Text(label, style = PqType.number(22))
    }
}

// ---------- Structure ----------

@Composable
fun PqTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth().heightIn(min = PqDimens.TopBarHeight), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            PqIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.navigate_back), onBack)
            Spacer(Modifier.width(PqDimens.Gap))
        }
        Column(Modifier.weight(1f)) {
            val style = when {
                subtitle != null -> PqType.CardTitle
                onBack != null -> PqType.Title
                else -> PqType.Display
            }
            Text(
                title.uppercase(),
                style = style.copy(color = PqColors.Title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics {
                    heading()
                },
            )
            if (subtitle != null) Text(subtitle, style = PqType.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PqDimens.SpaceSm), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun PqSectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = PqType.Label.copy(fontSize = 13.lsp), modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null) PqQuietButton(action, onAction)
    }
}

@Composable
fun PqGroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = PqType.Label.copy(fontSize = 12.lsp, color = PqColors.Muted), modifier = modifier.semantics { heading() }.padding(top = 4.dp))
}

@Composable
fun CategoryTile(icon: ImageVector, bg: Color, size: Dp = 44.dp, iconSize: Dp = 24.dp, radius: Dp = 11.dp) {
    Box(
        Modifier.size(size).clip(pqShape(radius)).background(bg).border(PqDimens.BorderThin, PqColors.Ink, pqShape(radius)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = PqColors.Ink, modifier = Modifier.size(iconSize)) }
}

/** Tap target that completes a quest. Empty ring → filled ink check. */
@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit, size: Dp = 40.dp, modifier: Modifier = Modifier, label: String? = null) {
    Box(
        // A checkbox to TalkBack: its row's title says what it's for, or [label] does.
        modifier.size(PqDimens.TouchMin.coerceAtLeast(size)).clip(PqCircle)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        // M03: the circle fills with a pop, then its tick draws.
        val reduced = reducedMotion()
        val fill = remember { Animatable(if (checked) 1f else 0f) }
        val draw = remember { Animatable(if (checked) 1f else 0f) }
        LaunchedEffect(checked) {
            when {
                reduced -> {
                    fill.snapTo(if (checked) 1f else 0f)
                    draw.snapTo(if (checked) 1f else 0f)
                }
                checked && fill.value < 1f -> {
                    fill.animateTo(
                        1f,
                        keyframes {
                            durationMillis = PqMotion.Base
                            1.15f at 160
                        },
                    )
                    draw.animateTo(1f, tween(200, easing = PqMotion.out()))
                }
                !checked -> {
                    draw.snapTo(0f)
                    fill.animateTo(0f, tween(PqMotion.Fast))
                }
            }
        }
        Box(
            Modifier.size(size).clip(PqCircle).background(PqColors.Paper)
                .border(PqDimens.Border, PqColors.Ink, PqCircle),
            contentAlignment = Alignment.Center,
        ) { QuestTick(fill.value, draw.value, dot = if (PqMotion.pixel) PqColors.Progress else PqColors.Ink, tick = PqColors.Paper) }
    }
}

@Composable
fun QuestRow(quest: QuestUi, onToggle: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val base = if (quest.done) {
        modifier.comicPressable(
            onClick = onClick,
            bg = PqColors.Sunken,
            radius = PqDimens.Radius,
            shadow = 0.dp,
        )
    } else {
        modifier.comicPressable(onClick, radius = PqDimens.Radius)
    }
    // M03: done just now, the row squashes, the title's strike slides across, and its XP floats up.
    var rewards by remember(quest.id) { mutableIntStateOf(0) }
    var wasDone by remember(quest.id) { mutableStateOf(quest.done) }
    LaunchedEffect(quest.done, quest.xp) {
        if (quest.done && !wasDone && quest.xp > 0) rewards++
        wasDone = quest.done
    }
    Row(
        base.questRowDone(quest.done, dim = false).fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryTile(quest.category.icon, quest.category.color)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                quest.title,
                style = PqType.BodyStrong.copy(color = if (quest.done) PqColors.Muted else PqColors.Ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.strikeThrough(strikeProgress(quest.done), PqColors.Muted),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (quest.missed) {
                    PqTag(stringResource(R.string.pq_missed), color = PqColors.Muted)
                } else if (quest.overdue && !quest.done) {
                    PqTag(stringResource(R.string.pq_overdue))
                }
                Text(
                    listOfNotNull(quest.time, stringResource(quest.category.label), quest.repeatLabel).joinToString(" · "),
                    style = PqType.Meta,
                    maxLines = 1,
                )
            }
        }
        Box(contentAlignment = Alignment.TopCenter) {
            CheckCircle(quest.done, onToggle)
            if (quest.xp > 0) FloatingReward(stringResource(R.string.motion_xp_gain, quest.xp), rewards)
        }
    }
}

/** Compact done row used in the Perfect Day list. */
@Composable
fun QuestRowCompact(quest: QuestUi, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(pqShape(12.dp)).background(PqColors.Sunken)
            .border(PqDimens.BorderThin, PqColors.Ink, pqShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryTile(quest.category.icon, quest.category.color, size = 32.dp, iconSize = 18.dp, radius = 8.dp)
        Text(
            quest.title,
            style = PqType.Body.copy(fontSize = 14.bsp, color = PqColors.Muted, textDecoration = TextDecoration.LineThrough),
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (quest.xp > 0) Text(stringResource(R.string.pq_xp_plus, quest.xp), style = PqType.MetaStrong.copy(color = PqColors.Muted))
        Box(Modifier.size(28.dp).clip(PqCircle).background(PqColors.Ink), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = PqColors.Paper, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PqListRow(icon: ImageVector, label: String, onClick: () -> Unit, value: String? = null, showDivider: Boolean = true) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = PqColors.Ink, modifier = Modifier.size(22.dp))
            Text(
                label,
                style = PqType.Body.copy(fontSize = 15.bsp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            if (value != null) Text(value, style = PqType.Meta.copy(fontSize = 13.bsp))
            Icon(Icons.Rounded.ChevronRight, null, tint = PqColors.Muted, modifier = Modifier.size(22.dp))
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
    }
}

/** Dashed outline box (upcoming prayer pip). */
fun Modifier.dashedBorder(color: Color = PqColors.Ink, width: Dp = 2.dp, radius: Dp = 9.dp) = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(w / 2, w / 2),
        size = size.copy(size.width - w, size.height - w),
        cornerRadius = CornerRadius((radius * PqSkinState.spec.radiusScale).toPx()),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}

/**
 * One row of a ruled card (mock 2F's surah list): rows that read as one bordered card, divided by
 * thin rules, for a LazyColumn whose rows can't share a container. Rows sit with no gap between.
 */
@Composable
fun Modifier.pqRuledRow(first: Boolean, last: Boolean): Modifier {
    val radius = PqDimens.Radius
    val border = PqDimens.Border
    val shape = when {
        first && last -> RoundedCornerShape(radius)
        first -> RoundedCornerShape(topStart = radius, topEnd = radius)
        last -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RectangleShape
    }
    val ink = PqColors.Ink
    val rule = PqColors.Divider
    return this
        .clip(shape)
        .background(PqColors.Paper)
        .drawWithContent {
            drawContent()
            val w = border.toPx()
            val r = radius.toPx()
            // The whole card's outline, reaching past this row where it goes on; the clip keeps this row's part.
            val top = if (first) w / 2 else -(2 * r + w)
            val bottom = if (last) size.height - w / 2 else size.height + 2 * r + w
            drawRoundRect(
                ink,
                topLeft = Offset(w / 2, top),
                size = Size(size.width - w, bottom - top),
                cornerRadius = CornerRadius(r),
                style = Stroke(w),
            )
            if (!last) {
                val y = size.height - 1.dp.toPx()
                drawLine(rule, Offset(w, y), Offset(size.width - w, y), strokeWidth = 2.dp.toPx())
            }
        }
}
