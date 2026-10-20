package com.pixelquest.app.quran.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.PixelTheme
import kotlinx.coroutines.launch

/** The surahs the fast-scroll index marks: the first, then every tenth. */
val FAST_SCROLL_MARKS: List<Int> = listOf(1) + (10..110 step 10)

/**
 * A strip of marks beside the full surah list (1, 10, 20 … 110): tap one, or drag along the strip,
 * and the list jumps to that surah. TalkBack users get each mark as a button ("Jump to surah 20").
 *
 * @param surahs the numbers shown in the list, in order: it jumps to their positions.
 */
@Composable
fun FastScrollIndex(listState: LazyListState, surahs: List<Int>, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val colors = PixelTheme.colors
    fun jumpTo(mark: Int) {
        val index = surahs.indexOfFirst { it >= mark }.takeIf { it >= 0 } ?: return
        scope.launch { listState.scrollToItem(index) }
    }
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(36.dp)
            .testTag(FastScrollTags.STRIP)
            .pointerInput(surahs) {
                detectVerticalDragGestures { change, _ ->
                    val share = (change.position.y / size.height).coerceIn(0f, 0.999f)
                    jumpTo(FAST_SCROLL_MARKS[(share * FAST_SCROLL_MARKS.size).toInt()])
                }
            }
    ) {
        FAST_SCROLL_MARKS.forEach { mark ->
            val spoken = stringResource(R.string.quran_jump_to_surah, mark)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
                    .clickable { jumpTo(mark) }
                    .testTag(FastScrollTags.mark(mark))
                    .clearAndSetSemantics {
                        contentDescription = spoken
                        role = Role.Button
                        onClick { jumpTo(mark); true }
                    }
                    .padding(vertical = 2.dp)
            ) {
                Text(text = mark.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** The fast-scroll index's test tags. */
object FastScrollTags {
    const val STRIP = "quran_fast_scroll"

    fun mark(surah: Int) = "quran_fast_scroll_$surah"
}
