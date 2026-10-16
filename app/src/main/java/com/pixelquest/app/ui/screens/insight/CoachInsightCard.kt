package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import com.pixelquest.app.ui.theme.LocalReduceMotion
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedVisibility
import com.pixelquest.app.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.ui.components.ComicButton
import com.pixelquest.app.ui.components.ComicButtonVariant
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * The AI Coach's insight card: the latest insight's summary, suggestion and encouragement, and when
 * it was made. Asking for a fresh one is the header's job (CoachRefreshButton). Each theme has its own look ([PixelSuccessCard], [ComicSuccessView],
 * [LightSuccessCard]).
 */
@Composable
fun CoachInsightCard(
    uiState: AiInsightUiState.Success,
    themeMode: ThemeMode,
    isSimpleMode: Boolean = false,
    modifier: Modifier = Modifier,
    /** False shows a short preview: the summary's first lines, without the other two sections. */
    expanded: Boolean = true,
    /** Shows the SHOW MORE / SHOW LESS toggle; null (always in full) hides it. */
    onToggleExpanded: (() -> Unit)? = null
) {
    when (themeMode) {
        ThemeMode.Comic -> ComicSuccessView(uiState = uiState, modifier = modifier, expanded = expanded, onToggleExpanded = onToggleExpanded)
        ThemeMode.Light -> LightSuccessCard(uiState = uiState, modifier = modifier, expanded = expanded, onToggleExpanded = onToggleExpanded)
        else -> PixelSuccessCard(
            uiState = uiState, isSimpleMode = isSimpleMode, modifier = modifier,
            expanded = expanded, onToggleExpanded = onToggleExpanded
        )
    }
}

/**
 * The part of a card that only shows when expanded. It slides open and fades in, unless the player
 * asked for less motion (Reduce Motion, or Android's own setting).
 */
@Composable
fun ColumnScope.CoachExpandable(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = if (reduceMotion) EnterTransition.None else expandVertically() + fadeIn(),
        exit = if (reduceMotion) ExitTransition.None else shrinkVertically() + fadeOut()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** Grows and shrinks smoothly as a preview's lines change, unless the player asked for less motion. */
@Composable
fun Modifier.coachAnimateSize(): Modifier = if (LocalReduceMotion.current) this else this.animateContentSize()

/**
 * SHOW MORE / SHOW LESS under a previewed insight, in the theme's type and colour.
 */
@Composable
fun CoachExpandToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val label = stringResource(if (expanded) R.string.coach_collapse else R.string.coach_expand)
    val shown = (if (themeMode == ThemeMode.Light) label else label.uppercase()) + if (expanded) " ▴" else " ▾"
    Box(
        modifier = modifier
            .heightIn(min = CoachDimens.MinTouchTarget)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onToggle)
            .padding(end = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = shown,
            style = when (themeMode) {
                ThemeMode.Light -> MaterialTheme.typography.labelLarge
                ThemeMode.Comic -> MaterialTheme.typography.labelLarge.copy(fontFamily = BangersFontFamily, letterSpacing = 0.5.sp)
                else -> PixelTypography.labelMedium
            },
            color = when (themeMode) {
                ThemeMode.Light -> Color(0xFF4F46E5)
                ThemeMode.Comic -> ComicTokens.SolidBlack
                else -> PixelTheme.colors.primary
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PixelSuccessCard(
    uiState: AiInsightUiState.Success,
    isSimpleMode: Boolean = false,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null
) {
    val insight = uiState.insight
    val colors = PixelTheme.colors
    val preview = onToggleExpanded != null && !expanded

    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        modifier = modifier.fillMaxWidth(),
        contentPadding = CoachDimens.Card.PixelPadding
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Arcade scanline title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSimpleMode) "[HABIT TELEMETRY]" else "[QUEST TELEMETRY]",
                    style = PixelTypography.labelMedium,
                    color = if (isSimpleMode) Color(0xFF34D399) else Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .background(if (uiState.isCached) Color(0xFF1E3A8A) else Color(0xFF065F46))
                        .border(1.dp, if (uiState.isCached) Color(0xFF60A5FA) else Color(0xFF34D399))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (uiState.isCached) "CACHED" else "FRESH",
                        style = PixelTypography.labelSmall,
                        color = if (uiState.isCached) Color(0xFF93C5FD) else Color(0xFF6EE7B7),
                        fontSize = CoachDimens.Card.Badge
                    )
                }
            }

            Spacer(modifier = Modifier.height(CoachDimens.Card.TitleGap))

            // Section 1: Observation Scan
            PixelTelemetryBlock(
                tag = if (isSimpleMode) "► HABIT PATTERN SCAN" else "► OBSERVATION SCAN",
                body = insight.summary,
                tagColor = Color(0xFF60A5FA),
                bodyColor = colors.onSurface,
                maxLines = if (preview) CoachDimens.Card.PreviewLines else Int.MAX_VALUE,
                modifier = Modifier.coachAnimateSize()
            )

            CoachExpandable(visible = !preview) {
                Spacer(modifier = Modifier.height(CoachDimens.Card.SectionGap))

                // Section 2: Strategic Protocol / Suggested Adjustment
                PixelTelemetryBlock(
                    tag = if (isSimpleMode) "► SUGGESTED ACTION" else "► STRATEGIC PROTOCOL",
                    body = insight.suggestion,
                    tagColor = Color(0xFF34D399),
                    bodyColor = colors.onSurface
                )

                Spacer(modifier = Modifier.height(CoachDimens.Card.SectionGap))

                // Section 3: Daily Perspective / Heroic Blessing
                PixelTelemetryBlock(
                    tag = if (isSimpleMode) "► DAILY PERSPECTIVE" else "► HEROIC BLESSING",
                    body = insight.encouragement,
                    tagColor = if (isSimpleMode) Color(0xFF34D399) else Color(0xFFFBBF24),
                    bodyColor = if (isSimpleMode) colors.onSurface else Color(0xFFFDE68A)
                )
            }
            if (onToggleExpanded != null) {
                Spacer(modifier = Modifier.height(CoachDimens.ItemGap))
                CoachExpandToggle(expanded = expanded, onToggle = onToggleExpanded, themeMode = ThemeMode.Pixel)
            }

            Spacer(modifier = Modifier.height(CoachDimens.Card.FooterGap))

            // Footer Terminal Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedTime = rememberDateFormatted(insight.generatedAt)
                Text(
                    text = "LOGGED: $formattedTime",
                    style = PixelTypography.labelSmall,
                    color = colors.onSurface.copy(alpha = 0.6f),
                    fontSize = CoachDimens.Card.Badge
                )
            }
        }
    }
}

