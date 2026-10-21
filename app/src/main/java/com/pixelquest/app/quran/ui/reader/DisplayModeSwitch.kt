package com.pixelquest.app.quran.ui.reader

import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R

/** The order the switch offers the modes in: the Arabic, both, the translation. */
private val SWITCH_ORDER = listOf(DisplayMode.ARABIC, DisplayMode.BOTH, DisplayMode.TRANSLATION)

/**
 * What the reader shows of each ayah, in three segments: Arabic, Both, Translation. Shaped by the
 * look (chamfered in Pixel, inked in Comic, rounded elsewhere); TalkBack hears a group of choices,
 * the current one selected.
 */
@Composable
fun DisplayModeSwitch(selected: DisplayMode, onSelect: (DisplayMode) -> Unit, modifier: Modifier = Modifier) {
    Segments(
        options = SWITCH_ORDER,
        selected = selected,
        onSelect = onSelect,
        label = { mode ->
            stringResource(
                when (mode) {
                    DisplayMode.ARABIC -> R.string.quran_mode_arabic
                    DisplayMode.BOTH -> R.string.quran_mode_both
                    DisplayMode.TRANSLATION -> R.string.quran_mode_translation
                }
            )
        },
        tag = ReaderTags::mode,
        modifier = modifier
    )
}

/** The page the reader's text is on, in two segments: the look's own, or sepia. */
@Composable
fun PaperSwitch(selected: ReaderPaper, onSelect: (ReaderPaper) -> Unit, modifier: Modifier = Modifier) {
    Segments(
        options = ReaderPaper.entries,
        selected = selected,
        onSelect = onSelect,
        label = { paper ->
            stringResource(if (paper == ReaderPaper.SEPIA) R.string.quran_paper_sepia else R.string.quran_paper_theme)
        },
        tag = ReaderTags::paper,
        modifier = modifier
    )
}

/**
 * One choice of a few, in segments shaped by the look: a group of radio buttons to TalkBack, each
 * a 48dp target. No word is ever broken to fit: side by side, each label must fit its segment on
 * one line, in the look's label style or, if not, a smaller one (Pixel's wide pixel font needs
 * that); if none fits, or the text is large, they stack, one to a line, in the full style. Large
 * text is never shrunk.
 */
@Composable
private fun <T> Segments(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    tag: (T) -> String,
    modifier: Modifier = Modifier
) {
    val labels = options.map { label(it) }
    val largeText = LocalDensity.current.fontScale >= STACK_AT_FONT_SCALE
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val room = with(LocalDensity.current) { ((maxWidth - SEGMENT_GAP * (options.size - 1)) / options.size - SEGMENT_PADDING * 2).toPx() }
        val full = PqType.Button.copy(letterSpacing = 0.sp)
        // Then the body face, narrower than Pixel's pixel labels, before stacking.
        val styles = if (largeText) listOf(full) else listOf(full, full.copy(fontSize = 11.sp), PqType.BodyStrong.copy(fontSize = 13.sp), PqType.MetaStrong)
        // Measured bold, as the chosen one is drawn.
        val sideBySide = styles.firstOrNull { style ->
            labels.all { measurer.measure(it, style.copy(fontWeight = FontWeight.Bold), softWrap = false, maxLines = 1).size.width <= room }
        }
        if (sideBySide != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP), modifier = Modifier.fillMaxWidth().selectableGroup()) {
                options.forEachIndexed { i, option ->
                    Segment(labels[i], sideBySide, option == selected, { onSelect(option) }, Modifier.weight(1f).testTag(tag(option)))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SEGMENT_GAP), modifier = Modifier.fillMaxWidth().selectableGroup()) {
                options.forEachIndexed { i, option ->
                    Segment(labels[i], full, option == selected, { onSelect(option) }, Modifier.fillMaxWidth().testTag(tag(option)))
                }
            }
        }
    }
}

private val SEGMENT_GAP = 6.dp
private val SEGMENT_PADDING = 8.dp

@Composable
private fun Segment(label: String, style: TextStyle, chosen: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    // v2's segmented control (PqSegmented): the chosen one filled with ink, the others on paper.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = MIN_TARGET)
            .comic(bg = if (chosen) PqColors.Ink else PqColors.Paper, radius = 12.dp, border = PqDimens.BorderThin, shadow = 0.dp)
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = SEGMENT_PADDING, vertical = 4.dp)
    ) {
        Text(text = label, style = style.copy(color = if (chosen) PqColors.Paper else PqColors.Ink))
    }
}

/** The smallest a control is, to touch: Android's guideline. */
internal val MIN_TARGET = 48.dp

/** From this text size on (the phone's font scale), choices that don't fit side by side stack rather than shrink. */
internal const val STACK_AT_FONT_SCALE = 1.5f
