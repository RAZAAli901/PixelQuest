package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * A quest's category: a chip each, with its icon, the chosen one in the category's own colour (the
 * colour its quests wear elsewhere). The icon is decoration: the name says it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PixelCategorySelector(
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Category"
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PqFieldLabel(label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.selectableGroup()
        ) {
            TaskCategory.entries.forEach { category ->
                val on = category == selectedCategory
                val look = QuestCategory.from(category.name)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(40.dp)
                        .clip(PqCircle)
                        .background(if (on) look.color else PqColors.Paper)
                        .border(if (on) PqDimens.Border else PqDimens.BorderThin, PqColors.Ink, PqCircle)
                        .choiceChip(on) { onCategorySelected(category) }
                        .padding(start = 8.dp, end = 14.dp)
                ) {
                    Icon(look.icon, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(20.dp).padding(end = 2.dp))
                    Text(text = category.displayName, style = PqType.MetaStrong, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