@Composable
fun PixelTelemetryBlock(
    tag: String,
    body: String,
    tagColor: Color,
    bodyColor: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = tag,
            style = PixelTypography.labelMedium,
            color = tagColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                .border(1.dp, tagColor.copy(alpha = 0.3f))
                .padding(CoachDimens.Card.PixelBlockPadding)
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
                lineHeight = CoachDimens.Card.BodyLineHeight,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ComicSuccessView(
    uiState: AiInsightUiState.Success,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null
) {
    val insight = uiState.insight
    val preview = onToggleExpanded != null && !expanded

    Column(modifier = modifier.fillMaxWidth()) {
        // Speech Bubble 1: The Situation (Summary)
        ComicSpeechBubble(
            title = "THE DISPATCH!",
            body = insight.summary,
            tagColor = ComicTokens.BurntOrange,
            bubbleBg = ComicTokens.PanelSurface,
            maxLines = if (preview) CoachDimens.Card.PreviewLines else Int.MAX_VALUE,
            modifier = Modifier.coachAnimateSize()
        )

        CoachExpandable(visible = !preview) {
            Spacer(modifier = Modifier.height(CoachDimens.Card.ComicSectionGap))

            // Speech Bubble 2: Action Directive (Suggestion)
            ComicSpeechBubble(
                title = "HEROIC DIRECTIVE!",
                body = insight.suggestion,
                tagColor = ComicTokens.SkyBlue,
                bubbleBg = Color(0xFFF0F9FF)
            )

            Spacer(modifier = Modifier.height(CoachDimens.Card.ComicSectionGap))

            // Speech Bubble 3: Power Surge (Encouragement)
            ComicSpeechBubble(
                title = "POWER SURGE!!",
                body = insight.encouragement,
                tagColor = ComicTokens.GoldAccent,
                bubbleBg = Color(0xFFFFFBEB)
            )
        }
        if (onToggleExpanded != null) {
            Spacer(modifier = Modifier.height(CoachDimens.ItemGap))
            CoachExpandToggle(expanded = expanded, onToggle = onToggleExpanded, themeMode = ThemeMode.Comic)
        }

        Spacer(modifier = Modifier.height(CoachDimens.Card.FooterGap))

        // Comic Action Footer Panel
        ComicPanel(
            variant = ComicPanelVariant.SURFACE,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val formattedTime = rememberDateFormatted(insight.generatedAt)
                    Text(
                        text = if (uiState.isCached) "ARCHIVED TRANSMISSION" else "FRESH FROM HQ!",
                        fontFamily = BangersFontFamily,
                        fontSize = CoachDimens.Card.ComicFooterTitle,
                        color = if (uiState.isCached) ComicTokens.SolidBlack else ComicTokens.CoralInk
                    )
                    Text(
                        text = formattedTime,
                        fontSize = CoachDimens.Card.ComicFooterTime,
                        fontWeight = FontWeight.Medium,
                        color = ComicTokens.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Speech-bubble container with comic black border, flat black drop shadow,
 * and a stylized bubble title badge.
 */
@Composable
fun ComicSpeechBubble(
    title: String,
    body: String,
    tagColor: Color,
    bubbleBg: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
            .comicDropShadow(offsetX = 4.dp, offsetY = 4.dp, shape = shape)
            .background(bubbleBg, shape)
            .comicBorder(width = 2.5.dp, color = ComicTokens.SolidBlack, shape = shape)
            .padding(CoachDimens.Card.ComicBubblePadding)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Speech Bubble Pointer / Title Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagColor)
                        .border(1.5.dp, ComicTokens.SolidBlack, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = BangersFontFamily,
                        fontSize = CoachDimens.Card.ComicBubbleTitle,
                        letterSpacing = 0.5.sp,
                        color = ComicTokens.SolidBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = body,
                fontWeight = FontWeight.Medium,
                fontSize = CoachDimens.Card.ComicBody,
                lineHeight = CoachDimens.Card.ComicBodyLineHeight,
                color = ComicTokens.TextPrimary,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LightSuccessCard(
    uiState: AiInsightUiState.Success,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null
) {
    val insight = uiState.insight
    val preview = onToggleExpanded != null && !expanded
    val typography = MaterialTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(CoachDimens.Card.LightPadding)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Habit Evaluation",
                        style = typography.titleSmall,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (uiState.isCached) Color(0xFFEFF6FF) else Color(0xFFECFDF5))
                        .border(
                            1.dp,
                            if (uiState.isCached) Color(0xFFBFDBFE) else Color(0xFFA7F3D0),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (uiState.isCached) "Cached" else "Fresh",
                        color = if (uiState.isCached) Color(0xFF1D4ED8) else Color(0xFF047857),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(CoachDimens.Card.LightTitleGap))

            // Section 1: Observation
            LightInsightSection(
                title = "Observation",
                body = insight.summary,
                accentColor = Color(0xFF3B82F6),
                backgroundColor = Color(0xFFF8FAFC),
                maxLines = if (preview) CoachDimens.Card.PreviewLines else Int.MAX_VALUE,
                modifier = Modifier.coachAnimateSize()
            )

            CoachExpandable(visible = !preview) {
                Spacer(modifier = Modifier.height(CoachDimens.Card.LightSectionGap))

                // Section 2: Strategy / Suggestion
                LightInsightSection(
                    title = "Actionable Strategy",
                    body = insight.suggestion,
                    accentColor = Color(0xFF10B981),
                    backgroundColor = Color(0xFFF0FDF4)
                )

                Spacer(modifier = Modifier.height(CoachDimens.Card.LightSectionGap))

                // Section 3: Encouragement
                LightInsightSection(
                    title = "Encouragement",
                    body = insight.encouragement,
                    accentColor = Color(0xFF8B5CF6),
                    backgroundColor = Color(0xFFF5F3FF)
                )
            }
            if (onToggleExpanded != null) {
                Spacer(modifier = Modifier.height(CoachDimens.ItemGap))
                CoachExpandToggle(expanded = expanded, onToggle = onToggleExpanded, themeMode = ThemeMode.Light)
            }

            Spacer(modifier = Modifier.height(CoachDimens.Card.LightFooterGap))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedTime = rememberDateFormatted(insight.generatedAt)
                Text(
                    text = "Generated on $formattedTime",
                    style = typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun LightInsightSection(
    title: String,
    body: String,
    accentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(CoachDimens.Card.LightBlockPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = typography.bodyMedium,
            color = Color(0xFF1E293B),
            lineHeight = CoachDimens.Card.LightBodyLineHeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
