package com.pixelquest.app.quran.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.quranChipShape
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.inkOnPanel

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

/** One choice of a few, in segments shaped by the look: a group of radio buttons to TalkBack. */
@Composable
private fun <T> Segments(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    tag: (T) -> String,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val shape: Shape = quranChipShape()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth().selectableGroup()) {
        options.forEach { option ->
            val chosen = option == selected
            val ink = if (PixelTheme.mode == ThemeMode.Comic) {
                Modifier.border(ComicShapeTokens.BorderWidthThin, ComicTokens.SolidBlack, shape)
            } else {
                Modifier.border(1.dp, colors.primary.copy(alpha = if (chosen) 1f else 0.4f), shape)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .background(if (chosen) colors.primary.copy(alpha = 0.18f) else colors.surface, shape)
                    .then(ink)
                    .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(option) })
                    .testTag(tag(option))
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                    color = inkOnPanel(colors.primaryText)
                )
            }
        }
    }
}
