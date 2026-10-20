package com.pixelquest.app.quran.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * The card at the top of the Quran screen that takes the player back to where they last read:
 * the surah by name, and the ayah. One button: tapping it opens the reader there.
 */
@Composable
fun ContinueReadingCard(place: ContinueReading, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    PixelCard(
        variant = PixelPanelVariant.BLUE,
        contentPadding = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag(CONTINUE_READING)
            .clickable(role = Role.Button, onClick = onOpen)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quran_continue_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = inkOnPanel(colors.primaryText)
                )
                Text(
                    text = stringResource(R.string.quran_continue_place, place.surah.number, place.surah.englishName, place.ayah),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
            }
            Text(
                text = "▶",
                style = MaterialTheme.typography.titleMedium,
                color = inkOnPanel(colors.primaryText),
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
    }
}

/** The Continue reading card's test tag. */
const val CONTINUE_READING = "quran_continue_reading"
