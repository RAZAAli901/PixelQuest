package com.pixelquest.app.study.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.PixelTheme
import java.time.ZoneId

/**
 * The leader's pinned items, in a strip across the top of the item tabs: each a small card with its
 * title and when it's due, opening the item. Nothing when none is pinned.
 */
@Composable
fun PinnedStrip(rows: List<ItemRow>, onOpen: (String) -> Unit, modifier: Modifier = Modifier, zone: ZoneId = ZoneId.systemDefault()) {
    if (rows.isEmpty()) return
    val colors = PixelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth().testTag(StudyTags.PINNED)) {
        Text(
            text = stringResource(R.string.study_pinned_title),
            style = MaterialTheme.typography.titleSmall,
            color = colors.primaryText,
            modifier = Modifier.semantics { heading() }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { "pinned_" + it.item.id }) { row ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .widthIn(min = 140.dp, max = 220.dp)
                        .studyChip(colors.surfaceVariant)
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.study_open_item)) { onOpen(row.item.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag(StudyTags.pinned(row.item.id))
                ) {
                    Text(
                        text = stringResource(R.string.study_pinned_line, row.item.title),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dueText(row.due, row.item.dueAt, zone),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.due == DueState.OVERDUE) colors.error else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
