package com.pixelquest.app.quran.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.ui.theme.PixelTheme

/** What can be done with one ayah, from its sheet. */
enum class AyahAction { BOOKMARK, COPY_ARABIC, COPY_TRANSLATION, SHARE }

/** The sheet a tapped ayah opens: its reference, then what can be done with it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahActionsSheet(
    ayah: Ayah,
    surahName: String,
    bookmarked: Boolean,
    onAction: (AyahAction) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PixelTheme.colors.background) {
        AyahActionsContent(ayah = ayah, surahName = surahName, bookmarked = bookmarked, onAction = onAction)
    }
}

/**
 * The sheet's content: "Al-Baqara 2:255", then bookmark (or remove the bookmark), copy the Arabic,
 * copy the translation, and share. Tested on its own.
 */
@Composable
fun AyahActionsContent(
    ayah: Ayah,
    surahName: String,
    bookmarked: Boolean,
    onAction: (AyahAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 24.dp).testTag(ReaderTags.ACTIONS)) {
        Text(
            text = stringResource(R.string.quran_ayah_reference, surahName, ayah.surah, ayah.number),
            style = MaterialTheme.typography.titleMedium,
            color = colors.primaryText,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() }
        )
        ActionRow(if (bookmarked) "🔖" else "📑", stringResource(if (bookmarked) R.string.quran_unbookmark else R.string.quran_bookmark), AyahAction.BOOKMARK, onAction)
        ActionRow("📋", stringResource(R.string.quran_copy_arabic), AyahAction.COPY_ARABIC, onAction)
        ActionRow("📋", stringResource(R.string.quran_copy_translation), AyahAction.COPY_TRANSLATION, onAction)
        ActionRow("📤", stringResource(R.string.quran_share), AyahAction.SHARE, onAction)
    }
}

/** One action: a full-width row, a button to TalkBack, its emoji left out. */
@Composable
private fun ActionRow(icon: String, label: String, action: AyahAction, onAction: (AyahAction) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button) { onAction(action) }
            .padding(horizontal = 24.dp)
            .testTag(ReaderTags.action(action))
    ) {
        Text(text = icon, fontSize = 20.sp, modifier = Modifier.clearAndSetSemantics {})
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = PixelTheme.colors.onSurface)
    }
}
