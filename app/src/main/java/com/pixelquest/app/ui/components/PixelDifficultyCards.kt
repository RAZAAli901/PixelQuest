package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** A difficulty's icon and colour on its card. */
private fun DifficultyLevel.look(): Pair<ImageVector, Color> = when (this) {
    DifficultyLevel.EASY -> Icons.Rounded.Spa to PqColors.Sky
    DifficultyLevel.MEDIUM -> Icons.Rounded.Bolt to PqColors.Amber
    DifficultyLevel.HARD -> Icons.Rounded.LocalFireDepartment to PqColors.Lilac
    DifficultyLevel.HARDEST -> Icons.Rounded.Whatshot to PqColors.Rose
}

/**
 * The difficulty levels, a card each, one chosen (a radio button to TalkBack): its name, what a perfect
 * day needs at it, and the days a level takes. The chosen one fills with its colour and is checked.
 */
@Composable
fun PixelDifficultyCards(
    selectedLevel: DifficultyLevel,
    onLevelSelected: (DifficultyLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DifficultyLevel.entries.forEach { level ->
            val on = level == selectedLevel
            val (icon, tint) = level.look()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .comic(bg = if (on) tint else PqColors.Paper, radius = PqDimens.Radius, shadow = if (on) PqDimens.Shadow else 0.dp, tappable = true)
                    .choiceChip(on) { onLevelSelected(level) }
                    .padding(14.dp)
            ) {
                CategoryTile(icon, if (on) PqColors.Paper else tint)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = DifficultyMode.getDisplayName(level).uppercase(), style = PqType.BodyStrong)
                    Text(text = "Perfect day: ${(DifficultyMode.getPerfectDayThreshold(level) * 100).toInt()}% done", style = PqType.Meta.copy(color = PqColors.Ink))
                    Text(text = "${DifficultyMode.getDaysRequiredPerLevel(level)} days a level", style = PqType.Meta)
                }
                if (on) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = PqColors.Ink)
            }
        }
    }
}
