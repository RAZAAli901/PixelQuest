package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * The party's milestone, pinned under the chat's top bar: how far the total is toward it, as a bar.
 * Open, it says "2,450 of 10,000 (24%)"; folded, it's just a slim bar, for more room to read the
 * chat. A tap on it opens or folds it.
 */
@Composable
fun MilestoneBar(
    total: Long,
    milestone: Long,
    finished: Boolean,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val progress = (total.toDouble() / milestone).coerceIn(0.0, 1.0).toFloat()
    val percent = (progress * 100).toInt()
    val words = if (finished) {
        stringResource(R.string.darood_milestone_reached, formatCount(milestone))
    } else {
        stringResource(R.string.darood_milestone_progress, formatCount(total), formatCount(milestone), percent)
    }
    val state = stringResource(if (collapsed) R.string.darood_milestone_folded else R.string.darood_milestone_open)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .testTag(DaroodTestTags.CHAT_MILESTONE)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(if (collapsed) R.string.darood_milestone_show else R.string.darood_milestone_hide),
                onClick = onToggle
            )
            // Folded or open, TalkBack hears the whole of it, and which it is.
            .clearAndSetSemantics {
                contentDescription = words
                stateDescription = state
            }
            .padding(horizontal = 16.dp, vertical = if (collapsed) 4.dp else 8.dp)
    ) {
        if (!collapsed) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = words,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "▲", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
        }
        PixelProgressBar(progress = progress, height = if (collapsed) 4.dp else 10.dp)
    }
}
